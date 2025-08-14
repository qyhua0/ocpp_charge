package top.modelx.service.tcpServer;

/**
 * 广汽充电协议
 * @author: hua
 */
public class AION_ChargeProtocol {

    public final static int head_s = 0xeb;
    public final static int head_e = 0x16;
    private int length;
    private byte[] raw;
    private String rawStr;

    public AION_ChargeProtocol(int length, byte[] raw){
        this.length=length;
        this.raw=raw;
    }
    public AION_ChargeProtocol(String raw){
        this.rawStr=raw;
    }

    public int getHead_s() {
        return head_s;
    }

    public int getHead_e() {
        return head_e;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    public String getRawStr() {
        return rawStr;
    }

    public void setRawStr(String rawStr) {
        this.rawStr = rawStr;
    }
}
