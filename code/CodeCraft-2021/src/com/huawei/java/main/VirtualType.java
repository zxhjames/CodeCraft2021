package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 15:33
 **/
// 虚拟机类型
public class VirtualType implements Comparable<VirtualType>{
    String type;
    Integer cpuNum;
    Integer memory;
    Integer isDeployWithTowNode; // 是否双节点部署 0单 1双
    String id;

    public VirtualType(String type, Integer cpuNum, Integer memory, Integer isDeployWithTowNode) {
        this.type = type;
        this.cpuNum = cpuNum;
        this.memory = memory;
        this.isDeployWithTowNode = isDeployWithTowNode;
    }

    @Override
    public String toString() {
        return "VirtualType{" +
                "type='" + type + '\'' +
                ", cpuNum=" + cpuNum +
                ", memory=" + memory +
                ", isDeployWithTowNode=" + isDeployWithTowNode +
                '}';
    }

    public String getType() {
        return type;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public Integer getMemory() {
        return memory;
    }

    public Integer getIsDeployWithTowNode() {
        return isDeployWithTowNode;
    }

    @Override
    public int compareTo(VirtualType o) {
        return this.cpuNum - o.cpuNum;
    }
}

