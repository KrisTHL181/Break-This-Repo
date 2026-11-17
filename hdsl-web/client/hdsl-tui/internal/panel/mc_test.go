package panel_test

import (
	"context"
	"net/http"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel/fakeserver"
)

func TestMCInstances(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	instances, err := client.MCInstances(context.Background())
	if err != nil {
		t.Fatalf("MCInstances: %v", err)
	}
	if len(instances) != 1 {
		t.Fatalf("instances = %d, want 1", len(instances))
	}
	instance := instances[0]
	if instance.ID != "mc-1" || instance.Name != "vanilla" {
		t.Errorf("instance = %+v", instance)
	}
	if instance.Port != 25565 {
		t.Errorf("Port = %d", instance.Port)
	}
	if !instance.ConsoleAvailable {
		t.Errorf("ConsoleAvailable = false, want true")
	}
}

func TestMCFeatureOffIs404(t *testing.T) {
	server := fakeserver.New()
	server.Handle("GET", "/api/mc/instances", func(w http.ResponseWriter, r *http.Request) {
		fakeserver.WriteError(w, http.StatusNotFound, "not found")
	})
	defer server.Close()
	client := newClient(t, server)

	_, err := client.MCInstances(context.Background())
	if panel.Status(err) != http.StatusNotFound {
		t.Fatalf("status = %d, want 404 (%v)", panel.Status(err), err)
	}
}

func TestMCConsoleTicket(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	terminal, err := client.MCConsoleTicket(context.Background(), "mc-1")
	if err != nil {
		t.Fatalf("MCConsoleTicket: %v", err)
	}
	if terminal.Mode != "pty" {
		t.Errorf("Mode = %q, want pty", terminal.Mode)
	}
	if terminal.WS == "" || terminal.Ticket == "" {
		t.Errorf("terminal = %+v", terminal)
	}
	request, ok := server.LastRequestTo("/api/mc/instances/mc-1/console/ticket")
	if !ok {
		t.Fatalf("ticket request not sent; requests: %+v", server.Requests())
	}
	if request.Method != http.MethodPost {
		t.Errorf("method = %s, want POST", request.Method)
	}
}

func TestMCStartAndStop(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	if err := client.MCStart(context.Background(), "mc-1"); err != nil {
		t.Fatalf("MCStart: %v", err)
	}
	if _, ok := server.LastRequestTo("/api/mc/instances/mc-1/start"); !ok {
		t.Fatalf("start request not sent; requests: %+v", server.Requests())
	}
	if err := client.MCStop(context.Background(), "mc-1"); err != nil {
		t.Fatalf("MCStop: %v", err)
	}
	if _, ok := server.LastRequestTo("/api/mc/instances/mc-1/stop"); !ok {
		t.Fatalf("stop request not sent; requests: %+v", server.Requests())
	}
}
