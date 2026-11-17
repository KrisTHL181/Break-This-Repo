package panel

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"syscall"
	"testing"
	"time"
)

// handshakeChain is how any transport failure arrives: the client wraps it in a
// TransportError and net/http in a *url.Error.
func handshakeChain(cause error) error {
	return &TransportError{
		Op: "GET /api/health",
		Err: &url.Error{
			Op:  "Get",
			URL: "https://192.0.2.10:3080/api/health",
			Err: cause,
		},
	}
}

// verificationChain is the certificate case: Go ≥1.20 adds
// *tls.CertificateVerificationError around the x509 error, and it carries the
// certificates that were rejected.
func verificationChain(cause error) error {
	return handshakeChain(&tls.CertificateVerificationError{
		Err:                    cause,
		UnverifiedCertificates: []*x509.Certificate{{}},
	})
}

func TestCertificateFailureNamesTheMissingSAN(t *testing.T) {
	leaf := &x509.Certificate{
		Subject:  pkix.Name{CommonName: "panel.example.com"},
		DNSNames: []string{"panel.example.com"},
	}
	err := verificationChain(x509.HostnameError{Certificate: leaf, Host: "192.0.2.10"})

	certificate, ok := CertificateFailure(err)
	if !ok {
		t.Fatalf("CertificateFailure missed a hostname failure: %v", err)
	}
	if certificate.Host != "192.0.2.10" {
		t.Errorf("Host = %q, want the address that failed", certificate.Host)
	}
	if !strings.Contains(certificate.Reason, "192.0.2.10") {
		t.Errorf("Reason = %q, want it to name the address", certificate.Reason)
	}
}

func TestCertificateFailureRecognisesAnUntrustedCA(t *testing.T) {
	certificate, ok := CertificateFailure(verificationChain(x509.UnknownAuthorityError{}))
	if !ok {
		t.Fatal("CertificateFailure missed an unknown authority")
	}
	if !strings.Contains(certificate.Reason, "CA") {
		t.Errorf("Reason = %q, want it to mention the CA", certificate.Reason)
	}
}

func TestCertificateFailureRecognisesAnExpiredCertificate(t *testing.T) {
	certificate, ok := CertificateFailure(verificationChain(x509.CertificateInvalidError{
		Cert:   &x509.Certificate{},
		Reason: x509.Expired,
	}))
	if !ok {
		t.Fatal("CertificateFailure missed an invalid certificate")
	}
	if !strings.Contains(certificate.Reason, "过期") {
		t.Errorf("Reason = %q, want it to mention expiry", certificate.Reason)
	}
}

// The wording of these errors is not a contract, so the failure has to be
// recognised even when nothing but the type survives the wrapping.
func TestCertificateFailureSeesThroughOuterWrappers(t *testing.T) {
	inner := verificationChain(x509.HostnameError{Certificate: &x509.Certificate{}, Host: "192.0.2.10"})
	outer := &url.Error{Op: "Get", URL: "https://192.0.2.10/", Err: inner}
	if _, ok := CertificateFailure(outer); !ok {
		t.Fatal("CertificateFailure lost the failure behind two url.Error layers")
	}
}

func TestCertificateFailureLeavesOtherTransportFailuresAlone(t *testing.T) {
	cases := map[string]error{
		"nil":             nil,
		"refused":         handshakeChain(&net.OpError{Op: "dial", Net: "tcp", Err: &net.AddrError{Err: "refused"}}),
		"no such host":    handshakeChain(&net.DNSError{Err: "no such host", Name: "panel.test"}),
		"plain text port": handshakeChain(tls.RecordHeaderError{Msg: "first record does not look like a TLS handshake"}),
		"ECONNRESET":      &TransportError{Op: "GET /api/health", Err: syscall.ECONNRESET},
		"unrelated":       errors.New("boom"),
		// A verification wrapper with no rejected certificate is a handshake
		// problem of some other kind; it is not an identity the user may accept.
		"verification without a certificate": handshakeChain(&tls.CertificateVerificationError{Err: errors.New("handshake failure")}),
	}
	for name, err := range cases {
		if certificate, ok := CertificateFailure(err); ok {
			t.Errorf("%s: treated as a certificate failure (%q)", name, certificate.Reason)
		}
	}
}

// A real handshake, not a constructed error: the panel's certificate is
// self-signed, so an ordinary client must classify the failure as one the user
// may knowingly accept — and the same request must go through once the
// certificate is trusted, or the classification would be hiding a broken client.
func TestCertificateFailureOnARealHandshake(t *testing.T) {
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		fmt.Fprint(w, `{"status":"ok","version":"test"}`)
	}))
	defer server.Close()

	distrusting, err := New(server.URL, &http.Client{Timeout: 5 * time.Second}, nil)
	if err != nil {
		t.Fatal(err)
	}
	_, err = distrusting.Health(context.Background())
	if err == nil {
		t.Fatal("the self-signed panel answered without certificate verification")
	}
	certificate, ok := CertificateFailure(err)
	if !ok {
		t.Fatalf("CertificateFailure missed a real handshake failure: %v", err)
	}
	if certificate.Reason == "" {
		t.Error("the failure carries no reason for the dialog")
	}

	trusting, err := New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := trusting.Health(context.Background()); err != nil {
		t.Fatalf("a trusted certificate was still refused: %v", err)
	}
	if _, ok := CertificateFailure(err); ok {
		t.Error("a successful call was classified as a certificate failure")
	}
}
