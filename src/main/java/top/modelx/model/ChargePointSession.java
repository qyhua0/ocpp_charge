package top.modelx.model;

import org.java_websocket.WebSocket;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Charge point session
 * @author: hua
 * @date: 2025/8/8
 */
public class ChargePointSession {
    private final String chargePointId;
    private final WebSocket conn;
    private volatile Instant lastSeen = Instant.now();
    private volatile String status = "Unknown";
    private volatile Integer currentTransactionId;
    private final Map<Integer, String> connectorStatuses = new ConcurrentHashMap<>();

    public ChargePointSession(String chargePointId, WebSocket conn) {
        this.chargePointId = chargePointId;
        this.conn = conn;
    }

    public String getChargePointId() { return chargePointId; }
    public WebSocket getConn() { return conn; }
    public Instant getLastSeen() { return lastSeen; }
    public void touch() { this.lastSeen = Instant.now(); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentTransactionId() { return currentTransactionId; }
    public void setCurrentTransactionId(Integer id) { this.currentTransactionId = id; }
    public Map<Integer, String> getConnectorStatuses() { return connectorStatuses; }
}