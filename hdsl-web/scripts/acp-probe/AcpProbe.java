import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket
public class AcpProbe {
    static final CountDownLatch done = new CountDownLatch(1);
    static volatile int exit = 3;
    static volatile Session session;
    static volatile String instanceId;

    public static void main(String[] args) throws Exception {
        String url = args[0];
        String cookie = args[1];
        instanceId = args[2];

        WebSocketClient client = new WebSocketClient();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Cookie", cookie);
        session = client.connect(new AcpProbe(), URI.create(url), request).get(15, TimeUnit.SECONDS);

        session.sendText("{\"type\":\"subscribe\",\"topics\":[\"instance:" + instanceId + "\"]}", org.eclipse.jetty.websocket.api.Callback.NOOP);
        session.sendText("{\"type\":\"acp-start\",\"instance\":\"" + instanceId + "\"}", org.eclipse.jetty.websocket.api.Callback.NOOP);

        if (!done.await(90, TimeUnit.SECONDS)) {
            System.out.println("PROBE-TIMEOUT");
        }
        client.stop();
        System.exit(exit);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("WS<- " + (message.length() > 300 ? message.substring(0, 300) + "…" : message));
        if (message.contains("\"acp-ready\"")) {
            exit = 0;
            session.sendText("{\"type\":\"acp-stop\",\"instance\":\"" + instanceId + "\"}", org.eclipse.jetty.websocket.api.Callback.NOOP);
            done.countDown();
        } else if (message.contains("\"acp-error\"")) {
            exit = 2;
            done.countDown();
        }
    }
}
