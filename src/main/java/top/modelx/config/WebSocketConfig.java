package top.modelx.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 此配置用于与前端页面进行通信
 * WebSocket配置类
 *
 * @author: hua
 * @date: 2025/8/14
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 注册协议日志WebSocket处理器
        registry.addHandler(protocolLogHandler(), "/ws/protocol-logs")
                .setAllowedOrigins("*"); // 允许跨域
    }



    @Bean
    public WebSocketHandler protocolLogHandler() {
        return new ProtocolLogWebSocketHandler();
    }
}