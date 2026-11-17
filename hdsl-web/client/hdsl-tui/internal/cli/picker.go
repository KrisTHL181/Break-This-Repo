package cli

import (
	"errors"
	"fmt"
	"io"
	"strconv"
	"strings"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
)

// ErrQuit is returned when the user refuses to pick an instance.
var ErrQuit = errors.New("已取消")

// Choice is the instance the user picked.
type Choice struct {
	Brand string
	ID    string
	Name  string
	State string
}

// Group is one block of the picker. An empty group is not rendered.
type Group struct {
	Title string
	Brand string
	Items []panel.Instance
}

// Pick renders the numbered menu on w and reads the choice from r. Numbering
// runs across groups, so a number identifies an instance no matter which list
// it came from.
func Pick(r io.Reader, w io.Writer, groups []Group) (Choice, error) {
	var choices []Choice
	fmt.Fprintln(w, "请选择要进入的实例：")
	fmt.Fprintln(w)
	for _, group := range groups {
		if len(group.Items) == 0 {
			continue
		}
		fmt.Fprintf(w, "  %s\n", group.Title)
		for _, item := range group.Items {
			choices = append(choices, Choice{
				Brand: group.Brand,
				ID:    item.ID,
				Name:  item.Name,
				State: item.State,
			})
			label := item.Name
			if label == "" {
				label = item.ID
			}
			state := ""
			if item.State != "" {
				state = "  [" + item.State + "]"
			}
			fmt.Fprintf(w, "    %d) %-24s %s%s\n", len(choices), label, item.ID, state)
		}
	}
	if len(choices) == 0 {
		return Choice{}, errors.New("没有可用的实例：请先在面板里创建，或用 --instance 指定")
	}
	fmt.Fprintln(w)
	for {
		line, err := termx.ReadLine(r, w, fmt.Sprintf("输入编号 1-%d（q 退出）: ", len(choices)))
		if err != nil {
			fmt.Fprintln(w)
			return Choice{}, ErrQuit
		}
		answer := strings.TrimSpace(line)
		if answer == "q" || answer == "quit" || answer == "exit" {
			return Choice{}, ErrQuit
		}
		index, err := strconv.Atoi(answer)
		if err != nil || index < 1 || index > len(choices) {
			fmt.Fprintf(w, "输入无效：请输入 1-%d 之间的编号。\n", len(choices))
			continue
		}
		return choices[index-1], nil
	}
}
