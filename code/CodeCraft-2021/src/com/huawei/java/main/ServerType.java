package com.huawei.java.main;
import java.util.List;

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

    //todo 修改
    List<VirtualType> Anodes; // A结点集群
    List<VirtualType> Bnodes; // B结点集群
//    VirtualType Anode; // A结点虚拟机
//    VirtualType Bnode; // B结点虚拟机

    public ServerType(String type, Integer acpuNum, Integer amemory, Integer bcpuNum, Integer bmemory, Integer machineConsume, Integer dayConsume) {
        this.type = type;
        AcpuNum = acpuNum;
        Amemory = amemory;
        BcpuNum = bcpuNum;
        Bmemory = bmemory;
        this.machineConsume = machineConsume;
        this.dayConsume = dayConsume;
    }

    public ServerType(String type, Integer acpuNum, Integer amemory, Integer bcpuNum, Integer bmemory, List<VirtualType> anodes, List<VirtualType> bnodes) {
        this.type = type;
        AcpuNum = acpuNum;
        Amemory = amemory;
        BcpuNum = bcpuNum;
        Bmemory = bmemory;
        Anodes = anodes;
        Bnodes = bnodes;
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
                ", Anodes=" + Anodes +
                ", Bnodes=" + Bnodes +
                '}';
    }


    @Override
    public int compareTo(ServerType o) {
        return this.machineConsume - o.machineConsume;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getAcpuNum() {
        return AcpuNum;
    }

    public void setAcpuNum(Integer acpuNum) {
        AcpuNum = acpuNum;
    }

    public Integer getAmemory() {
        return Amemory;
    }

    public void setAmemory(Integer amemory) {
        Amemory = amemory;
    }

    public Integer getBcpuNum() {
        return BcpuNum;
    }

    public void setBcpuNum(Integer bcpuNum) {
        BcpuNum = bcpuNum;
    }

    public Integer getBmemory() {
        return Bmemory;
    }

    public void setBmemory(Integer bmemory) {
        Bmemory = bmemory;
    }

    public Integer getMachineConsume() {
        return machineConsume;
    }

    public void setMachineConsume(Integer machineConsume) {
        this.machineConsume = machineConsume;
    }

    public Integer getDayConsume() {
        return dayConsume;
    }

    public void setDayConsume(Integer dayConsume) {
        this.dayConsume = dayConsume;
    }

    public List<VirtualType> getAnodes() {
        return Anodes;
    }

    public void setAnodes(List<VirtualType> anodes) {
        Anodes = anodes;
    }

    public List<VirtualType> getBnodes() {
        return Bnodes;
    }

    public void setBnodes(List<VirtualType> bnodes) {
        Bnodes = bnodes;
    }
}
