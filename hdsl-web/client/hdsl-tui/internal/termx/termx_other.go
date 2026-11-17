//go:build !unix && !windows

package termx

import "os"

// Open has no raw-mode support outside unix and windows; such builds behave
// like a pipe, which is what Nop already models.
func Open(in, out *os.File) (Raw, error) { return Nop{}, nil }
