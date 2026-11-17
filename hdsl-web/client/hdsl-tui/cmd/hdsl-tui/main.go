// Command hdsl-tui opens one HDSL-Web instance's terminal in a real terminal.
//
// Run with no arguments in a terminal and it opens its own menu: pick a saved
// panel or type a new address, log in once (the session can be remembered),
// then choose 分类 → 实例 → 动作. Attaching to an agent returns to the menu,
// not to the shell — the login and the connection survive the agent.
//
// Run with a panel address and no terminal (a pipe, cron, a script) and it
// falls back to the line-based flow it always had.
package main

import (
	"context"
	"crypto/tls"
	"errors"
	"flag"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/app"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/cli"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/rtty"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
)

// version is stamped by the release build: -X main.version=v0.5.4.
var version = "dev"

// brands are the instance surfaces beyond dsh, in menu order: the three brand
// categories and ZCode (its own experimental category, same terminal contract).
var brands = []string{"kimi", "opencode", "claude-web", "pi-web", "zcode"}

const requestTimeout = 30 * time.Second

type options struct {
	instance    string
	brand       string
	resume      string
	kernel      string
	user        string
	ca          string
	insecure    bool
	showVersion bool
	verbose     bool
	plain       bool
}

func main() {
	os.Exit(run(os.Args[1:], os.Stdin, os.Stdout, os.Stderr))
}

func run(args []string, stdin io.Reader, stdout, stderr io.Writer) int {
	opts, base, err := parseArgs(args)
	if err != nil {
		if errors.Is(err, flag.ErrHelp) {
			usage(stdout)
			return cli.ExitOK
		}
		fmt.Fprintln(stderr, err)
		return cli.ExitUsage
	}
	if opts.showVersion {
		fmt.Fprintf(stdout, "hdsl-tui %s\nterminal protocol %d\n", version, pterm.Protocol)
		return cli.ExitOK
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if !opts.plain && isTerminal(stdin) && isTerminal(stdout) {
		return runInteractive(ctx, opts, base, stdin.(*os.File), stdout.(*os.File), stderr)
	}
	return runPlain(ctx, opts, base, stdin, stdout, stderr)
}

func isTerminal(stream any) bool {
	file, ok := stream.(*os.File)
	return ok && termx.IsTerminal(file)
}

// runInteractive is the default in a terminal: the menu owns the screen from
// here on.
func runInteractive(ctx context.Context, opts options, base *url.URL, in, out *os.File, stderr io.Writer) int {
	log := logger{w: stderr, enabled: opts.verbose}

	tlsConfig, err := tlsConfigFor(opts.ca, opts.insecure, base, in, stderr, log.Logf)
	if err != nil {
		fmt.Fprintln(stderr, "错误: "+err.Error())
		return cli.ExitUsage
	}
	httpClient := &http.Client{
		Transport: &http.Transport{TLSClientConfig: tlsConfig, Proxy: http.ProxyFromEnvironment},
		Timeout:   requestTimeout,
	}
	newClient := func(baseURL string) (*panel.Client, error) {
		return panel.New(baseURL, httpClient, log.Logf)
	}

	// The dialog for a certificate that does not prove the panel's identity.
	// --insecure already skips verification for every server, so there is
	// nothing left to offer; otherwise both halves of the connection switch
	// together, which is why the WebSocket config is prepared here as well.
	var newInsecureClient func(string) (*panel.Client, error)
	var insecureTLSConfig *tls.Config
	if !tlsConfig.InsecureSkipVerify {
		insecureTLSConfig = &tls.Config{MinVersion: tls.VersionTLS12, InsecureSkipVerify: true}
		insecureHTTPClient := &http.Client{
			Transport: &http.Transport{TLSClientConfig: insecureTLSConfig, Proxy: http.ProxyFromEnvironment},
			Timeout:   requestTimeout,
		}
		newInsecureClient = func(baseURL string) (*panel.Client, error) {
			return panel.New(baseURL, insecureHTTPClient, log.Logf)
		}
	}

	initial := ""
	if base != nil {
		initial = base.String()
	}

	raw, openErr := termx.Open(in, out)
	if openErr != nil {
		fmt.Fprintln(stderr, "错误: 无法初始化终端："+openErr.Error())
		return cli.ExitUsage
	}

	code, runErr := app.Run(ctx, app.Options{
		NewClient:         newClient,
		NewInsecureClient: newInsecureClient,
		Instance:          opts.instance,
		Brand:             normaliseBrand(opts.brand),
		Resume:            opts.resume,
		Kernel:            opts.kernel,
		User:              opts.user,
		TLSConfig:         tlsConfig,
		InsecureTLSConfig: insecureTLSConfig,
		Insecure:          opts.insecure && base == nil,
		InitialServer:     initial,
		Version:           version,
		Logf:              log.Logf,
	}, in, out, stderr, raw)
	if runErr != nil {
		fmt.Fprintln(stderr, "错误: "+runErr.Error())
		return cli.ExitUsage
	}
	return code
}

// runPlain is the scriptable flow: a panel address is required, the username
// and both passwords are read from stdin, and the chooser is a numbered list.
func runPlain(ctx context.Context, opts options, base *url.URL, stdin io.Reader, stdout, stderr io.Writer) int {
	log := logger{w: stderr, enabled: opts.verbose}
	fail := func(err error) int {
		code, text := cli.Explain(err)
		fmt.Fprintln(stderr, "错误: "+text)
		return code
	}
	if base == nil {
		fmt.Fprintln(stderr, "缺少面板地址：非交互式运行需要显式指定，例如 hdsl-tui https://panel.example.com:8443")
		fmt.Fprintln(stderr)
		fmt.Fprint(stderr, usageText)
		return cli.ExitUsage
	}

	tlsConfig, err := tlsConfigFor(opts.ca, opts.insecure, base, stdin, stderr, log.Logf)
	if err != nil {
		return fail(err)
	}
	httpClient := &http.Client{
		Transport: &http.Transport{TLSClientConfig: tlsConfig, Proxy: http.ProxyFromEnvironment},
		Timeout:   requestTimeout,
	}
	client, err := panel.New(base.String(), httpClient, log.Logf)
	if err != nil {
		fmt.Fprintln(stderr, err)
		return cli.ExitUsage
	}

	health, err := client.Health(ctx)
	if err != nil {
		return fail(err)
	}
	log.Logf("面板 %s 版本 %s，已运行 %d 秒", client.BaseURL(), health.Version, health.UptimeSec)
	if health.Status != "" && health.Status != "ok" {
		fmt.Fprintf(stderr, "警告: 面板自报状态 %q，仍会尝试连接。\n", health.Status)
	}

	username := opts.user
	if username == "" {
		answer, err := termx.ReadLine(stdin, stderr, "面板用户名 [admin]: ")
		if err != nil && !errors.Is(err, io.EOF) {
			fmt.Fprintln(stderr, "错误: 无法读取用户名："+err.Error())
			return cli.ExitUsage
		}
		username = strings.TrimSpace(answer)
		if username == "" {
			username = "admin"
		}
	}
	password, err := termx.ReadPassword(stderr, fmt.Sprintf("面板密码（%s）: ", username))
	if err != nil {
		fmt.Fprintln(stderr, "错误: "+err.Error())
		return cli.ExitUsage
	}
	if _, err := client.Login(ctx, username, password); err != nil {
		return fail(err)
	}
	log.Logf("已登录，会话 Cookie 只保存在内存里")

	developerPassword, err := termx.ReadPassword(stderr, "开发者密码: ")
	if err != nil {
		fmt.Fprintln(stderr, "错误: "+err.Error())
		return cli.ExitUsage
	}
	elevation, err := client.Elevate(ctx, developerPassword)
	if err != nil {
		return fail(err)
	}
	if expiry := elevation.Expiry(); !expiry.IsZero() {
		log.Logf("提权有效至 %s", expiry.Format(time.RFC3339))
	}

	brand, id := normaliseBrand(opts.brand), opts.instance
	if id == "" {
		groups, err := collectGroups(ctx, client, log)
		if err != nil {
			return fail(err)
		}
		choice, err := cli.Pick(stdin, stderr, groups)
		if errors.Is(err, cli.ErrQuit) {
			return cli.ExitOK
		}
		if err != nil {
			return fail(err)
		}
		brand, id = choice.Brand, choice.ID
	}

	terminal, err := client.OpenTerminal(ctx, brand, id, opts.resume, opts.kernel)
	if err != nil {
		return fail(err)
	}
	if terminal.Desktop() {
		// A Windows panel has no PTY at all; there is nothing to render.
		fmt.Fprintln(stderr, "错误: "+terminal.Hint)
		return cli.ExitPolicy
	}
	wsURL, err := client.WebSocketURL(terminal.WS)
	if err != nil {
		return fail(err)
	}
	log.Logf("终端已开: instance=%s brand=%s protocol=%d", terminal.Instance, terminal.Brand, terminal.Protocol)

	conn, err := pterm.Dial(ctx, wsURL, client.Session(), tlsConfig)
	if err != nil {
		return fail(err)
	}
	defer conn.Close()

	tty := fileOrStdin(stdin)
	result, runErr := rtty.Run(ctx, tty, fileOrStdout(stdout), tty, conn, log.Logf)
	if runErr != nil {
		return fail(runErr)
	}
	switch result.Command {
	case pterm.CommandDetach:
		fmt.Fprintln(stderr, cli.DetachHint)
		return cli.ExitOK
	case pterm.CommandKill:
		return killTerminal(ctx, client, brand, id, stderr)
	}
	if result.ExitKnown {
		fmt.Fprintf(stderr, "终端已结束（agent 退出码 %d）。\n", result.ExitCode)
		return result.ExitCode
	}
	if result.CloseCode != 0 {
		return fail(&pterm.CloseError{Code: result.CloseCode, Reason: result.CloseReason})
	}
	return fail(errors.New("终端连接在没有收到 exit 帧的情况下结束"))
}

// fileOrStdin and fileOrStdout let the plain flow keep working with buffers in
// tests: rtty only needs a terminal when there is one.
func fileOrStdin(stream io.Reader) *os.File {
	if file, ok := stream.(*os.File); ok {
		return file
	}
	return os.Stdin
}

func fileOrStdout(stream io.Writer) *os.File {
	if file, ok := stream.(*os.File); ok {
		return file
	}
	return os.Stdout
}

// killTerminal stops the terminal through the REST surface, which is what
// Ctrl-] k promises.
func killTerminal(ctx context.Context, client *panel.Client, brand, id string, stderr io.Writer) int {
	closeCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), requestTimeout)
	defer cancel()
	killed, err := client.CloseTerminal(closeCtx, brand, id)
	if err != nil {
		code, text := cli.Explain(err)
		fmt.Fprintln(stderr, "错误: 停止终端失败："+text)
		return code
	}
	if killed {
		fmt.Fprintln(stderr, "终端已停止。")
	} else {
		fmt.Fprintln(stderr, "没有正在运行的终端。")
	}
	return cli.ExitOK
}

// collectGroups fetches the DSH list plus the three brand lists for the picker.
// A brand that answers with an error is skipped: a panel without that brand's
// directory is normal, not a reason to refuse the whole menu.
func collectGroups(ctx context.Context, client *panel.Client, log logger) ([]cli.Group, error) {
	instances, err := client.Instances(ctx)
	if err != nil {
		return nil, err
	}
	groups := []cli.Group{{Title: "DSH 实例", Items: instances}}
	for _, brand := range brands {
		items, err := client.BrandInstances(ctx, brand)
		if err != nil {
			log.Logf("跳过 %s 实例列表: %v", brand, err)
			continue
		}
		groups = append(groups, cli.Group{Title: brandTitle(brand), Brand: brand, Items: items})
	}
	return groups, nil
}

// brandTitle is the picker's label for a surface.
func brandTitle(brand string) string {
	switch brand {
	case "zcode":
		return "ZCode 实例（实验）"
	case "pi-web":
		return "PI WEB 实例（实验）"
	default:
		return brand + " 实例"
	}
}

func normaliseBrand(brand string) string {
	if brand == "dsh" {
		return ""
	}
	return brand
}

func parseArgs(args []string) (options, *url.URL, error) {
	var opts options
	flags := flag.NewFlagSet("hdsl-tui", flag.ContinueOnError)
	flags.SetOutput(io.Discard)
	flags.Usage = func() {}
	flags.StringVar(&opts.instance, "instance", "", "直接进入该实例，不显示菜单")
	flags.StringVar(&opts.brand, "brand", "", "品牌：kimi | opencode | claude-web | pi-web | zcode（默认 dsh）")
	flags.StringVar(&opts.resume, "resume", "", "附着到该 harness 会话")
	flags.StringVar(&opts.kernel, "kernel", "", "Claude Web 的内核：community | official（默认面板推荐）")
	flags.StringVar(&opts.user, "user", "", "面板用户名（默认 admin）")
	flags.StringVar(&opts.ca, "ca", "", "额外信任的 CA 证书（PEM 文件）")
	flags.BoolVar(&opts.insecure, "insecure", false, "跳过证书校验（终端上打印指纹并要求确认；非交互运行时打印警告后继续）")
	flags.BoolVar(&opts.showVersion, "version", false, "打印客户端版本与协议号后退出")
	flags.BoolVar(&opts.verbose, "verbose", false, "把请求细节写到 stderr")
	flags.BoolVar(&opts.plain, "plain", false, "使用逐行问答的旧界面（脚本/无终端时自动启用）")

	// Go's flag package stops at the first positional argument, so
	// `hdsl-tui URL --insecure` used to be read as two addresses and refused.
	// Parsing again from after each address accepts both orders; a second
	// address is still counted below and still refused.
	var addresses []string
	remaining := args
	for {
		if err := flags.Parse(remaining); err != nil {
			if errors.Is(err, flag.ErrHelp) {
				return options{}, nil, flag.ErrHelp
			}
			return options{}, nil, fmt.Errorf("%w\n\n%s", err, usageText)
		}
		rest := flags.Args()
		if len(rest) == 0 {
			break
		}
		addresses = append(addresses, rest[0])
		remaining = rest[1:]
		if len(remaining) == 0 {
			break
		}
	}
	if opts.showVersion {
		return opts, nil, nil
	}
	switch opts.brand {
	case "", "dsh", "kimi", "opencode", "claude-web", "pi-web", "zcode":
	default:
		return options{}, nil, fmt.Errorf("--brand 只能是 kimi | opencode | claude-web | pi-web | zcode（或留空表示 dsh），收到 %q\n\n%s", opts.brand, usageText)
	}
	switch opts.kernel {
	case "", "community", "official":
	default:
		return options{}, nil, fmt.Errorf("--kernel 只能是 community | official（Claude Web 才需要），收到 %q\n\n%s", opts.kernel, usageText)
	}
	if len(addresses) > 1 {
		return options{}, nil, fmt.Errorf("只需要一个面板地址，收到 %d 个\n\n%s", len(addresses), usageText)
	}
	if len(addresses) == 0 {
		// No address is fine: the interactive client starts with its server
		// picker. Plain mode rejects it later, with the usage text.
		return opts, nil, nil
	}
	base, err := url.Parse(strings.TrimRight(strings.TrimSpace(addresses[0]), "/"))
	if err != nil || (base.Scheme != "http" && base.Scheme != "https") || base.Host == "" {
		return options{}, nil, fmt.Errorf("面板地址必须是 http:// 或 https:// 开头的完整地址，收到 %q\n\n%s", addresses[0], usageText)
	}
	return opts, base, nil
}

func usage(w io.Writer) { fmt.Fprint(w, usageText) }

const usageText = `hdsl-tui — 在终端里进入 HDSL-Web 面板的实例

用法:
  hdsl-tui [选项] [面板地址]

  选项写在地址前面或后面都一样：
  hdsl-tui --insecure https://panel.test 与
  hdsl-tui https://panel.test --insecure 是同一条命令。

  不带面板地址时进入全屏菜单：选择已保存的服务器或输入新地址，登录一次，
  再按「分类 → 实例 → 动作」操作。Esc 逐级返回，Ctrl-C 取消。

  带面板地址时直接连过去（菜单仍会显示，地址只是跳过选择那一步）。

选项:
  --instance <id>   直接进入该实例的终端（仍会显示菜单）
  --brand <name>    品牌：kimi | opencode | claude-web | pi-web | zcode（默认 dsh）
  --resume <id>     附着到该 harness 会话（kimi / opencode）
  --kernel <name>   Claude Web 内核：community | official（默认面板推荐）
  --user <name>     面板用户名，默认 admin；给了就不再提示
  --ca <pem>        额外信任的 CA 证书（PEM 文件）
  --insecure        全局跳过证书校验：先打印 SHA-256 指纹；终端上要求输入 yes，
                    stdin 不是终端（脚本/无人值守）时打印警告后继续
                    （只想跳过一次时不用它：菜单会在证书校验失败时当场询问）
  --plain           使用逐行问答的旧界面（无终端时自动启用）
  --version         打印客户端版本与协议号后退出（不联网）
  --verbose         把请求细节写到 stderr
  -h, --help        显示本帮助

菜单里的按键:
  ↑/↓ 或 j/k        移动光标        Enter 确认
  Esc               返回上一级      Ctrl-C 取消 / 退出
  输入框里:
  Ctrl-U            清空当前输入

  在 agent 终端里:
  Ctrl-] q          离开终端，agent 继续跑，回到菜单
  Ctrl-] k          停掉终端再回到菜单
  Ctrl-] Ctrl-]     给 agent 发一个字面量 Ctrl-]
  Ctrl-C            原样发给 agent

退出码:
  0  正常结束（含菜单里选择退出、Ctrl-] q 离开）
  2  认证或提权失败
  3  无法连接面板 / 连接中断
  4  策略拒绝：面板不在 HTTPS、Web Shell 已关闭、实例没装 TUI 插件、面板启动终端失败、
     Windows 面板的桌面终端
  5  用法错误
`

type logger struct {
	w       io.Writer
	enabled bool
}

func (l logger) Logf(format string, args ...any) {
	if l.enabled {
		fmt.Fprintf(l.w, "[debug] "+format+"\n", args...)
	}
}
