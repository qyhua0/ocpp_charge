package top.modelx.model;

import java.time.LocalDateTime;

/**
 *  设备消息事件
 * @author: hua
 */
public class DeviceMessageEvent {
    public enum Direction {
        SEND, RECEIVE, SYSTEM
    }
    public enum Level {
        INFO, ERROR, DEBUG
    }
    private Direction direction;
    private Level level;
    private String message;
    private String cpId;
    private String messageType;
    private String messageId;
    private String rawData;
    private LocalDateTime timestamp;

    public DeviceMessageEvent(String cpId, String message,Direction  direction) {
        this.cpId = cpId;
        this.message = message;
        this.direction = direction;
        this.timestamp = LocalDateTime.now();
    }



    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getCpId() {
        return cpId;
    }

    public void setCpId(String cpId) {
        this.cpId = cpId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}



