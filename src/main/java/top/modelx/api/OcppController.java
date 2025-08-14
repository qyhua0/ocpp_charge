package top.modelx.api;

import top.modelx.model.SendLocalListRequest;
import top.modelx.service.OcppService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * OCPP1.6 控制器
 *
 * @author: hua
 * @date: 2025/8/11
 */
@RestController
@RequestMapping("/api/ocpp")
public class OcppController {
    private static final Logger log = LogManager.getLogger(OcppController.class);

    @Autowired
    OcppService ocppService;

    public OcppController(OcppService ocppService) {
        this.ocppService = ocppService;
    }

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
        // 输入校验
        if (cpId == null || cpId.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid cpId: must not be null or empty");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            Map<String, Object> result = ocppService.getStatus(cpId);
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
    public ResponseEntity remoteStart(@PathVariable String cpId,@RequestParam String idTag, @RequestParam(required = false) Integer connectorId
    ) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }


    /**
     * 下发结束充电
     */
    @GetMapping("/remoteStop/{cpId}")
    public ResponseEntity remoteStop(
            @PathVariable String cpId,
            @RequestParam int transactionId
    ) {
        // 参数校验
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
            // 记录异常日志（可根据实际日志框架替换）
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

        // 参数校验
        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId: must not be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Invalid timeoutSeconds: must be positive");
        }


        try {

            //ocppService.triggerMessage(cpId, "GetLocalListVersion",10);

            Map<String, Object> res = ocppService.getLocalListVersion(cpId, timeoutSeconds);
            Map<String, Object> response = new HashMap<>();
            response.put("cpId", cpId);
            response.put("result", res);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 异常处理：避免将底层异常直接暴露给调用方
            log.error("Failed to get local list version for cpId: {}", cpId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cpId", cpId);
            errorResponse.put("error", "Failed to retrieve local list version");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private static final String FULL_UPDATE_TYPE = "Full";
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    /**
     * 全量下发白名单（Full 覆盖）
     */
    @RequestMapping("/localList/full/{cpId}")
    public Map<String, Object> sendLocalListFull(@PathVariable String cpId,
                                                 // @RequestBody SendLocalListRequest body,
                                                 @RequestParam(defaultValue = "10") long timeoutSeconds) {
        // 参数校验
        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId");
        }

        SendLocalListRequest body = new SendLocalListRequest();
        body.setListVersion(6);

        List list = new ArrayList();
        HashMap hashMap = new HashMap<String, Object>();
        hashMap.put("idTag", "a123456789b123456789");
        //"idTagInfo": { "status":"Accepted", "expiryDate": "2026-01-01T00:00:00Z" }

        HashMap idTagInfo = new HashMap<String, Object>();
        idTagInfo.put("status", "Accepted");
        // idTagInfo.put("expiryDate", "2026-01-01T00:00:00Z");

        hashMap.put("idTagInfo", idTagInfo);
        list.add(hashMap);
        body.setLocalAuthorisationList(list);


        if (body == null) {
            throw new IllegalArgumentException("Invalid request body or list version");
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = DEFAULT_TIMEOUT_SECONDS; // fallback to default
        }

        try {

            ocppService.tryGetLocalListVersionThenSend(cpId, body.getListVersion(), body.getLocalAuthorisationList());
//            Map<String, Object> res = ocppService.sendLocalList(
//                    cpId,
//                    body.getListVersion(),
//                    FULL_UPDATE_TYPE,
//                    body.getLocalAuthorisationList(),
//                    timeoutSeconds,
//                    false
//            );
//            Map<String, Object> response = new HashMap<>();
//            response.put("cpId", cpId);
            //response.put("result", res);
            return null;
        } catch (Exception e) {
            // 可根据实际业务需求封装为特定异常或记录日志
            throw new RuntimeException("Failed to send local list for cpId: " + cpId, e);
        }
    }


    /**
     * 增量下发白名单（Differential）
     */
    @PostMapping("/localList/diff/{cpId}")
    public Map<String, Object> sendLocalListDiff(@PathVariable String cpId,
                                                 @RequestBody SendLocalListRequest body,
                                                 @RequestParam(defaultValue = "10") long timeoutSeconds) throws Exception {
        // 参数校验
        if (cpId == null || cpId.isEmpty()) {
            throw new IllegalArgumentException("Invalid cpId: must not be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Invalid timeoutSeconds: must be positive");
        }

        // 判空处理
        List<Map<String, Object>> localAuthorisationList = body.getLocalAuthorisationList();
        if (localAuthorisationList == null) {
            localAuthorisationList = Collections.emptyList();
        }

        // 调用服务
        Map<String, Object> res = ocppService.sendLocalList(cpId, body.getListVersion()
                , "Differential", localAuthorisationList, timeoutSeconds, true);

        // 返回结构保持一致
        Map<String, Object> response = new HashMap<>();
        response.put("cpId", cpId);
        response.put("result", res);
        return response;
    }


}