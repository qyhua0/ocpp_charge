package top.modelx.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.modelx.model.ChargePointSession;
import top.modelx.util.OcppMessageUtil;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * occp1.6协议服务
 * @author: hua
 * @date: 2025/8/11
 */
@Service
public class OcppService {

    private final Map<WebSocket, ChargePointSession> sessionsByConn = new ConcurrentHashMap<>();
    private final Map<String, ChargePointSession> sessionsById = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, CompletableFuture<Map<String,Object>>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    /**
     * 注册新连接
     * @param cpId
     * @param conn
     */
    public void register(String cpId, WebSocket conn) {
        ChargePointSession s = new ChargePointSession(cpId, conn);
        sessionsByConn.put(conn, s);
        sessionsById.put(cpId, s);
    }

    /**
     * 连接关闭
     * @param conn
     */
    public void unregister(WebSocket conn) {
        ChargePointSession s = sessionsByConn.remove(conn);
        if (s != null) {
            sessionsById.remove(s.getChargePointId());
        }
    }

    /**
     * 根据连接拿会话
     * @param conn
     * @return
     */
    public ChargePointSession byConn(WebSocket conn) { return sessionsByConn.get(conn); }

    /**
     * 根据桩ID拿会话
     * @param cpId
     * @return
     */
    public ChargePointSession byId(String cpId) { return sessionsById.get(cpId); }

    /**
     * 列出当前连接资料
     * @return
     */
    public List<Map<String, Object>> listConnections() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ChargePointSession s : sessionsById.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("chargePointId", s.getChargePointId());
            row.put("status", s.getStatus());
            row.put("currentTransactionId", s.getCurrentTransactionId());
            row.put("lastSeen", s.getLastSeen().toString());
            row.put("connectors", s.getConnectorStatuses());
            list.add(row);
        }
        return list;
    }

    /**
     * 获取某桩状态资料
     * @param cpId
     * @return
     */
    public Map<String, Object> getStatus(String cpId) {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            return Collections.singletonMap("error", "not connected");
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("chargePointId", s.getChargePointId());
        res.put("status", s.getStatus());
        res.put("currentTransactionId", s.getCurrentTransactionId());
        res.put("lastSeen", s.getLastSeen().toString());
        res.put("connectors", s.getConnectorStatuses());
        return res;
    }

    /**
     * 下发远程启动充电
     * @param cpId
     * @param idTag
     * @param connectorId
     * @return
     * @throws Exception
     */
    public String remoteStart(String cpId, String idTag, Integer connectorId) throws Exception {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            throw new IllegalStateException("Charge point not connected: " + cpId);
        }

        String uid = OcppMessageUtil.newUid();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idTag", idTag);
        if (connectorId != null) {
            payload.put("connectorId", connectorId);
        }

        String frame = OcppMessageUtil.buildCall(uid, "RemoteStartTransaction", payload);
        System.out.println("remoteStart - Sending -> " + frame);
        s.getConn().send(frame);
        return uid;
    }

    /** 下发远程结束充电 */
    public String remoteStop(String cpId, int transactionId) throws Exception {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            throw new IllegalStateException("Charge point not connected: " + cpId);
        }

        String uid = OcppMessageUtil.newUid();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transactionId", transactionId);

        String frame = OcppMessageUtil.buildCall(uid, "RemoteStopTransaction", payload);
        System.out.println("remoteStop - Sending -> " + frame);
        s.getConn().send(frame);
        return uid;
    }


    public void onIncoming(WebSocket conn, String text) throws Exception {
        List<Object> arr = OcppMessageUtil.parseArray(text);
        int msgType = (int) (arr.get(0) instanceof Integer ? arr.get(0) : ((Number)arr.get(0)).intValue());

        if (msgType == 2) { // CALL
            String uid = (String) arr.get(1);
            String action = (String) arr.get(2);
            Map<String, Object> payload = arr.size() > 3 && arr.get(3) instanceof Map ? (Map<String, Object>) arr.get(3) : Collections.emptyMap();
            handleCall(conn, uid, action, payload);
            return;
        }

        if (msgType == 3) { // CALLRESULT
            String uid = (String) arr.get(1);
            Map<String,Object> result = arr.size() > 2 && arr.get(2) instanceof Map ? (Map<String,Object>) arr.get(2) : Collections.emptyMap();
            CompletableFuture<Map<String,Object>> fut = pendingRequests.remove(uid);
            System.out.println("CALLRESULT uid=" + uid+ " result=" + result);
            if (fut != null) {
                fut.complete(result);
                return;
            }
            return;
        }


        if (msgType == 4) { // CALLERROR
            String uid = (String) arr.get(1);
            String errorCode = (String) arr.get(2);
            String desc = (String) arr.get(3);
            CompletableFuture<Map<String,Object>> fut = pendingRequests.remove(uid);
            if (fut != null) {
                Map<String,Object> err = new LinkedHashMap<>();
                err.put("errorCode", errorCode);
                err.put("errorDescription", desc);
                fut.complete(err);
                return;
            }
            System.err.println("CALLERROR uid=" + uid + " code=" + errorCode + " desc=" + desc);
        }
    }

    /**
     * 统一处理
     * @param conn
     * @param uid
     * @param action
     * @param payload
     * @throws Exception
     */
    private void handleCall(WebSocket conn, String uid, String action, Map<String, Object> payload) throws Exception {
        ChargePointSession s = byConn(conn);
        if (s != null) {
            s.touch();
        }

        switch (action) {
            case "BootNotification": {
                System.out.println("BootNotification");
                if (s != null) {
                    s.setStatus("Available");
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("currentTime", Instant.now().toString());
                result.put("interval", 60);
                result.put("status", "Accepted");
                conn.send(OcppMessageUtil.buildCallResult(uid, result));
                break;
            }
            case "Heartbeat": {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("currentTime", Instant.now().toString());
                conn.send(OcppMessageUtil.buildCallResult(uid, result));
                break;
            }
            case "StatusNotification": {
                System.out.println("StatusNotification");
                Integer connectorId = toInt(payload.get("connectorId"));
                String status = str(payload.get("status"));
                if (s != null && connectorId != null && status != null) {
                    s.getConnectorStatuses().put(connectorId, status);
                    s.setStatus(status);
                }
                conn.send(OcppMessageUtil.buildCallResult(uid, Collections.emptyMap()));
                break;
            }
            case "Authorize": {
                System.out.println("Authorize");
                Map<String, Object> idTagInfo = new LinkedHashMap<>();
                idTagInfo.put("status", "Accepted");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("idTagInfo", idTagInfo);
                conn.send(OcppMessageUtil.buildCallResult(uid, result));
                break;
            }
            case "StartTransaction": {
                System.out.println("StartTransaction");
                int txId = OcppMessageUtil.genTransactionId();
                if (s != null) {
                    s.setCurrentTransactionId(txId);
                }
                Map<String, Object> idTagInfo = new LinkedHashMap<>();
                idTagInfo.put("status", "Accepted");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("transactionId", txId);
                result.put("idTagInfo", idTagInfo);
                conn.send(OcppMessageUtil.buildCallResult(uid, result));
                break;
            }
            case "StopTransaction": {
                System.out.println("StopTransaction");
                if (s != null) {
                    s.setCurrentTransactionId(null);
                }
                Map<String, Object> result = new LinkedHashMap<>();
                Map<String, Object> idTagInfo = new LinkedHashMap<>();
                idTagInfo.put("status", "Accepted");
                result.put("idTagInfo", idTagInfo);
                conn.send(OcppMessageUtil.buildCallResult(uid, result));
                break;
            }
            case "MeterValues": {
                System.out.println("MeterValues");
                conn.send(OcppMessageUtil.buildCallResult(uid, Collections.emptyMap()));
                break;
            }
            default: {
                // 未实现的 Action，回错误或空成功
                conn.send(OcppMessageUtil.buildCallError(uid, "NotImplemented", "Action not implemented", null));
            }
        }
    }




    //仅发送（不等待）
    public String sendAction(String cpId, String action, Map<String,Object> payload) throws Exception {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            throw new IllegalStateException("Charge point not connected: " + cpId);
        }
        String uid = OcppMessageUtil.newUid();
        String frame = OcppMessageUtil.buildCall(uid, action, payload == null ? Collections.emptyMap() : payload);
        System.out.println(" sendAction -> "+ frame);
        s.getConn().send(frame);
        return uid;
    }

    //发送并等待 CALLRESULT / CALLERROR（超时秒）
    public Map<String,Object> sendActionAndWait(String cpId, String action, Map<String,Object> payload, long timeoutSeconds) throws Exception {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            throw new IllegalStateException("Charge point not connected: " + cpId);
        }

        String uid = OcppMessageUtil.newUid();
        CompletableFuture<Map<String,Object>> fut = new CompletableFuture<>();
        pendingRequests.put(uid, fut);

        // 超时清理
        scheduler.schedule(() -> {
            CompletableFuture<Map<String,Object>> f = pendingRequests.remove(uid);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new TimeoutException("Timeout waiting for " + action));
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        String frame = OcppMessageUtil.buildCall(uid, action, payload == null ? Collections.emptyMap() : payload);
        System.out.println("Wait Sending -> " + frame);
        s.getConn().send(frame);

        try {
            Map<String,Object> res = fut.get(timeoutSeconds + 1, TimeUnit.SECONDS);
            return res;
        } finally {
            pendingRequests.remove(uid);
        }
    }


    /**
     * 查询桩端本地名单版本号（GetLocalListVersion）
     *
     * */
    public Map<String,Object> getLocalListVersion(String cpId, long timeoutSeconds) throws Exception {
        return sendActionAndWait(cpId, "GetLocalListVersion", Collections.emptyMap(), timeoutSeconds);
    }

    /**
     * 下发本地白名单（SendLocalList - Full/Differential）
     * */
    public Map<String,Object> sendLocalList(String cpId, int listVersion, String updateType,
                                            List<Map<String,Object>> localAuthorisationList,
                                            long timeoutSeconds,boolean isWait) throws Exception {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("listVersion", listVersion);
        payload.put("updateType", updateType); // "Full" 或 "Differential"
        if (localAuthorisationList != null) {
            payload.put("localAuthorisationList", localAuthorisationList);
        }

        if (isWait) {
            return sendActionAndWait(cpId, "SendLocalList", payload, timeoutSeconds);
        } else {
             sendAction(cpId, "SendLocalList", payload);
             return null;
        }
    }


    public Map<String,Object> debugSendActionAndWait(String cpId, String action, Map<String,Object> payload, long timeoutSeconds) throws Exception {
        ChargePointSession s = byId(cpId);
        if (s == null) {
            throw new IllegalStateException("not connected: " + cpId);
        }
        WebSocket conn = s.getConn();

        System.out.println("debugSendAction -> connOpen=" + conn.isOpen() + " cpId=" + cpId + " action=" + action + " payload=" + mapper.writeValueAsString(payload));
        if (!conn.isOpen()) {
            throw new IllegalStateException("WebSocket not open for " + cpId);
        }

        // 触发一次低层 ping（促使设备网络栈活跃并查看 pong）
        try {
            conn.sendPing();
            System.out.println("sent low-level ping");
        } catch (Exception e) {
            System.out.println("sendPing failed: " + e.getMessage());
        }

        // 使用已有的 sendActionAndWait
        return sendActionAndWait(cpId, action, payload, timeoutSeconds);
    }

    // 先查询版本再下发（推荐做法）
    public Map<String,Object> tryGetLocalListVersionThenSend(String cpId, int desiredListVersion, List<Map<String,Object>> localList) throws Exception {
        // 1) try get version
        Map<String,Object> gv;
        try {
            gv = getLocalListVersion(cpId, 8);
            System.out.println("GetLocalListVersion result: " + gv);
        } catch (Exception e) {
            System.err.println("GetLocalListVersion no response: " + e.getMessage());
            // 仍然尝试下发，但记日志
            gv = null;
        }

        // 若 gv 包含 listVersion，确保我们发送的版本大于设备版本
        if (gv != null && gv.get("listVersion") instanceof Number) {
            int deviceVer = ((Number) gv.get("listVersion")).intValue();
            if (desiredListVersion <= deviceVer) {
                desiredListVersion = deviceVer + 1;
                System.out.println("bumped desiredListVersion -> " + desiredListVersion);
            }
        }

        // 2) 组装 payload（确保字段精确）
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("listVersion", desiredListVersion);
        payload.put("updateType", "Full"); // 或 Differential
        payload.put("localAuthorisationList", localList);

        try {
            Map<String,Object> res = sendLocalList(cpId, desiredListVersion, "Full", localList, 12, true);
            System.out.println("SendLocalList result: " + res);
            return res;
        } catch (Exception ex) {
            System.err.println("SendLocalList failed: " + ex.getMessage());
            throw ex;
        }
    }

    // 测试 (让 ChargePoint 主动发送某消息)
    public Map<String,Object> triggerMessage(String cpId, String requestedMessage, long timeoutSeconds) throws Exception {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("requestedMessage", requestedMessage); // e.g. "GetLocalListVersion" or "Heartbeat"
        // optional: payload.put("connectorId", 1);
        return sendActionAndWait(cpId, "TriggerMessage", payload, timeoutSeconds);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
    private static Integer toInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Integer) {
            return (Integer) o;
        }
        if (o instanceof Number) {
            return ((Number)o).intValue();
        }
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e){ return null; }
    }
}
