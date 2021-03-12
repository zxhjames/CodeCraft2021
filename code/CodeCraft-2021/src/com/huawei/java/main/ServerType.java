package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 15:33
 **/


// 服务器类型
public class ServerType implements Comparable<ServerType> {
    Integer id; // 服务器的唯一id
    String type; //型号
    Integer AcpuNum;
    Integer Amemory;
    Integer BcpuNum;
    Integer Bmemory;
    Integer machineConsume; // 硬件消耗
    Integer dayConsume; // 每日消耗成本
    VirtualType Anode; // A结点虚拟机
    VirtualType Bnode; // B结点虚拟机

    public ServerType(String type, Integer acpuNum, Integer amemory, Integer bcpuNum, Integer bmemory, Integer machineConsume, Integer dayConsume) {
        this.type = type;
        AcpuNum = acpuNum;
        Amemory = amemory;
        BcpuNum = bcpuNum;
        Bmemory = bmemory;
        this.machineConsume = machineConsume;
        this.dayConsume = dayConsume;
    }

    public ServerType(String type, Integer acpuNum, Integer amemory, Integer bcpuNum, Integer bmemory, VirtualType anode, VirtualType bnode) {
        this.type = type;
        AcpuNum = acpuNum;
        Amemory = amemory;
        BcpuNum = bcpuNum;
        Bmemory = bmemory;
        Anode = anode;
        Bnode = bnode;
    }

    @Override
    public String toString() {
        return "ServerType{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", AcpuNum=" + AcpuNum +
                ", Amemory=" + Amemory +
                ", BcpuNum=" + BcpuNum +
                ", Bmemory=" + Bmemory +
                ", machineConsume=" + machineConsume +
                ", dayConsume=" + dayConsume +
                ", Anode=" + Anode +
                ", Bnode=" + Bnode +
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
        return this.AcpuNum + this.BcpuNum;
    }


    public Integer getMachineConsume() {
        return machineConsume;
    }

    public Integer getDayConsume() {
        return dayConsume;
    }
}
