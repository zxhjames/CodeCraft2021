package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-12 11:30
 **/
public class DeployInfo {
    String vmid; // 虚拟机id
    ServerType serverType;
    String node;

    public DeployInfo(String vmid, ServerType serverType, String node) {
        this.vmid = vmid;
        this.serverType = serverType;
        this.node = node;
    }
}
