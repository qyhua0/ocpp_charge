package top.modelx.service.tcpServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 包处理实现类
 * @author: hua
 */
public  class PacketHandlerImpl implements PacketHandler {

    public static List<PacketHandler> packetHandlers = new ArrayList<PacketHandler>();
    static PacketHandlerImpl packageHandler;
    protected PacketHandlerImpl(){
        super();
        System.out.println("init PackageHandlerImpl");
    }

    public static PacketHandlerImpl getInstance(){
        if(packageHandler==null){
            packageHandler=new PacketHandlerImpl();
        }
        return packageHandler;
    }

    @Override
    public void doHandle(String hex) {
        for(PacketHandler f : packetHandlers){
            f.doHandle(hex);
        }

    }
    public PacketHandlerImpl addHandle(PacketHandler f){
        packetHandlers.add(f);
        return this;
    }


}
