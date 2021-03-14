package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-14 10:54
 **/
public class ServerRespurce {
    Integer minCpu;
    Integer minMemory;
    ServerType serverType;
    String loc;

    public ServerRespurce(Integer minCpu, Integer minMemory, ServerType serverType, String loc) {
        this.minCpu = minCpu;
        this.minMemory = minMemory;
        this.serverType = serverType;
        this.loc = loc;
    }

    public Integer getMinCpu() {
        return minCpu;
    }

    public void setMinCpu(Integer minCpu) {
        this.minCpu = minCpu;
    }

    public Integer getMinMemory() {
        return minMemory;
    }

    public void setMinMemory(Integer minMemory) {
        this.minMemory = minMemory;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public void setServerType(ServerType serverType) {
        this.serverType = serverType;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }
}
