package com.huawei.java.main;


import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.huawei.java.main.ToolsBuilder.*;

public class Main {
    static int buycount = 0; // 记录购买的服务器总数量

    public static void main(String[] args) throws IOException {
        // 构建输入输出
        Scanner in = new Scanner(System.in);
        int N,M,T,R;
        List<ServerType> serverTypeList = new ArrayList<>();  // 用于采购的服务器类型数量
        List<VirtualType> virtualTypeList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS组
        Map<String,ServerType> ECSMAP = new HashMap<>(); // 用于存储虚拟机id与ECS的映射

        N = Integer.parseInt(in.nextLine());
        for (int i = 0; i < N; ++i) {
            String str = in.nextLine();
            serverTypeList.add(SplitServerTypeline(str));
        }

//        List<ServerType> serverTypes = serverTypeList.stream().
//                sorted(Comparator.comparing(ServerType::getCpuNum)).collect(Collectors.toList());

        // 读取M行的每一种类型的虚拟机
        M = Integer.parseInt(in.nextLine());
        for (int i=0;i<M;++i) {
            String str = in.nextLine();
            virtualTypeList.add(SplitVirtualTypeline(str));
        }

        // 列表转为map (key->Type Value->VirtualType）各种种类的虚拟机
        Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));
        // 请求数据
        T = Integer.parseInt(in.nextLine());
        for (int i = 0; i< T;++i) {
            R = Integer.parseInt(in.nextLine());
            // 每一天的调度信息
            Scheduler(in,stringBuilder,R,virtualTypeMap,serverTypeList,ECS,ECSMAP);
        }
        System.out.println(stringBuilder.toString());
        in.close();
    }





    /**
     * 调度策略
     * @param in 输入的请求
     * @param stringBuilder 结果
     * @param count 请求数量
     * @param virtualTypeMap 虚拟机参数映射
     * @param serverTypes ECS参数
     * @param ECS 所有的已有的服务器
     * @param ECSMAP 存储服务器类型与详细信息的映射
     */


    static void Scheduler(Scanner in,
                          StringBuilder stringBuilder,
                          int count,
                          Map<String,VirtualType> virtualTypeMap,
                          List<ServerType> serverTypes,
                          List<ServerType> ECS,
                          Map<String,ServerType> ECSMAP) {

        // 详细购买情况
        Map<String,Integer> buyDetail = new HashMap<>();

        // 用于对当天购买的服务器进行分组
        List<ServerType> DayECSList = new ArrayList<>();

        // 每一天的部署顺序
        List<DeployInfo> deployInfoList = new ArrayList<>();

        // main loop
        for (int i = 0;i<count; ++i) {
            // 接受输入
            String line = in.nextLine();
            String[] result = line.substring(1,line.length() - 1)
                        .replace(" ","").split(",");
                if("add".equals(result[0])) {

                    // 检索虚拟机参数表
                    VirtualType v = virtualTypeMap.get(result[1]);

                    // 重新分配
                    VirtualType virtualType = new VirtualType(v.type,v.cpuNum,v.memory,v.isDeployWithTowNode);

                    virtualType.id = result[2];

                    // 判断是否此次操作还需要购买
                    boolean isfit = false;


                    for (int k=0;k<ECS.size();++k) {
                        ServerType serverType = ECS.get(k);

                        // 单点部署试探
                        if (virtualType.isDeployWithTowNode == 0) {
                            if (serverType.Anode == null && serverType.AcpuNum >= virtualType.cpuNum && serverType.Amemory >= virtualType.memory) {
                                serverType.Anode = virtualType;
                                serverType.AcpuNum -= virtualType.cpuNum;
                                serverType.Amemory -= virtualType.memory;
                                deployInfoList.add(new DeployInfo(serverType,"A"));
                                ECSMAP.put(virtualType.id,serverType);
                                // 移除这个虚拟机结点
                                isfit = true;
                                break;
                            }
                            if (serverType.Bnode == null && serverType.BcpuNum >= virtualType.cpuNum && serverType.Bmemory >= virtualType.memory) {
                                serverType.Bnode = virtualType;
                                serverType.BcpuNum -= virtualType.cpuNum;
                                serverType.Bmemory -= virtualType.memory;
                                deployInfoList.add(new DeployInfo(serverType,"B"));
                                ECSMAP.put(virtualType.id,serverType);
                                isfit = true;
                                break;
                            }
                        }

                        // 多点部署
                        if (virtualType.isDeployWithTowNode == 1 ) {
                            if (serverType.Anode == null && serverType.Bnode == null && serverType.AcpuNum  >= virtualType.cpuNum / 2 && serverType.Amemory  >= virtualType.memory / 2) {
                                virtualType.cpuNum /= 2;
                                virtualType.memory /= 2;
                                serverType.Anode = virtualType;
                                serverType.Bnode = virtualType;
                                serverType.AcpuNum -= virtualType.cpuNum;
                                serverType.Amemory -= virtualType.memory;
                                serverType.BcpuNum = serverType.AcpuNum;
                                serverType.Bmemory = serverType.Amemory;
                                deployInfoList.add(new DeployInfo(serverType,""));
                                ECSMAP.put(virtualType.id,serverType);
                                isfit = true;
                                break;
                            }
                        }
                        // 迁移算法 暂时不考虑
                    }



                    if(!isfit) {
                        // 需要买服务器
                        ServerType serverType = null;
                        for (int k = 0; k < serverTypes.size(); ++k) {

                            // 申请一台新机器
                            ServerType serverTypenode = serverTypes.get(k);

                            // 加入当日机器组

                            // 双结点分配算法
                            if (virtualType.isDeployWithTowNode == 1) {
                                if (virtualType.cpuNum / 2 <= serverTypenode.AcpuNum && virtualType.memory / 2 <= serverTypenode.Amemory) {
                                    virtualType.cpuNum /= 2;
                                    virtualType.memory /= 2;
                                    serverType = new ServerType(serverTypenode.type,
                                            serverTypenode.AcpuNum - virtualType.cpuNum,
                                            serverTypenode.Amemory - virtualType.memory,
                                            serverTypenode.BcpuNum - virtualType.cpuNum,
                                            serverTypenode.Bmemory - virtualType.memory,
                                            virtualType,
                                            virtualType);
                                }
                            }


                            // 单结点分配算法 默认分配A node
                            if (virtualType.isDeployWithTowNode == 0) {
                                if (virtualType.cpuNum <= serverTypenode.AcpuNum && virtualType.memory <= serverTypenode.Amemory) {
                                    serverType = new ServerType(serverTypenode.type,
                                            serverTypenode.AcpuNum - virtualType.cpuNum,
                                            serverTypenode.Amemory - virtualType.memory,
                                            serverTypenode.BcpuNum,
                                            serverTypenode.Bmemory,
                                            virtualType,
                                            null);
                                }
                            }

                            // 重置serverid
                                DayECSList.add(serverType);
                                int tc = 0;
                                Map<String, List<ServerType>> collect = DayECSList.stream().collect(Collectors.groupingBy(t -> t.type));
                                for (String key : collect.keySet()) {
                                    List<ServerType> tmp = collect.get(key);
                                    for (int p = 0;p<tmp.size();p++) {
                                        tmp.get(p).id = buycount + tc;
                                        ++tc;
                                    }
                                }

                                // 纪录购买的详细信息
                                if (buyDetail.get(serverType.type) == null) {
                                    buyDetail.put(serverType.type,1);
                                }else{
                                    buyDetail.put(serverType.type,buyDetail.get(serverType.type) + 1);
                                }

                                if (virtualType.isDeployWithTowNode == 1) {
                                    deployInfoList.add(new DeployInfo(serverType,""));
                                }else if(virtualType.isDeployWithTowNode == 0) {
                                    deployInfoList.add(new DeployInfo(serverType,"A"));
                                }

                                ECS.add(DayECSList.get(DayECSList.size() - 1));
                                // 存储虚拟机ID => 服务器映射 注意 它只是id变了
                                ECSMAP.put(virtualType.id,serverType);
                                break;
                          //  System.out.println("null!!!!");

                        }
                    }
                } else if("del".equals(result[0])) {
                    // 删除虚拟机结点,去已有的ECS组里面删除
                    String delId = result[1];

                    // 去ECS里面找
                    if (ECSMAP.get(delId) != null) {
                        ServerType serverType = ECSMAP.get(delId);

                        // 只有一个为空，那肯定存在一个虚拟机结点
                        if (serverType.Anode != null && serverType.Bnode == null) {
                            // 释放A结点
                            serverType.AcpuNum += serverType.Anode.cpuNum;serverType.Amemory += serverType.Anode.memory;serverType.Anode = null;
                            ECSMAP.remove(delId);
                            // 同时在列表中移除部署信息
                            deployInfoList.stream()
                                    .filter(producer -> producer.serverType.id.equals(serverType.id) && producer.node.equals("A"))
                                    .findFirst()
                                    .map(p -> {
                                        deployInfoList.remove(p);
                                        return p;
                                    });

                            continue;
                        }else if(serverType.Anode == null && serverType.Bnode != null) {
                            // 释放B
                            serverType.BcpuNum += serverType.Bnode.cpuNum;serverType.Bmemory += serverType.Bnode.memory;serverType.Bnode = null;
                            ECSMAP.remove(delId);
                            // 同时在列表中移除部署信息
                            deployInfoList.stream()
                                    .filter(producer -> producer.serverType.id.equals(serverType.id) && producer.node.equals("B"))
                                    .findFirst()
                                    .map(p -> {
                                        deployInfoList.remove(p);
                                        return p;
                                    });

                            continue;
                        }else if(serverType.Anode != null && serverType.Bnode != null) {
                            // 两个都不为空，可能其中一个是虚拟机结点，也可能两个都是
                            if (serverType.Anode.isDeployWithTowNode == 1 || serverType.Bnode.isDeployWithTowNode == 1) {
                                // 肯定是双结点 要回收
                                serverType.AcpuNum = serverType.AcpuNum + serverType.Anode.cpuNum ;
                                serverType.Amemory = serverType.Amemory + serverType.Bnode.memory ;
                                serverType.BcpuNum = serverType.AcpuNum;
                                serverType.Bmemory = serverType.Amemory;
                                serverType.Anode = serverType.Bnode = null;
                                ECSMAP.remove(delId);
                                // 同时在列表中移除部署信息
                                deployInfoList.stream()
                                        .filter(producer -> producer.serverType.id.equals(serverType.id) && producer.node.equals(""))
                                        .findFirst()
                                        .map(p -> {
                                            deployInfoList.remove(p);
                                            return p;
                                        });

                                continue;
                            }else if (serverType.Anode.isDeployWithTowNode == 0 && delId.equals(serverType.Anode.id)) {
                                serverType.AcpuNum += serverType.Anode.cpuNum;
                                serverType.Amemory += serverType.Anode.memory;
                                serverType.Anode = null;
                                ECSMAP.remove(delId);
                                // 同时在列表中移除部署信息
                                deployInfoList.stream()
                                        .filter(producer -> producer.serverType.id.equals(serverType.id) && producer.node.equals("A"))
                                        .findFirst()
                                        .map(p -> {
                                            deployInfoList.remove(p);
                                            return p;
                                        });
                                continue;
                            }else if (serverType.Bnode.isDeployWithTowNode == 0 && delId.equals(serverType.Bnode.id)) {
                                serverType.BcpuNum += serverType.Bnode.cpuNum;
                                serverType.Bmemory += serverType.Bnode.memory;
                                serverType.Bnode = null;
                                ECSMAP.remove(delId);
                                // 同时在列表中移除部署信息
                                deployInfoList.stream()
                                        .filter(producer -> producer.serverType.id.equals(serverType.id) && producer.node.equals("B"))
                                        .findFirst()
                                        .map(p -> {
                                            deployInfoList.remove(p);
                                            return p;
                                        });
                                continue;
                            }
                        }
                    }
                }
        }


        buycount = buycount + DayECSList.size();
       // System.out.println(buycount);
        // 今天的任务已经分配完毕 注意是每一个请求 需要完成 1.添加新的服务器，添加部署信息，添加新的购买数量，重置
//        if (DayECSList.size() > 0) {
//            ECS.addAll(DayECSList);
//            buycount = buycount + DayECSList.size();
//        }



        // 对所有请求进行资源规划
        stringBuilder.append("(purchase,"+buyDetail.size()+")\n");
        for (String key : buyDetail.keySet()) {
            stringBuilder.append("(" + key + "," + buyDetail.get(key) + ")\n");
        }
        stringBuilder.append("(migration,0)\n");
        for (DeployInfo key : deployInfoList) {
            if (("").equals(key.node)) {
                stringBuilder.append("("+key.serverType.id+")\n");
            }else {
                stringBuilder.append("(" + key.serverType.id +"," + key.node + ")\n");
            }
        }
       // System.out.println("买了"+ DayECSList.size()+"台" + " " +deployInfoList.size());
    }

}







