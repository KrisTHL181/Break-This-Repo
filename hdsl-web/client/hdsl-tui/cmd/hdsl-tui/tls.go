package main

import (
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"encoding/hex"
	"fmt"
	"io"
	"net"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
)

// tlsConfigFor builds the transport security for both the REST calls and the
// WebSocket dial. --ca adds a private CA; --insecure is the last resort and
// has to be confirmed by typing `yes` after seeing the fingerprint.
func tlsConfigFor(caPath string, insecure bool, base *url.URL, stdin io.Reader, stderr io.Writer, logf func(string, ...any)) (*tls.Config, error) {
	config := &tls.Config{MinVersion: tls.VersionTLS12}

	if caPath != "" {
		pem, err := os.ReadFile(caPath)
		if err != nil {
			return nil, fmt.Errorf("读取 --ca 证书失败: %w", err)
		}
		pool, err := x509.SystemCertPool()
		if err != nil || pool == nil {
			pool = x509.NewCertPool()
		}
		if !pool.AppendCertsFromPEM(pem) {
			return nil, fmt.Errorf("--ca 文件里没有可用的 PEM 证书: %s", caPath)
		}
		config.RootCAs = pool
		logf("已加入自定义 CA: %s", caPath)
	}

	if !insecure {
		return config, nil
	}
	if base == nil {
		// Interactive mode: the address is picked later, so there is no
		// certificate to fingerprint yet. The app shows its own warning page
		// before the server picker (app.Options.Insecure).
		config.InsecureSkipVerify = true
		return config, nil
	}
	if base.Scheme != "https" {
		fmt.Fprintln(stderr, "提示：面板地址不是 https，--insecure 没有作用。")
		return config, nil
	}
	if err := confirmFingerprint(base, stdin, stderr, logf); err != nil {
		return nil, err
	}
	config.InsecureSkipVerify = true
	return config, nil
}

// confirmFingerprint shows what the certificate actually is and, on a
// terminal, requires a typed confirmation. Skipping verification silently is
// exactly the habit that makes TLS pointless, so the fingerprint is printed
// first — also on the non-interactive path, where nobody can answer and the
// run proceeds on the strength of the explicit `--insecure` flag alone.
func confirmFingerprint(base *url.URL, stdin io.Reader, stderr io.Writer, logf func(string, ...any)) error {
	address := base.Host
	if base.Port() == "" {
		address = net.JoinHostPort(base.Hostname(), "443")
	}
	dialer := &net.Dialer{Timeout: 10 * time.Second}
	probe := &tls.Config{InsecureSkipVerify: true, ServerName: base.Hostname(), MinVersion: tls.VersionTLS12}
	conn, err := tls.DialWithDialer(dialer, "tcp", address, probe)
	if err != nil {
		return fmt.Errorf("无法读取面板证书: %w", err)
	}
	defer conn.Close()
	certificates := conn.ConnectionState().PeerCertificates
	if len(certificates) == 0 {
		return fmt.Errorf("面板没有提供证书")
	}
	leaf := certificates[0]
	sum := sha256.Sum256(leaf.Raw)

	fmt.Fprintf(stderr, "面板证书 SHA-256 指纹: %s\n", fingerprint(sum[:]))
	fmt.Fprintf(stderr, "  颁发对象: %s\n", leaf.Subject)
	fmt.Fprintf(stderr, "  有效期至: %s\n", leaf.NotAfter.Format(time.RFC3339))
	fmt.Fprintln(stderr, "跳过证书校验会让任何人都能冒充这台面板。指纹核对无误再继续。")
	logf("--insecure: 已跳过证书校验")

	// A script, a CI job or an ECS console cannot answer a prompt. Dying there
	// ("需要交互确认，但当前无法读取输入") left such runs with no way to proceed
	// at all — and `--insecure` was typed on purpose. The warning above is
	// printed either way; only the typed confirmation is dropped.
	if file, ok := stdin.(*os.File); !ok || !termx.IsTerminal(file) {
		logf("--insecure: stdin 不是终端，跳过指纹确认（脚本/无人值守用法）")
		return nil
	}
	answer, err := termx.ReadLine(stdin, stderr, "确认继续请输入 yes: ")
	if err != nil {
		return fmt.Errorf("--insecure 需要交互确认，但当前无法读取输入: %w", err)
	}
	if strings.TrimSpace(answer) != "yes" {
		return fmt.Errorf("指纹未被确认，已取消连接")
	}
	return nil
}

func fingerprint(sum []byte) string {
	octets := make([]string, len(sum))
	for index, value := range sum {
		octets[index] = hex.EncodeToString([]byte{value})
	}
	return strings.Join(octets, ":")
}
