package top.modelx.ws;

import top.modelx.service.OcppService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * WebSocket 服务器
 * @author hua
 * @date 2025/8/8
 */
@Component
public class JavaWsServerServer extends WebSocketServer {
    Logger log=  LogManager.getLogger(JavaWsServerServer.class);

    private final OcppService ocppService;

    public JavaWsServerServer(
            OcppService ocppService,
            @Value("${ocpp.ws.port}") int port
    ) {
        super(new InetSocketAddress(port));
        this.ocppService = ocppService;
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(
            WebSocket conn,
            Draft draft,
            ClientHandshake request
    ) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
        String subProtocol = request.getFieldValue("Sec-WebSocket-Protocol");
        if (subProtocol != null && !subProtocol.isEmpty()) {
            builder.put("Sec-WebSocket-Protocol", subProtocol); // 必须回传 ocpp1.6
        }
        return builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("连接: " + handshake.getResourceDescriptor());
        String cpId = extractCpId(handshake.getResourceDescriptor());
        ocppService.register(cpId, conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.info("["+conn.getResourceDescriptor()+"]收到: " + message);
        try {
            ocppService.onIncoming(conn, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ocppService.unregister(conn);
        log.info("断开: " + reason + " (code=" + code + ")");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        log.info("OCPP 测试服务器启动在端口 " + getPort());
    }

    @PostConstruct
    public void startServer() {
        setConnectionLostTimeout(0); // 先关闭
        this.start();
        log.info("WebSocket 服务已启动");
    }

    @PreDestroy
    public void stopServer() throws InterruptedException {
        this.stop();
        log.info("WebSocket 服务已停止");
    }

    private String extractCpId(String path) {
        if (path == null) {
            return "UNKNOWN";
        }
        if(path.startsWith("/ks")){
            String p = path.substring(3);
            return p;
        }

        return "UNKNOWN2";
    }
}
