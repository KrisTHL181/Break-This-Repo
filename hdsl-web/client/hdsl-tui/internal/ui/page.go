package ui

// The palette is the Debian installer's: a blue screen, light grey dialogs,
// a cyan bar on the selected row.
const (
	// Reset closes every style.
	Reset = "\x1b[0m"
	// ScreenBG is the blue background every line is painted on.
	ScreenBG = "\x1b[44m"
	// ScreenText is plain text on the blue background.
	ScreenText = "\x1b[37;44m"
	// Border draws the box.
	Border = "\x1b[1;37;44m"
	// TitleStyle is the box title, embedded in the top border.
	TitleStyle = "\x1b[1;37;44m"
	// Body is the dialog interior: black on light grey.
	Body = "\x1b[30;47m"
	// BodyDim is a disabled or secondary row.
	BodyDim = "\x1b[90;47m"
	// Selected is the bar under the cursor.
	Selected = "\x1b[30;46m"
	// Danger marks an error inside a dialog.
	Danger = "\x1b[1;31;47m"
	// Hint is the key legend on the bottom of the box.
	Hint = "\x1b[30;47m"
	// Status is the always-on bottom line outside the box.
	Status = "\x1b[37;44m"
)

// Box drawing pieces. Terminals in a UTF-8 locale render these; the client
// already requires UTF-8 for Chinese text.
const (
	topLeft        = "╔"
	topRight       = "╗"
	bottomLeft     = "╚"
	bottomRight    = "╝"
	horizontal     = "═"
	separatorLeft  = "╟"
	separatorRight = "╢"
	separatorDash  = "─"
	vertical       = "║"
)

// Page is one full screen dialog.
type Page struct {
	// Title sits in the top border.
	Title string
	// Body lines must already be InnerWidth cells wide (widgets produce
	// them); Render pads or clips whatever is left.
	Body []string
	// Hint is the key legend row.
	Hint string
	// Status is the line under the box: which panel, which user, elevation.
	Status string
}

// Geometry reports the sizes Render will use, so a caller can build body lines
// of exactly the right width before drawing.
func Geometry(width, height int) (innerWidth, bodyHeight int) {
	boxWidth := width - 6
	if boxWidth < 24 {
		boxWidth = width - 2
	}
	boxHeight := height - 4
	if boxHeight < 7 {
		boxHeight = height - 1
	}
	innerWidth = boxWidth - 2
	if innerWidth < 10 {
		innerWidth = 10
	}
	bodyHeight = boxHeight - 4
	if bodyHeight < 1 {
		bodyHeight = 1
	}
	return innerWidth, bodyHeight
}

// Render lays a page out on a width×height screen. The result is exactly
// height lines, each filling width cells.
func Render(page Page, width, height int) []string {
	if width < 12 {
		width = 12
	}
	if height < 6 {
		height = 6
	}
	innerWidth, bodyHeight := Geometry(width, height)
	boxWidth := innerWidth + 2

	lines := make([]string, 0, height)
	push := func(line string) {
		// Pad after switching back to the screen background, so the right
		// margin is blue rather than the terminal's default colour.
		missing := width - VisibleWidth(line)
		if missing < 0 {
			missing = 0
		}
		lines = append(lines, line+ScreenBG+Repeat(" ", missing))
	}

	// One blank blue line above the dialog.
	push(ScreenBG + ScreenText + Reset)

	title := page.Title
	if title == "" {
		title = "HDSL-Web 终端客户端"
	}
	titleCells := VisibleWidth(title) + 4
	fill := boxWidth - 3 - titleCells
	if fill < 0 {
		fill = 0
	}
	push(ScreenBG + Border + topLeft + horizontal + " " + Reset + TitleStyle + title + Reset + Border +
		" " + Repeat(horizontal, fill) + topRight + Reset)

	body := page.Body
	for index := 0; index < bodyHeight; index++ {
		line := ""
		if index < len(body) {
			line = body[index]
		}
		push(ScreenBG + Border + vertical + Reset + Fill(stripTailReset(line), innerWidth, Body) +
			Border + vertical + Reset)
	}

	hint := page.Hint
	if hint == "" {
		hint = "↑/↓ 选择    Enter 确认    Esc 返回    Ctrl-C 取消"
	}
	push(ScreenBG + Border + separatorLeft + Repeat(separatorDash, innerWidth) + separatorRight + Reset)
	push(ScreenBG + Border + vertical + Reset + Fill(" "+hint, innerWidth, Hint) + Border + vertical + Reset)
	push(ScreenBG + Border + bottomLeft + Repeat(horizontal, innerWidth) + bottomRight + Reset)

	// The status line and one blank line underneath.
	if page.Status != "" {
		push(ScreenBG + ScreenText + " " + Truncate(page.Status, width-2) + Reset)
	} else {
		push(ScreenBG + ScreenText + Reset)
	}
	for len(lines) < height {
		push(ScreenBG + ScreenText + Reset)
	}
	return lines[:height]
}

// stripTailReset removes a trailing reset so Fill can apply the body style
// without a style change in the middle of the padded row.
func stripTailReset(line string) string {
	const reset = "\x1b[0m"
	if len(line) >= len(reset) && line[len(line)-len(reset):] == reset {
		return line[:len(line)-len(reset)]
	}
	return line
}
