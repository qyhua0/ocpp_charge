package top.modelx.service;

import top.modelx.OcppApplication;
import top.modelx.service.tcpServer.TcpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author hua
 */
@Service
public class TranServiceImpl {

    private static final Logger logger = LogManager.getLogger(OcppApplication.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;




    /**
     * 发送消息 采用系统配置类型
     *
     * @param queueName 是发送到的队列名称
     * @param message   是发送到的队列
     */
    public void sendMessage(String queueName, final String message) {
        rabbitTemplate.convertAndSend("", "hex_rev_AION", message);

    }

    /**
     * 发送消息 采用指定队列类型
     *
     * @param queueName 是发送到的队列
     * @param message   是发送到的队列
     */
    public void sendMessageByQueue(String queueName, final String message) {
        rabbitTemplate.convertAndSend("", "hex_rev_AION", message);
    }

    /**
     * 接收消息
     *
     * @param text
     */
    @RabbitListener(queues = "hex_send_AION")
    public void receiveQueue(String text) {
        if(!TcpHandler.RepDev(text)){
            logger.error("write mq fail ==> "+text);
        }
    }
}
