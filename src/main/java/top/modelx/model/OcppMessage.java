package top.modelx.model;

/**
 * OCPP 1.6使用特定的JSON消息格式
 * @author: hua
 * @date: 2025/8/8
 */
public class OcppMessage {
    private String messageId;
    private String action;
    private Object payload;

    // OCPP 1.6消息类型
    public static final String CALL = "2";
    public static final String CALL_RESULT = "3";
    public static final String CALL_ERROR = "4";

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}