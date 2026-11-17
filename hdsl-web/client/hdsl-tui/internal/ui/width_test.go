package ui

import "testing"

func TestVisibleWidthIgnoresColour(t *testing.T) {
	cases := []struct {
		input string
		want  int
	}{
		{"", 0},
		{"abc", 3},
		{"\x1b[1;31mabc\x1b[0m", 3},
		{"中", 2},
		{"a中b", 4},
		{"\x1b[44m中\x1b[0m", 2},
	}
	for _, testCase := range cases {
		if got := VisibleWidth(testCase.input); got != testCase.want {
			t.Errorf("VisibleWidth(%q) = %d, want %d", testCase.input, got, testCase.want)
		}
	}
}

func TestPadCountsCellsNotBytes(t *testing.T) {
	if got := Pad("中", 4); got != "中  " {
		t.Errorf("Pad(中,4) = %q", got)
	}
	if got := Pad("abcd", 2); got != "abcd" {
		t.Errorf("Pad must not truncate: %q", got)
	}
}

func TestTruncateKeepsTheEscapeCodesItKeeps(t *testing.T) {
	got := Truncate("abcdef", 4)
	if VisibleWidth(got) > 4 {
		t.Errorf("Truncate = %q (%d cells)", got, VisibleWidth(got))
	}
	if got != "abc…" {
		t.Errorf("Truncate = %q, want abc…", got)
	}
	coloured := Truncate("\x1b[31mabcdef\x1b[0m", 4)
	if VisibleWidth(coloured) > 4 {
		t.Errorf("coloured truncate = %q (%d cells)", coloured, VisibleWidth(coloured))
	}
}

func TestFillIsExactlyTheRequestedWidth(t *testing.T) {
	for _, input := range []string{"", "a", "中文测试", "a中b", "\x1b[31mred\x1b[0m"} {
		for width := 1; width <= 12; width++ {
			if got := VisibleWidth(Fill(input, width, Body)); got != width {
				t.Errorf("Fill(%q,%d) is %d cells", input, width, got)
			}
		}
	}
}

func TestRowPadsWithoutChangingColour(t *testing.T) {
	got := Row("\x1b[30;46mab\x1b[0m", 6)
	if VisibleWidth(got) != 6 {
		t.Errorf("Row = %q (%d cells)", got, VisibleWidth(got))
	}
}
