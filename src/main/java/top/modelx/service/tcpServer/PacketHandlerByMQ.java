package top.modelx.service.tcpServer;

import top.modelx.OcppApplication;
import top.modelx.service.TranServiceImpl;


/**
 * 通过MQ处理报文
 * @author hua
 */
public class PacketHandlerByMQ extends PacketHandlerImpl {
    private final String in_name="AION_in";


    @Override
    public void doHandle(String hexStr) {

            TranServiceImpl tranService= OcppApplication.applicationContext.getBean(TranServiceImpl.class);
            System.out.println("dev to mq  >>> "+hexStr);
            tranService.sendMessage(in_name,hexStr);

    }
}
