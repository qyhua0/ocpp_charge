package top.modelx.service.tcpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hua
 */
@Service
public class TcpHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(TcpHandler.class);

    static Map<String, ChannelHandlerContext> inList = new ConcurrentHashMap<String, ChannelHandlerContext>();


    /**
     * 新连接
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String channelName = getChannelName(ctx);
        // 将新连接添加到连接列表中
        synchronized (inList) {
            inList.put(channelName, ctx);
        }
        // 记录新连接建立的日志
        logger.info("dev new conn > {}", channelName);
    }


    private String getChannelName(ChannelHandlerContext ctx) {
        return "AION".concat(ctx.channel().remoteAddress().toString());

    }

    /**
     * 连接下线
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            String channelName = getChannelName(ctx);
            if (channelName != null) {
                logger.info("dev close conn > {}", channelName);
                synchronized (inList) {
                    inList.remove(channelName);
                }
            } else {
                logger.warn("Channel name is null when channel inactive");
            }
        } catch (Exception e) {
            logger.error("Error occurred when handling channel inactive", e);
        }
        ctx.fireChannelInactive();
    }


    /**
     * 接收信息
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            logger.warn("Received null message in channelRead");
            return;
        }

        if (!(msg instanceof AION_ChargeProtocol)) {
            logger.warn("Received message is not instance of AIONChargeProtocol, actual type: " + msg.getClass().getName());
            return;
        }

        AION_ChargeProtocol in = (AION_ChargeProtocol) msg;

        String readMsg = in.getRawStr();
        logger.info("form dev <- " + readMsg);
        String channelName = getChannelName(ctx);
        readMsg = channelName + "$$" + readMsg;
        PacketHandlerImpl.getInstance().doHandle(readMsg);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 回复信息给设备
     *
     * @param hex
     */
    public static boolean RepDev(String hex) {
        if (hex == null) {
            logger.error("input hex is null");
            return false;
        }

        String[] kv = hex.split("\\$\\$");
        if (kv.length == 2) {
            String key = kv[0];
            ChannelHandlerContext context = inList.get(key);
            if (context != null) {
                try {
                    byte[] bytes = hexString2Bytes(kv[1]);
                    ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
                    context.writeAndFlush(byteBuf);
                    return true;
                } catch (Exception e) {
                    logger.error("convert hex string to bytes failed, hexString: " + kv[1], e);
                }
            } else {
                logger.error("dev offline=" + key);
            }
        } else {
            logger.error("cmd format err");
        }
        return false;
    }

   public static byte[] hexString2Bytes(String src) {
    if (src == null) {
        return null;
    }

    if (src.length() % 2 != 0) {
        throw new IllegalArgumentException("输入的十六进制字符串长度必须为偶数");
    }

    int length = src.length() / 2;
    byte[] ret = new byte[length];

    for (int i = 0; i < length; ++i) {
        try {
            ret[i] = (byte) Integer.parseInt(src.substring(i * 2, i * 2 + 2), 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("输入包含非法的十六进制字符: " + src.substring(i * 2, i * 2 + 2), e);
        }
    }

    return ret;
}



}
