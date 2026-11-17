package cli

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"net/url"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
)

// The scripted flow cannot show a dialog, so the sentence has to carry the way
// out: the two flags that make this panel reachable again.
func TestExplainTurnsACertificateFailureIntoAdvice(t *testing.T) {
	err := &panel.TransportError{
		Op: "GET /api/health",
		Err: &url.Error{
			Op:  "Get",
			URL: "https://192.0.2.10:3080/api/health",
			Err: &tls.CertificateVerificationError{
				Err:                    x509.HostnameError{Certificate: &x509.Certificate{}, Host: "192.0.2.10"},
				UnverifiedCertificates: []*x509.Certificate{{}},
			},
		},
	}

	code, text := Explain(err)
	if code != ExitTransport {
		t.Errorf("exit code = %d, want the transport code", code)
	}
	for _, want := range []string{"192.0.2.10", "--ca", "--insecure"} {
		if !strings.Contains(text, want) {
			t.Errorf("text = %q, want it to mention %q", text, want)
		}
	}
}

func TestExplainLeavesPlainTransportFailuresAlone(t *testing.T) {
	refused := &panel.TransportError{Op: "GET /api/health", Err: errors.New("dial tcp 192.0.2.10:3080: connect: connection refused")}
	_, text := Explain(refused)
	if !strings.Contains(text, "无法连接面板") {
		t.Errorf("text = %q, want the plain transport wording", text)
	}
	if strings.Contains(text, "--insecure") {
		t.Errorf("text = %q offers a certificate bypass for a refused connection", text)
	}
}
