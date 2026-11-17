package app

import (
	"fmt"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// certificateRiskPage is what the user gets instead of a dead end when the
// panel's certificate does not prove its identity.
//
// The order of the rows is the safety mechanism: the menu starts on the first
// enabled item, so pressing Enter on a screen nobody read returns to the server
// list rather than disabling verification.
func certificateRiskPage(server string, certificate *panel.CertificateError) menuPage {
	detail := fmt.Sprintf(
		"%s\n\n"+
			"继续连接仍然会加密，但客户端不再核对对面是谁：这中间的人可以冒充这台面板，"+
			"你输入的面板密码和开发者密码也会被它看到。\n\n"+
			"只在可信网络或临时测试时这样做。这个选择只对本次运行有效，不写进配置，"+
			"也不影响其他服务器。\n\n"+
			"更稳的做法：让证书包含这个地址（面板「设置 → HTTPS 证书」可以生成带本机地址的"+
			"自签名证书，或给证书加 IP SAN），再用 --ca 信任它。",
		certificate.Reason)

	return menuPage{
		Title:    "无法验证面板证书",
		Subtitle: server,
		Detail:   detail,
		Items: []ui.Item{
			{Label: "← 返回（推荐）"},
			{Label: "仅本次忽略风险并连接"},
		},
		Hint: "↑/↓ 选择    Enter 确定    Esc / Ctrl-C 返回",
	}
}
