package panel

import (
	"crypto/tls"
	"crypto/x509"
	"errors"
	"fmt"
)

// CertificateError is a TLS identity failure: the handshake reached the panel,
// but the certificate did not prove it is the panel the client asked for. It is
// kept apart from TransportError because this is the one transport failure a
// user can knowingly accept for a single run — a refused connection, a DNS
// failure or a timeout is not.
type CertificateError struct {
	// Host is the name or address the client asked for, when the failure names
	// one.
	Host string
	// Reason is one actionable sentence, short enough for the dialog.
	Reason string
	// Err keeps the handshake error reachable for --verbose.
	Err error
}

func (e *CertificateError) Error() string {
	if e.Reason == "" {
		return "面板证书未通过校验"
	}
	return "面板证书未通过校验：" + e.Reason
}

func (e *CertificateError) Unwrap() error { return e.Err }

// CertificateFailure recognises certificate failures by type instead of by
// message: the wording of these errors is not a contract, and a Go upgrade must
// not silently replace the offer to connect with a different button.
//
// The chain is usually TransportError → *url.Error → *tls.CertificateVerificationError
// → x509.*; errors.As walks all of it, so the wrapping in between does not
// matter.
func CertificateFailure(err error) (*CertificateError, bool) {
	if err == nil {
		return nil, false
	}
	var hostname x509.HostnameError
	if errors.As(err, &hostname) {
		host := hostname.Host
		if host == "" {
			return &CertificateError{
				Reason: "证书里的名字与这个地址不一致",
				Err:    err,
			}, true
		}
		return &CertificateError{
			Host:   host,
			Reason: fmt.Sprintf("证书里没有 %s，它只签给了别的名字（用 IP 访问只含域名的证书时最常见）", host),
			Err:    err,
		}, true
	}
	var unknown x509.UnknownAuthorityError
	if errors.As(err, &unknown) {
		return &CertificateError{
			Reason: "签发这张证书的 CA 不在本机信任列表里（自签名证书或私有 CA）",
			Err:    err,
		}, true
	}
	var invalid x509.CertificateInvalidError
	if errors.As(err, &invalid) {
		return &CertificateError{Reason: invalidReason(invalid.Reason), Err: err}, true
	}
	// Go ≥1.20 reports a failed verification as this type. It unwraps to the
	// x509 error in every case above, so reaching here means a verification
	// failure with a cause the switch does not name. It only counts when a
	// certificate was actually left unverified: a handshake can carry other
	// problems in the same wrapper, and those are not something the user may
	// wave through.
	var verification *tls.CertificateVerificationError
	if errors.As(err, &verification) && len(verification.UnverifiedCertificates) > 0 {
		return &CertificateError{Reason: "证书没有通过校验", Err: err}, true
	}
	return nil, false
}

func invalidReason(reason x509.InvalidReason) string {
	switch reason {
	case x509.Expired:
		return "证书已经过期，或还没到生效时间"
	case x509.NotAuthorizedToSign:
		return "签发者不被允许签发证书（它不是 CA）"
	case x509.CANotAuthorizedForThisName:
		return "这个 CA 不被允许为这个地址签发证书"
	case x509.TooManyIntermediates:
		return "证书链的中间证书过多"
	case x509.IncompatibleUsage:
		return "证书的用途与 TLS 服务端不匹配"
	case x509.NameConstraintsWithoutSANs:
		return "证书只写了名称约束，没有写地址"
	case x509.UnconstrainedName:
		return "证书签发给了不受约束的名字"
	case x509.TooManyConstraints:
		return "证书的名称约束过多"
	case x509.CANotAuthorizedForExtKeyUsage:
		return "这个 CA 不被允许签发这种用途的证书"
	}
	return "证书不可用"
}
