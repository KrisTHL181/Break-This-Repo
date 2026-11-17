package panel_test

import (
	"context"
	"net/http"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel/fakeserver"
)

func TestInstanceCarriesThePanelFields(t *testing.T) {
	server := fakeserver.New()
	server.DSHInstances = []map[string]any{{
		"id": "dsh-1", "name": "main", "state": "FAILED",
		"version": "0.2.1", "port": 41234, "url": "/i/dsh-1/?token=secret",
		"error": "exit code 1", "account": "deepseek",
	}}
	defer server.Close()
	client := newClient(t, server)

	instances, err := client.Instances(context.Background())
	if err != nil {
		t.Fatalf("Instances: %v", err)
	}
	if len(instances) != 1 {
		t.Fatalf("instances = %d, want 1", len(instances))
	}
	instance := instances[0]
	if instance.Version != "0.2.1" {
		t.Errorf("Version = %q", instance.Version)
	}
	if instance.PortOf() != 41234 {
		t.Errorf("PortOf = %d, want 41234", instance.PortOf())
	}
	if instance.Error != "exit code 1" {
		t.Errorf("Error = %q", instance.Error)
	}
	if instance.Account != "deepseek" {
		t.Errorf("Account = %q", instance.Account)
	}
	if instance.URL != "/i/dsh-1/?token=secret" {
		t.Errorf("URL = %q", instance.URL)
	}
}

func TestBrandInstancePortFallsBackToLastPort(t *testing.T) {
	server := fakeserver.New()
	server.BrandInstances = map[string][]map[string]any{
		"kimi": {{"id": "kimi-1", "name": "one", "state": "running", "lastPort": 42000}},
	}
	defer server.Close()
	client := newClient(t, server)

	instances, err := client.BrandInstances(context.Background(), "kimi")
	if err != nil {
		t.Fatalf("BrandInstances: %v", err)
	}
	if got := instances[0].PortOf(); got != 42000 {
		t.Errorf("PortOf = %d, want 42000", got)
	}
}

func TestLaunchAndStopUseTheSurfacePath(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	if err := client.LaunchInstance(context.Background(), "kimi", "kimi-1"); err != nil {
		t.Fatalf("LaunchInstance: %v", err)
	}
	request, ok := server.LastRequestTo("/api/brands/kimi/instances/kimi-1/launch")
	if !ok {
		t.Fatalf("launch was not sent to the brand surface; requests: %+v", server.Requests())
	}
	if request.Method != http.MethodPost {
		t.Errorf("launch method = %s, want POST", request.Method)
	}

	if err := client.StopInstance(context.Background(), "", "dsh-1"); err != nil {
		t.Fatalf("StopInstance: %v", err)
	}
	if _, ok := server.LastRequestTo("/api/instances/dsh-1/stop"); !ok {
		t.Fatalf("stop was not sent to the DSH surface; requests: %+v", server.Requests())
	}
}

func TestMintAgentToken(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	ticket, err := client.MintAgentToken(context.Background(), "hdsl-tui", []string{"read", "mc"})
	if err != nil {
		t.Fatalf("MintAgentToken: %v", err)
	}
	if ticket.Token != "agent-token-hdsl-tui" {
		t.Errorf("token = %q", ticket.Token)
	}
	if len(ticket.Scopes) != 2 || ticket.Scopes[0] != "read" || ticket.Scopes[1] != "mc" {
		t.Errorf("scopes = %v", ticket.Scopes)
	}
	if ticket.Expiry().IsZero() {
		t.Errorf("ExpiresAt did not parse: %q", ticket.ExpiresAt)
	}

	request, ok := server.LastRequestTo("/api/developer/agent-token")
	if !ok {
		t.Fatal("the mint request was not sent")
	}
	if request.Method != http.MethodPost {
		t.Errorf("method = %s, want POST", request.Method)
	}
	if !strings.Contains(request.Body, `"read"`) || !strings.Contains(request.Body, `"mc"`) {
		t.Errorf("body does not carry the scopes: %s", request.Body)
	}
	if !strings.Contains(request.Body, `"hdsl-tui"`) {
		t.Errorf("body does not carry the label: %s", request.Body)
	}
}

func TestMintAgentTokenWithoutElevationIs403(t *testing.T) {
	server := fakeserver.New()
	server.Elevated = false
	defer server.Close()
	client := newClient(t, server)

	_, err := client.MintAgentToken(context.Background(), "hdsl-tui", []string{"read"})
	if panel.Status(err) != http.StatusForbidden {
		t.Fatalf("status = %d, want 403 (%v)", panel.Status(err), err)
	}
	if !strings.Contains(err.Error(), "developer_elevation_required") {
		t.Errorf("error = %v, want the panel's reason", err)
	}
}
