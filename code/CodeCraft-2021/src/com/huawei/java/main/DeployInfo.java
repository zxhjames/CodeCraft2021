package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-12 11:30
 **/
public class DeployInfo {
    ServerType serverType;
    String node;

    public DeployInfo(ServerType serverType, String node) {
        this.serverType = serverType;
        this.node = node;
    }
}
