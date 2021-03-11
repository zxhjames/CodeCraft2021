package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 15:33
 **/


// 服务器类型
public class ServerType implements Comparable<ServerType> {
    String type; //型号
    Integer cpuNum;  //cpu核数
    Integer memory; // 内存大小
    Integer machineConsume; // 硬件消耗
    Integer dayConsume; // 每日消耗成本
    VirtualType Anode; // A结点虚拟机
    VirtualType Bnode; // B结点虚拟机

    public ServerType(String type, Integer cpuNum, Integer memory, Integer machineConsume, Integer dayConsume) {
        this.type = type;
        this.cpuNum = cpuNum;
        this.memory = memory;
        this.machineConsume = machineConsume;
        this.dayConsume = dayConsume;
    }

    public ServerType(String type, Integer cpuNum, Integer memory, Integer machineConsume, Integer dayConsume, VirtualType anode, VirtualType bnode) {
        this.type = type;
        this.cpuNum = cpuNum;
        this.memory = memory;
        this.machineConsume = machineConsume;
        this.dayConsume = dayConsume;
        Anode = anode;
        Bnode = bnode;
    }

    @Override
    public String toString() {
        return "ServerType{" +
                "type='" + type + '\'' +
                ", cpuNum=" + cpuNum +
                ", memory=" + memory +
                ", machineConsume=" + machineConsume +
                ", dayConsume=" + dayConsume +
                '}';
    }


    @Override
    public int compareTo(ServerType o) {
        return this.machineConsume - o.machineConsume;
    }

    public VirtualType getAnode() {
        return Anode;
    }

    public VirtualType getBnode() {
        return Bnode;
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

    public Integer getMachineConsume() {
        return machineConsume;
    }

    public Integer getDayConsume() {
        return dayConsume;
    }
}
