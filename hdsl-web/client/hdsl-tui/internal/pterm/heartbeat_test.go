package pterm

import (
	"context"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"
)

// The shipped heartbeat has to leave room for a pong: every ping re-arms the
// pong deadline, so a deadline wider than the interval would be pushed back by
// the next ping and never expire — a half-dead link would look healthy forever.
func TestHeartbeatDefaultsLeaveRoomForAPong(t *testing.T) {
	beat := heartbeat
	if beat.pong <= 0 || beat.ping <= 0 {
		t.Fatalf("heartbeat = ping %s / pong %s", beat.ping, beat.pong)
	}
	if beat.pong >= beat.ping {
		t.Errorf("pong deadline %s is not below the ping interval %s: it would never expire",
			beat.pong, beat.ping)
	}
}

// fastHeartbeat shortens the ping interval and the pong deadline for one test,
// in the same shape as the shipped pair (half the interval). The heartbeat is
// the only part of the client whose real timings are tens of seconds, so it is
// the only part that needs the knobs. The interval stays in the hundreds of
// milliseconds: a pong deadline of a few milliseconds would expire on ordinary
// scheduling jitter and the test would be about the machine, not the client.
//
// Dial copies the timing into the connection, so a session that is already
// running keeps its own and restoring this at the end of the test races with
// nothing.
func fastHeartbeat(t *testing.T, interval time.Duration) {
	t.Helper()
	saved := heartbeat
	heartbeat = heartbeatConfig{ping: interval, pong: interval / 2}
	t.Cleanup(func() { heartbeat = saved })
}

func heartbeatURL(server *httptest.Server) string {
	return "ws" + strings.TrimPrefix(server.URL, "http")
}

// A quiet terminal is normal — the agent is thinking — so the client has to ask
// whether the link is still there, and a peer that answers must keep its
// session: pings go out, nothing is torn down.
func TestHeartbeatPingsAQuietSessionWithoutEndingIt(t *testing.T) {
	const interval = 300 * time.Millisecond
	fastHeartbeat(t, interval)

	pings := make(chan string, 16)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		// gorilla's default ping handler answers with a pong; this one does the
		// same and counts, which is what the test is about.
		socket.SetPingHandler(func(data string) error {
			select {
			case pings <- data:
			default:
			}
			return socket.WriteControl(websocket.PongMessage, []byte(data), time.Now().Add(time.Second))
		})
		for {
			if _, _, err := socket.ReadMessage(); err != nil {
				return
			}
		}
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := Dial(ctx, heartbeatURL(server), "", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	done := make(chan error, 1)
	go func() {
		_, err := conn.Run(ctx, Options{In: strings.NewReader(""), Out: io.Discard})
		done <- err
	}()

	// Several silent intervals: a heartbeat that killed a session its peer kept
	// answering would have spoken up by now.
	for index := 0; index < 3; index++ {
		select {
		case <-pings:
		case err := <-done:
			t.Fatalf("ping %d: the session ended (err %v) although the peer answered every ping", index+1, err)
		case <-time.After(5 * time.Second):
			t.Fatalf("ping %d: the client never pinged an idle session", index+1)
		}
	}
	select {
	case err := <-done:
		t.Fatalf("the session ended (err %v) although the peer answered every ping", err)
	case <-time.After(3 * interval):
	}

	// The socket is still usable in both directions, which is the point of not
	// having torn it down.
	if err := conn.SendText([]byte(`{"type":"resize","cols":80,"rows":24}`)); err != nil {
		t.Errorf("SendText after the heartbeats: %v", err)
	}
}

// The failure this exists for: a link that stops carrying anything without ever
// closing (a NAT that dropped the flow, a panel that died silently). The
// session must end by itself instead of sitting there looking attached.
func TestHeartbeatEndsASessionThatStopsAnswering(t *testing.T) {
	const interval = 300 * time.Millisecond
	fastHeartbeat(t, interval)

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		// Pings are swallowed: gorilla's default handler answers them, this one
		// does not, which is exactly what a half-dead link looks like.
		socket.SetPingHandler(func(string) error { return nil })
		for {
			if _, _, err := socket.ReadMessage(); err != nil {
				return
			}
		}
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := Dial(ctx, heartbeatURL(server), "", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	started := time.Now()
	_, err = conn.Run(ctx, Options{In: strings.NewReader(""), Out: io.Discard})
	if err == nil {
		t.Fatal("a session whose pings are never answered ended without an error")
	}
	if !strings.Contains(err.Error(), "心跳") {
		t.Errorf("error = %q, want it to say the heartbeat was lost", err)
	}
	if ctx.Err() != nil {
		t.Fatalf("the context ended first (%v), so this proves nothing about the heartbeat", ctx.Err())
	}
	if elapsed := time.Since(started); elapsed > 4*interval {
		t.Errorf("the dead link took %s to notice; the deadline should fire within one interval", elapsed)
	}
}
