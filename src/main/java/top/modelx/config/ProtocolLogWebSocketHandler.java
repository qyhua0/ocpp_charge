package top.modelx.config;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.modelx.model.DeviceMessageEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议日志WebSocket处理器
 *
 * @author: hua
 * @date: 2025/8/14
 */
@Component
public class ProtocolLogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LogManager.getLogger(ProtocolLogWebSocketHandler.class);

    // 存储所有连接的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();


    private final ObjectMapper objectMapper = new ObjectMapper();

    @EventListener
    public void handleDeviceMessage(DeviceMessageEvent event) {
        String payload = String.format("设备 %s: %s", event.getCpId(), event.getMessage());

        broadcastProtocolLog(event);
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("协议日志WebSocket连接已建立, sessionId: {}", sessionId);

        sendToSession(session, "协议日志WebSocket连接已建立，开始接收OCPP协议数据");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("协议日志WebSocket连接已关闭, sessionId: {}, 状态: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("协议日志WebSocket传输错误, sessionId: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }

    /**
     * 向指定会话发送日志数据
     */
    private void sendToSession(WebSocketSession session, String msg) {
        try {
            DeviceMessageEvent logData = new DeviceMessageEvent("", msg, DeviceMessageEvent.Direction.SYSTEM);
            String jsonData =JSONObject.toJSONString(logData);
            session.sendMessage(new TextMessage(jsonData));
        } catch (IOException e) {
            log.error("发送协议日志数据失败, sessionId: {}", session.getId(), e);
        }
    }

    /**
     * 广播协议日志数据到所有连接的客户端
     */
    public static void broadcastProtocolLog(DeviceMessageEvent data) {
        ObjectMapper mapper = new ObjectMapper();
        sessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                return true; // 移除已关闭的连接
            }

            try {
                String jsonData = JSONObject.toJSONString(data);
                session.sendMessage(new TextMessage(jsonData));
                return false;
            } catch (IOException e) {
                log.error("广播协议日志数据失败, sessionId: {}", session.getId(), e);
                return true; // 移除发送失败的连接
            }
        });
    }

}
