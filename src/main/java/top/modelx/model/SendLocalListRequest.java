package top.modelx.model;

import java.util.List;
import java.util.Map;

/**
 * 发送本地授权列表请求
 * @author: hua
 * @date: 2025/8/8
 */
public class SendLocalListRequest {
    private int listVersion;
    private List<Map<String, Object>> localAuthorisationList;

    public int getListVersion() {
        return listVersion;
    }

    public void setListVersion(int listVersion) {
        this.listVersion = listVersion;
    }

    public List<Map<String, Object>> getLocalAuthorisationList() {
        return localAuthorisationList;
    }

    public void setLocalAuthorisationList(List<Map<String, Object>> localAuthorisationList) {
        this.localAuthorisationList = localAuthorisationList;
    }
}