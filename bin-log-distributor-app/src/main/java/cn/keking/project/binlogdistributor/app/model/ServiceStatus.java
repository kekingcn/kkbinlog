package cn.keking.project.binlogdistributor.app.model;


import java.util.Set;

/**
 * @author wanglaomo
 * @since 2019/10/18
 **/
public class ServiceStatus {

    private String ip;

    private Set<String> activeNamespaces;

    private long totalEventCount;

    private long latelyEventCount;

    private long totalPublishCount;

    private long latelyPublishCount;

    private String updateTime;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Set<String> getActiveNamespaces() {
        return activeNamespaces;
    }

    public void setActiveNamespaces(Set<String> activeNamespaces) {
        this.activeNamespaces = activeNamespaces;
    }

    public long getTotalEventCount() {
        return totalEventCount;
    }

    public void setTotalEventCount(long totalEventCount) {
        this.totalEventCount = totalEventCount;
    }

    public long getTotalPublishCount() {
        return totalPublishCount;
    }

    public void setTotalPublishCount(long totalPublishCount) {
        this.totalPublishCount = totalPublishCount;
    }

    public long getLatelyPublishCount() {
        return latelyPublishCount;
    }

    public void setLatelyPublishCount(long latelyPublishCount) {
        this.latelyPublishCount = latelyPublishCount;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public long getLatelyEventCount() {
        return latelyEventCount;
    }

    public void setLatelyEventCount(long latelyEventCount) {
        this.latelyEventCount = latelyEventCount;
    }

    @Override
    public String toString() {
        return "ServiceStatus{" +
                "ip='" + ip + '\'' +
                ", activeNamespaces=" + activeNamespaces +
                ", totalEventCount=" + totalEventCount +
                ", latelyEventCount=" + latelyEventCount +
                ", totalPublishCount=" + totalPublishCount +
                ", latelyPublishCount=" + latelyPublishCount +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
