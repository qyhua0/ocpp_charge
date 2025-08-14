package top.modelx.service.tcpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.modelx.OcppApplication;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 广汽充电协议
 *
 * @author: hua
 */
public class AION_DecoderV1 extends ByteToMessageDecoder {
    private static final Logger logger = LogManager.getLogger(OcppApplication.class);


    final static String rex0000 = "^eb09.{14,2024}";
    final static Pattern pattern0000 = Pattern.compile(rex0000);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf bufferIn, List<Object> list) throws Exception {

        int readableBytes = bufferIn.readableBytes();
        if (readableBytes < 8) {
            logger.warn("err! cmd len < 8.");
            String hexDump = ByteBufUtil.hexDump(bufferIn);
            logger.warn(hexDump);
            bufferIn.skipBytes(readableBytes);
            return;
        }

        String hexDump = ByteBufUtil.hexDump(bufferIn);
        Matcher matcher00 = pattern0000.matcher(hexDump);
        if (!matcher00.find()) {
            logErr(channelHandlerContext, hexDump);
            logger.warn("err! cmd format invalid: " + hexDump);
            bufferIn.skipBytes(readableBytes);
            return;
        }

        String cmd = matcher00.group();
        logger.info("sign cmd: " + cmd);

        if (bufferIn.readableBytes() < 4) {
            logger.error("Buffer does not have enough bytes to read length field.");
            bufferIn.skipBytes(readableBytes);
            return;
        }

        int length = (int) ((bufferIn.getByte(3) & 0xFF) << 8 | (bufferIn.getByte(2) & 0xFF));
        logger.info("len = " + length);

        int expectedCmdLength = (length + 7) * 2;
        int actualCmdLength = cmd.length();

        if (actualCmdLength == expectedCmdLength) {
            try {
                AION_ChargeProtocol p = new AION_ChargeProtocol(cmd);
                list.add(p);
            } catch (Exception e) {
                logger.error("Failed to create AIONChargeProtocol from cmd: " + cmd, e);
            }
            bufferIn.skipBytes(readableBytes);
        } else if (actualCmdLength > expectedCmdLength) {
            try {
                multiHand(cmd, list);
            } catch (Exception e) {
                logger.error("Error occurred in multiHand with cmd: " + cmd, e);
            }
            bufferIn.skipBytes(readableBytes);
        } else {
            logger.warn("Unexpected cmd length. cmd: {}, expected: {}, actual: {}", cmd, expectedCmdLength, actualCmdLength);
            bufferIn.skipBytes(readableBytes);
        }


    }

    private void multiHand(String cmd, List<Object> list) {
        if (cmd == null || cmd.length() < 8) {
            return;
        }

        String lenStr1 = cmd.substring(4, 6);
        String lenStr2 = cmd.substring(6, 8);

        int length;
        try {
            length = Integer.parseInt(lenStr2 + lenStr1, 16);
        } catch (NumberFormatException e) {
            System.err.println("Invalid hex string for length: " + lenStr2 + lenStr1);
            return;
        }

        int payloadLength = (length + 7) * 2;
        int totalLength = (payloadLength + 7) * 2;

        if (totalLength > cmd.length()) {
            return;
        }

        String newCmd = cmd.substring(0, totalLength);
        System.out.println("multi cmd-> " + newCmd);

        AION_ChargeProtocol p = new AION_ChargeProtocol(newCmd);
        list.add(p);

        if (cmd.length() > totalLength) {
            String remainingCmd = cmd.substring(totalLength);
            System.out.println("multi xxx-> " + remainingCmd);
            if (remainingCmd.startsWith("eb09")) {
                multiHand(remainingCmd, list);
            }
        }
    }


    private void logErr(ChannelHandlerContext ctx, String msg) {
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIP = insocket.getAddress().getHostAddress();
        System.out.println(clientIP + " :: " + msg);
    }

}
