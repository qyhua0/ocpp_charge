package top.modelx.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.modelx.service.OcppService;

import java.util.*;

/**
 * 增强版OCPP1.6 控制器（集成WebSocket功能）
 *
 * @author: hua
 * @date: 2025/8/14
 */
@RestController
@RequestMapping("/api/ocpp")
public class EnhancedOcppController {
    private static final Logger log = LogManager.getLogger(EnhancedOcppController.class);

    @Autowired
    private OcppService ocppService;


    /**
     * 显示当前的桩连接资料
     */
    @GetMapping("/connections")
    public ResponseEntity connections() {
        try {
            log.info("Received request to list OCPP connections");

            List<Map<String, Object>> connections = ocppService.listConnections();

            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            log.error("Failed to retrieve OCPP connections", e);
            return ResponseEntity.badRequest().body("Failed to retrieve OCPP connections");
        }
    }

    /**
     * 获取充电桩当前状态资料
     */
    @GetMapping("/status/{cpId}")
    public ResponseEntity status(@PathVariable String cpId) {
        if (cpId == null || cpId.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid cpId: must not be null or empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            Map<String, Object> result = ocppService.getStatus(cpId);
            // 更新充电状态到WebSocket
            updateChargingStatusFromResult(result);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error occurred while fetching status for cpId: {}", cpId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 下发启动充电
     */
    @GetMapping("/remoteStart/{cpId}")
    public ResponseEntity remoteStart(@PathVariable String cpId, @RequestParam String idTag,
                                      @RequestParam(required = false) Integer connectorId) {
        try {

            String uid = ocppService.remoteStart(cpId, idTag, connectorId);

            Map<String, Object> response = new HashMap<>();
            response.put("requestId", uid);
            response.put("cpId", cpId);

            log.info("Remote start successful for cpId={}, requestId={}", cpId, uid);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Remote start failed for cpId={}, idTag={}", cpId, idTag, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "远程启动失败");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 下发结束充电
     */
    @GetMapping("/remoteStop/{cpId}")
    public ResponseEntity remoteStop(@PathVariable String cpId, @RequestParam int transactionId) {
        if (cpId == null || cpId.isEmpty() || cpId.length() > 50) {
            throw new IllegalArgumentException("Invalid cpId");
        }

        try {

            String uid = ocppService.remoteStop(cpId, transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put("requestId", uid);
            response.put("cpId", cpId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Remote stop failed for cpId: {}, transactionId: {}", cpId, transactionId, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Operation failed"));
        }
    }

    /**
     * 查询桩端 LocalList 版本
     */
    @GetMapping("/localList/version/{cpId}")
    public ResponseEntity<Map<String, Object>> getLocalListVersion(
            @PathVariable String cpId,
            @RequestParam(defaultValue = "10") long timeoutSeconds) {

        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId: must not be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Invalid timeoutSeconds: must be positive");
        }

        try {

            Map<String, Object> res = ocppService.getLocalListVersion(cpId, timeoutSeconds);

            Map<String, Object> response = new HashMap<>();
            response.put("cpId", cpId);
            response.put("result", res);


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get local list version for cpId: {}", cpId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cpId", cpId);
            errorResponse.put("error", "Failed to retrieve local list version");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    /**
     * 全量下发白名单（Full 覆盖）
     */
    @RequestMapping("/localList/full/{cpId}")
    public ResponseEntity<Map<String, Object>> sendLocalListFull(@PathVariable String cpId,
                                                                 @RequestParam(defaultValue = "10") long timeoutSeconds) {
        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId");
        }

        SendLocalListRequest body = new SendLocalListRequest();
        body.setListVersion(6);

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> authItem = new HashMap<>();
        authItem.put("idTag", "a123456789b123456789");

        Map<String, Object> idTagInfo = new HashMap<>();
        idTagInfo.put("status", "Accepted");
        authItem.put("idTagInfo", idTagInfo);
        list.add(authItem);
        body.setLocalAuthorisationList(list);


        try {

            ocppService.tryGetLocalListVersionThenSend(cpId, body.getListVersion(), body.getLocalAuthorisationList());

            Map<String, Object> response = new HashMap<>();
            response.put("cpId", cpId);
            response.put("status", "success");
            response.put("message", "全量白名单下发成功");


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send full local list for cpId: {}", cpId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cpId", cpId);
            errorResponse.put("error", "Failed to send local list");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 增量下发白名单（Differential）
     */
    @PostMapping("/localList/diff/{cpId}")
    public ResponseEntity<Map<String, Object>> sendLocalListDiff(@PathVariable String cpId,
                                                                 @RequestBody SendLocalListRequest body,
                                                                 @RequestParam(defaultValue = "10") long timeoutSeconds) {
        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId: must not be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Invalid timeoutSeconds: must be positive");
        }

        List<Map<String, Object>> localAuthorisationList = body.getLocalAuthorisationList();
        if (localAuthorisationList == null) {
            localAuthorisationList = Collections.emptyList();
        }

        try {

            Map<String, Object> res = ocppService.sendLocalList(cpId, body.getListVersion(),
                    "Differential", localAuthorisationList, timeoutSeconds, true);

            Map<String, Object> response = new HashMap<>();
            response.put("cpId", cpId);
            response.put("result", res);


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send differential local list for cpId: {}", cpId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cpId", cpId);
            errorResponse.put("error", "Failed to send differential local list");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 从查询结果更新充电状态
     */
    private void updateChargingStatusFromResult(Map<String, Object> result) {
        try {
            // 根据实际返回的数据结构来解析
            Double voltage = getDoubleFromResult(result, "voltage", 0.0);
            Double current = getDoubleFromResult(result, "current", 0.0);
            Double power = voltage * current;
            Double energy = getDoubleFromResult(result, "energy", 0.0);
            Integer batteryLevel = getIntegerFromResult(result, "batteryLevel", 0);
            Integer temperature = getIntegerFromResult(result, "temperature", 25);
            String chargingState = getStringFromResult(result, "status", "空闲");
            String connectorStatus = getStringFromResult(result, "connectorStatus", "未知");

        } catch (Exception e) {
            log.warn("Failed to update charging status from result", e);
        }
    }

    private Double getDoubleFromResult(Map<String, Object> result, String key, Double defaultValue) {
        Object value = result.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private Integer getIntegerFromResult(Map<String, Object> result, String key, Integer defaultValue) {
        Object value = result.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private String getStringFromResult(Map<String, Object> result, String key, String defaultValue) {
        Object value = result.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * SendLocalListRequest 数据传输对象
     */
    public static class SendLocalListRequest {
        private Integer listVersion;
        private List<Map<String, Object>> localAuthorisationList;

        public Integer getListVersion() {
            return listVersion;
        }

        public void setListVersion(Integer listVersion) {
            this.listVersion = listVersion;
        }

        public List<Map<String, Object>> getLocalAuthorisationList() {
            return localAuthorisationList;
        }

        public void setLocalAuthorisationList(List<Map<String, Object>> localAuthorisationList) {
            this.localAuthorisationList = localAuthorisationList;
        }
    }
}