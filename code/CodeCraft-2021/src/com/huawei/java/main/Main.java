package com.huawei.java.main;


import java.io.*;
import java.text.DecimalFormat;
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
        List<VirtualType> virtualTypeList = new ArrayList<>(); // 用于使用的虚拟机
        StringBuilder stringBuilder = new StringBuilder(); // 结果集合
        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS组
        Map<String,ServerType> ECSMAP = new HashMap<>(); // 用于存储虚拟机id与ECS的映射


        N = Integer.parseInt(in.nextLine());
        // 读取服务器信息
        for (int i = 0; i < N; ++i) {
            String str = in.nextLine();
            ServerType serverType = SplitServerTypeline(str);
            serverTypeList.add(serverType);
        }


        // 读取虚拟机信息
        M = Integer.parseInt(in.nextLine());
        for (int i=0;i<M;++i) {
            String str = in.nextLine();
            virtualTypeList.add(SplitVirtualTypeline(str));
        }

        List<ServerType> serverTypes = serverTypeList.stream().
                sorted(Comparator.comparing(a -> a.AcpuNum * 0.5 + a.Amemory * 0.5,Comparator.reverseOrder())).collect(Collectors.toList());

        // 虚拟机信息转map
        Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));
        // 请求数据
        T = Integer.parseInt(in.nextLine());
        for (int i = 0; i< T;++i) {
            R = Integer.parseInt(in.nextLine());
            // 每一天的调度信息
            Scheduler(in,stringBuilder,R,virtualTypeMap,serverTypes,ECS,ECSMAP);
        }
        System.out.println(stringBuilder.toString());
        in.close();
    }





    /**
     * 调度器
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

        // 每一天删除的结点的标记vm
       // Map<ServerType,Integer> DelVm = new HashMap<>();

        // 是否购买服务器
        boolean isfit = false;

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
                    isfit = false;

                    // 首先去已有的服务器上寻找空间
                    for (int k=0;k<ECS.size();++k) {
                        ServerType serverType = ECS.get(k);

                        // 单点部署试探 没有即创建
                        if (virtualType.isDeployWithTowNode == 0) {
                            if (serverType.AcpuNum >= virtualType.cpuNum && serverType.Amemory >= virtualType.memory) {
                                serverType.Anodes.add(virtualType);
                                serverType.AcpuNum -= virtualType.cpuNum;
                                serverType.Amemory -= virtualType.memory;
                                deployInfoList.add(new DeployInfo(virtualType.id,serverType,"A"));
                                ECSMAP.put(virtualType.id,serverType);
                                // 移除这个虚拟机结点
                                isfit = true;
                                break;
                            }
                            if (serverType.BcpuNum >= virtualType.cpuNum && serverType.Bmemory >= virtualType.memory) {
                                serverType.Bnodes.add(virtualType);
                                serverType.BcpuNum -= virtualType.cpuNum;
                                serverType.Bmemory -= virtualType.memory;
                                deployInfoList.add(new DeployInfo(virtualType.id,serverType,"B"));
                                ECSMAP.put(virtualType.id,serverType);
                                isfit = true;
                                break;
                            }
                        }

                        // 多点部署 没有即创建
                        if (virtualType.isDeployWithTowNode == 1 ) {
                            if (serverType.AcpuNum  >= virtualType.cpuNum / 2 && serverType.Amemory  >= virtualType.memory / 2
                                    && serverType.BcpuNum >= virtualType.cpuNum / 2 && serverType.Bmemory >= virtualType.memory / 2
                            ) {
                                virtualType.cpuNum /= 2;
                                virtualType.memory /= 2;
                                serverType.Anodes.add(virtualType);
                                serverType.Bnodes.add(virtualType);
                                serverType.AcpuNum -= virtualType.cpuNum;
                                serverType.Amemory -= virtualType.memory;
                                serverType.BcpuNum -= virtualType.cpuNum;
                                serverType.Bmemory -= virtualType.memory;
                                deployInfoList.add(new DeployInfo(virtualType.id,serverType,""));
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

                            // 双结点分配算法
                            if (virtualType.isDeployWithTowNode == 1) {
                                if (virtualType.cpuNum / 2 <= serverTypenode.AcpuNum
                                        && virtualType.memory / 2 <= serverTypenode.Amemory
                                        && virtualType.cpuNum / 2 <= serverTypenode.BcpuNum
                                        && virtualType.memory / 2 <= serverTypenode.Bmemory) {

                                    virtualType.cpuNum /= 2;
                                    virtualType.memory /= 2;
                                    serverType = new ServerType(serverTypenode.type,
                                            serverTypenode.AcpuNum - virtualType.cpuNum,
                                            serverTypenode.Amemory - virtualType.memory,
                                            serverTypenode.BcpuNum - virtualType.cpuNum,
                                            serverTypenode.Bmemory - virtualType.memory,
                                            new ArrayList<VirtualType>(),
                                            new ArrayList<VirtualType>());
                                }
                            }


                            // 单结点分配算法 默认分配A node
                            if (virtualType.isDeployWithTowNode == 0) {
                                if (virtualType.cpuNum <= serverTypenode.AcpuNum && virtualType.memory <= serverTypenode.Amemory) {
                                    List<VirtualType> virtualTypes = new ArrayList<>();
                                    virtualTypes.add(virtualType);
                                    serverType = new ServerType(serverTypenode.type,
                                            serverTypenode.AcpuNum - virtualType.cpuNum,
                                            serverTypenode.Amemory - virtualType.memory,
                                            serverTypenode.BcpuNum,
                                            serverTypenode.Bmemory,
                                            virtualTypes,
                                            new ArrayList<VirtualType>());
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
                                    deployInfoList.add(new DeployInfo(virtualType.id,serverType,""));
                                }else if(virtualType.isDeployWithTowNode == 0) {
                                    deployInfoList.add(new DeployInfo(virtualType.id,serverType,"A"));
                                }

                                ECS.add(DayECSList.get(DayECSList.size() - 1));
                                // 存储虚拟机ID => 服务器映射 注意 它只是id变了
                                ECSMAP.put(virtualType.id,serverType);
                                break;
                        }
                    }
                } else if("del".equals(result[0])) {
                    // 删除虚拟机结点,去已有的ECS组里面删除
                    String delId = result[1];

                    // 去ECS里面找
                    if (ECSMAP.get(delId) != null) {
                        ServerType serverType = ECSMAP.get(delId);
                        // 去服务器上删除掉这个虚拟机，并且回收资源

                        // 确定这个虚拟机是双结点还是单结点
                        boolean isSimple = true;
                        boolean isA = false;

                        // 先去A结点找
                        for (int k = 0;k<serverType.Anodes.size();++k) {
                            if (serverType.Anodes.get(k).id.equals(delId)) {
                                if (serverType.Anodes.get(k).isDeployWithTowNode == 1) {
                                    isSimple = false;
                                }
                                serverType.AcpuNum += serverType.Anodes.get(k).cpuNum;
                                serverType.Amemory += serverType.Anodes.get(k).memory;
                                serverType.Anodes.remove(k);
                                isA = true;
                                break;
                            }
                        }

                        // 找不到就去B里找
                        if (!isA) {
                            for (int k = 0;k<serverType.Bnodes.size();++k) {
                                if (serverType.Bnodes.get(k).id.equals(delId)) {
                                    serverType.BcpuNum += serverType.Bnodes.get(k).cpuNum;
                                    serverType.Bmemory += serverType.Bnodes.get(k).memory;
                                    serverType.Bnodes.remove(k);
                                    break;
                                }
                            }
                        }

                        // 如果是双节点就一起删除
                        if (!isSimple) {
                            // 删除B结点
                            for (int k =0;k<serverType.Bnodes.size();++k) {
                                if (serverType.Bnodes.get(k).id.equals(delId)) {
                                    serverType.BcpuNum += serverType.Bnodes.get(k).cpuNum;
                                    serverType.Bmemory += serverType.Bnodes.get(k).memory;
                                    serverType.Bnodes.remove(k);
                                    break;
                                }
                            }
                        }


                        ECSMAP.remove(delId);
                        // 移除部署信息
                        if (!isSimple) {
                            deployInfoList.stream()
                                    .filter(producer -> producer.serverType.id.equals(serverType.id) &&
                                             producer.vmid.equals(delId) &&
                                            (producer.node.equals("")))
                                    .findFirst()
                                    .map(p -> {
                                        deployInfoList.remove(p);
                                        return p;
                                    });
                        }else if (isA){
                            deployInfoList.stream()
                                    .filter(producer -> producer.serverType.id.equals(serverType.id) &&
                                            producer.vmid.equals(delId) &&
                                            producer.node.equals("A") )
                                    .findFirst()
                                    .map(p -> {
                                        deployInfoList.remove(p);
                                        return p;
                                    });

                        }else if(!isA) {
                            deployInfoList.stream()
                                    .filter(producer -> producer.serverType.id.equals(serverType.id) &&
                                            producer.vmid.equals(delId) &&
                                            producer.node.equals("B") )
                                    .findFirst()
                                    .map(p -> {
                                        deployInfoList.remove(p);
                                        return p;
                                    });
                        }

                    }
                }
        }

        // 每天结束之后删除部署消息中已经失效的


        buycount = buycount + DayECSList.size();
        // 对所有请求进行资源规划
        stringBuilder.append("(purchase,"+buyDetail.size()+")\n");
        for (String key : buyDetail.keySet()) {
            stringBuilder.append("(" + key + "," + buyDetail.get(key) + ")\n");
        }
        stringBuilder.append("(migration,0)\n");

        // 这里有del的就不输出
        for (DeployInfo key : deployInfoList) {
//            if (DelVm.get(key.serverType) != null ) {
                if (("").equals(key.node)) {
                    stringBuilder.append("("+key.serverType.id+")\n");
                }else {
                    stringBuilder.append("(" + key.serverType.id +"," + key.node + ")\n");
                }
//            }
        }
    }

}







