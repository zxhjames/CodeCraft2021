package com.huawei.java.main;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.huawei.java.main.ToolsBuilder.*;

public class Main {

    public static void main(String[] args) throws IOException {
        // 构建输入输出
        Scanner in = new Scanner(System.in);
        //InputStreamReader isr = new InputStreamReader(System.in);
        //BufferedReader in = new BufferedReader(isr);


        int N,M,T,R;
        List<ServerType> serverTypeList = new ArrayList<>();  // 用于采购的服务器类型数量
        List<VirtualType> virtualTypeList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder("");

        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS组
        Map<String,ServerType> ECSMAP = new HashMap<>(); // 用于存储虚拟机id与ECS的映射

        N = Integer.parseInt(in.nextLine());
        //System.out.println("输入了" + N);
        for (int i = 0; i < N; ++i) {
            String str = in.nextLine();
            //System.out.println("输入了" + str);
            serverTypeList.add(SplitServerTypeline(str));
        }
//        List<ServerType> serverTypes = serverTypeList;
        List<ServerType> serverTypes = serverTypeList.stream().
                sorted(Comparator.comparing(ServerType::getCpuNum)).collect(Collectors.toList());

        // 读取M行的每一种类型的虚拟机
        M = Integer.parseInt(in.nextLine());
        //System.out.println("输入了" + M);
        for (int i=0;i<M;++i) {
            String str = in.nextLine();
            //System.out.println("输入了" + str);
            virtualTypeList.add(SplitVirtualTypeline(str));
        }

        // 列表转为map (key->Type Value->VirtualType）各种种类的虚拟机
        Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));
        // 请求数据
        T = Integer.parseInt(in.nextLine());
        //System.out.println("输入了" + T);
        for (int i = 0; i< T;++i) {
            R = Integer.parseInt(in.nextLine());
            //System.out.println("输入了" + R);
            // 每一天的调度信息
            Scheduler(in,stringBuilder,R,virtualTypeMap,serverTypes,ECS,ECSMAP);
        }

        System.out.println(stringBuilder.toString());
        //System.out.println(ECS.size());
        in.close();
    }





    /**
     * 调度策略
     * @param in 输入的请求
     * @param stringBuilder 结果
     * @param count 请求数量
     * @param virtualTypeMap 虚拟机参数映射
     * @param serverTypes ECS参数
     */
    static void Scheduler(Scanner in,
                          StringBuilder stringBuilder,
                          int count,
                          Map<String,VirtualType> virtualTypeMap,
                          List<ServerType> serverTypes,
                          List<ServerType> ECS,
                          Map<String,ServerType> ECSMAP) throws IOException {

        int buycount = 0; // 记录每天购买的服务器数量
        Map<String,Integer> buyDetail = new HashMap<>(); // 详细购买情况
        Map<Integer,String> deploydetail = new HashMap<>(); // 详细的分配情况
        List<VirtualType> virtualTypes = new ArrayList<>(); // 存储每天购买的虚拟机


        // main loop
        for (int i = 0;i<count; ++i) {
            String line = in.nextLine();
            //System.out.println("输入了" + line);
            String[] result = line.substring(1,line.length() - 1)
                        .replace(" ","").split(",");

                if("add".equals(result[0])) {
                    // 添加虚拟机结点,直接分配
                    VirtualType virtualType = virtualTypeMap.get(result[1]);
                    virtualType.id = result[2];
                    virtualTypes.add(virtualType);
                    boolean isfit = false;
                    // 首先去已有ECS中分配,否则购买
                    for (int k=0;k<ECS.size();++k) {
                        ServerType serverType = ECS.get(k);

                        // 单点部署
                        if (virtualType.isDeployWithTowNode == 0) {
                            // A结点满足就直接分配A结点,否则分配B结点
                            if (serverType.cpuNum >= virtualType.cpuNum && serverType.memory >= virtualType.memory) {
                                if (serverType.Anode == null) {
                                    serverType.Anode = virtualType;serverType.cpuNum -= virtualType.cpuNum;serverType.memory -= virtualType.memory;
                                    //System.out.println(virtualType.type + "分配完成至"+serverType.type+"A结点");
                                    deploydetail.put(serverType.id,"A");
                                    // 移除这个虚拟机结点
                                    isfit = true;
                                    break;
                                }
                                if (serverType.Bnode == null) {
                                    serverType.Bnode = virtualType;serverType.cpuNum -= virtualType.cpuNum;serverType.memory -= virtualType.memory;
                                    // System.out.println(virtualType.type + "分配完成"+serverType.type+"B结点");
                                    deploydetail.put(serverType.id,"B");
                                    isfit = true;
                                    break;
                                }
                            }
                        }

                        // 多点部署
                        if (virtualType.isDeployWithTowNode == 1 && serverType.cpuNum >= virtualType.cpuNum && serverType.memory >= virtualType.memory) {
                            if (serverType.Anode == null && serverType.Bnode == null) {
                                serverType.Anode = serverType.Bnode = virtualType;serverType.cpuNum -= virtualType.cpuNum;serverType.memory -= virtualType.memory;
                                //System.out.println(virtualType.type + "分配完成至" + serverType.type + "双结点");
                                deploydetail.put(serverType.id,"");
                                isfit = true;
                                break;
                            }
                        }

                        // 迁移算法
                    }



                    if(!isfit) {
                        // 需要买服务器
                        for (int k = 0; k < serverTypes.size(); ++k) {
                            //  如果当前cpu和内存满足要求，就将虚拟机分配到这个服务器结点,并减去相应的服务器资源，下一轮迭代时，重新遍历
                            // 优先分配双结点
                            ServerType serverTypenode = serverTypes.get(k);
                            if (serverTypenode.cpuNum >= virtualType.cpuNum && serverTypenode.memory >= virtualType.memory) {
                                ServerType serverType = null;
                                if (virtualType.isDeployWithTowNode == 1) {
                                    serverType = new ServerType(serverTypenode.type,serverTypenode.cpuNum - virtualType.cpuNum,serverTypenode.memory - virtualType.memory,serverTypenode.machineConsume,serverTypenode.dayConsume,virtualType,virtualType);
                                    //System.out.println("购买了 " + serverType.type + "双节点部署 " + tmp.type);
//                                    serverType.id = ECS.size();
                                    deploydetail.put(serverType.id,"");
                                }else if (virtualType.isDeployWithTowNode == 0) {
                                    serverType = new ServerType(serverTypenode.type,serverTypenode.cpuNum - virtualType.cpuNum,serverTypenode.memory - virtualType.memory,serverTypenode.machineConsume,serverTypenode.dayConsume,virtualType,null);
                                    //System.out.println("购买了 " + serverType.type + "A节点部署 " + tmp.type);
//                                    serverType.id = ECS.size();
                                    deploydetail.put(serverType.id,"A");
                                }
                                serverType.id = buycount;
                                if (buyDetail.get(serverType.type) == null) {
                                    buyDetail.put(serverType.type,1);
                                }else{
                                    buyDetail.put(serverType.type,buyDetail.get(serverType.type) + 1);
                                }
                                // 记录已经购买的ECS的ID并且分配它
                                buycount = buycount + 1;
                                // 分配它
                                ECS.add(serverType);
                                ECSMAP.put(virtualType.id,serverType);
                                break;
                            }
                        }
                    }


                } else if("del".equals(result[0])) {
                    // 删除虚拟机结点,去已有的ECS组里面删除
                    String delId = result[1];
                    // 去ECS里面找
                    if (ECSMAP.get(delId) != null) {
                        ServerType serverType = ECSMAP.get(delId);
                        if (serverType.Anode != null && serverType.Bnode == null) {
                            // 释放A结点
                            serverType.cpuNum += serverType.Anode.cpuNum;serverType.memory += serverType.Anode.memory;serverType.Anode = null;
                            ECSMAP.remove(delId);
                            continue;
                        }else if(serverType.Anode == null && serverType.Bnode != null) {
                            serverType.cpuNum += serverType.Bnode.cpuNum;serverType.memory += serverType.Bnode.memory;serverType.Bnode = null;
                            ECSMAP.remove(delId);
                            continue;
                        }else if(serverType.Anode != null && serverType.Bnode != null) {
                            serverType.cpuNum = serverType.cpuNum + (serverType.Anode.cpuNum + serverType.Bnode.cpuNum);
                            serverType.memory = serverType.memory + (serverType.Anode.memory + serverType.Bnode.memory);
                            serverType.Anode = serverType.Bnode = null;
                            ECSMAP.remove(delId);
                            continue;
                        }
                    }
                }
        }

        // 对所有请求进行资源规划
        stringBuilder.append("(purchase, "+buycount+")\n");
        for (String key : buyDetail.keySet()) {
            stringBuilder.append("(" + key + ", " + buyDetail.get(key) + ")\n");
        }
        stringBuilder.append("(migration, 0)\n");
        for (Integer key : deploydetail.keySet()) {
            if ((deploydetail.get(key).equals("")) ){
                stringBuilder.append("("+key+")\n");
            }else {
                stringBuilder.append("(" + key +", " + deploydetail.get(key) + ")\n");
            }
        }
    }

}







