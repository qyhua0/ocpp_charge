package top.modelx.util;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OCPP 消息工具类
 * @author: hua
 * @date: 2025/8/8
 */
public class OcppMessageUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String newUid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 构造 CALL 帧
     */
    public static String buildCall(String uniqueId, String action, Map<String, Object> payload) throws Exception {
        List<Object> arr = new ArrayList<>();
        arr.add(2);
        arr.add(uniqueId);
        arr.add(action);
        arr.add(payload == null ? Collections.emptyMap() : payload);
        return MAPPER.writeValueAsString(arr);
    }

    /**
     * 构造 CALLRESULT 帧
     */
    public static String buildCallResult(String uniqueId, Map<String, Object> payload) throws Exception {
        List<Object> arr = new ArrayList<>();
        arr.add(3);
        arr.add(uniqueId);
        arr.add(payload == null ? Collections.emptyMap() : payload);
        String json =MAPPER.writeValueAsString(arr);
        System.out.println(" -> "+ json);
        return json;
    }

    /**
     * 构造 CALLERROR 帧
     */
    public static String buildCallError(String uniqueId, String errorCode, String errorDescription, Map<String, Object> details) throws Exception {
        List<Object> arr = new ArrayList<>();
        arr.add(4);
        arr.add(uniqueId);
        arr.add(errorCode);
        arr.add(errorDescription);
        arr.add(details == null ? Collections.emptyMap() : details);
        return MAPPER.writeValueAsString(arr);
    }

    /**
     * 解析入站 JSON 数组帧
     */
    public static List<Object> parseArray(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<List<Object>>() {
        });
    }

    /**
     * 简单生成交易号（示例）
     */
    public static int genTransactionId() {
        return ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}