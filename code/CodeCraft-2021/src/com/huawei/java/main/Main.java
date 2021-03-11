package com.huawei.java.main;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.huawei.java.main.ToolsBuilder.*;

public class Main {

    public static void main(String[] args) throws IOException {
        // 构建输入输出
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(isr);
        int N,M,T,R;
        List<ServerType> serverTypeList = new ArrayList<>();  // 用于采购的服务器类型数量
        List<VirtualType> virtualTypeList = new ArrayList<>();
        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS组
        StringBuilder stringBuilder = new StringBuilder("");
        N = Integer.parseInt(in.readLine());
        for (int i = 0; i < N; ++i) {
                String str = in.readLine();
                serverTypeList.add(SplitServerTypeline(str));
        }
        List<ServerType> serverTypes = serverTypeList.stream().
                sorted(Comparator.comparing(ServerType::getMachineConsume)).collect(Collectors.toList());

        // 读取M行的每一种类型的虚拟机
        M = Integer.parseInt(in.readLine());
        for (int i=0;i<M;++i) {
            String str = in.readLine();
            virtualTypeList.add(SplitVirtualTypeline(str));
        }

        // 列表转为map (key->Type Value->VirtualType）各种种类的虚拟机
        Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));
        // 请求数据
        T = Integer.parseInt(in.readLine());
        for (int i = 0; i< T;++i) {
            R = Integer.parseInt(in.readLine());
            // 每一天的调度信息
            Scheduler(in,stringBuilder,R,virtualTypeMap,serverTypes,ECS);
        }
        System.out.println(stringBuilder.toString());
        in.close();
    }





    /**
     * 调度策略
     * @param in 输入的请求
     * @Param count 请求数量
     * @param virtualTypeMap 虚拟机参数映射
     * @param serverTypes ECS参数
     */
    static void Scheduler(BufferedReader in,
                          StringBuilder stringBuilder,
                          int count,
                          Map<String,VirtualType> virtualTypeMap,
                          List<ServerType> serverTypes,
                          List<ServerType> ECS) throws IOException {
        List<VirtualType> virtualTypes = new ArrayList<>(); //用于存储每天的虚拟机请求
        int buycount = 0; // 记录每天购买的服务器数量
        Map<String,Integer> buyDetail = new HashMap<>(); // 详细购买情况
        Map<Integer,String> deploydetail = new HashMap<>(); // 详细的分配情况

        // main loop
        for (int i = 0;i<count; ++i) {
            String[] result = null;
            String line = null;
                line = in.readLine();
                result = line.substring(1,line.length() - 1)
                        .replace(" ","").split(",");

                if("add".equals(result[0])) {
                    // 添加虚拟机结点
                    // System.out.println("添加虚拟机请求,添加了" + virtualTypeMap.get(result[1]).type);
                    VirtualType v = virtualTypeMap.get(result[1]);
                    v.id = result[2];
                    virtualTypes.add(v);
                } else if("del".equals(result[0])) {
                    // 删除虚拟机结点,去已有的ECS组里面删除
                    String delId = result[1];

                    // 去ECS里面找
                    for (ServerType serverType : ECS) {
                        if (serverType.Anode != null && delId.equals(serverType.Anode.id)) {
                            if (serverType.Anode.isDeployWithTowNode == 0) {
                                serverType.cpuNum += serverType.Anode.cpuNum;
                                serverType.memory += serverType.Anode.memory;
                                // System.out.println("删除了单结点A" + serverType.Anode.type);
                                serverType.Anode = null; // 释放资源
                                break;
                            }else if (serverType.Anode.isDeployWithTowNode == 1) {
                                serverType.cpuNum += serverType.Anode.cpuNum;
                                serverType.memory += serverType.Anode.memory;
                                // System.out.println("删除了双结点" + serverType.Anode.type);
                                serverType.Anode = serverType.Bnode = null; // 释放资源
                                break;
                            }
                        }else if(serverType.Bnode != null && delId.equals(serverType.Bnode.id)) {
                            if (serverType.Bnode.isDeployWithTowNode == 0) {
                                serverType.cpuNum += serverType.Bnode.cpuNum;
                                serverType.memory += serverType.Bnode.memory;
                                // System.out.println("删除了单结点B" + serverType.Bnode.type);
                                serverType.Bnode = null; // 释放资源
                                break;
                            }
                        }

                    }
                }

            // 首次适应 first fit
            for (int p = 0;p<virtualTypes.size();++p) {
                VirtualType virtualType = virtualTypes.get(p);
                // 当日的所有虚拟机请求数据
                //在已有的ECS中进行迁移或者分配
                if (0 < ECS.size()) {
                    // 如果满足结点要求就分配，否则进行迁移
                    for (ServerType serverType : ECS) {
                        // 单点部署
                        if (virtualType.isDeployWithTowNode == 0) {
                            // A结点满足就直接分配A结点,否则分配B结点
                            if (serverType.cpuNum >= virtualType.cpuNum && serverType.memory > virtualType.memory) {
                                if (serverType.Anode == null) {
                                    serverType.Anode = virtualType;serverType.cpuNum -= virtualType.cpuNum;serverType.memory -= virtualType.memory;
                                    //System.out.println(virtualType.type + "分配完成至"+serverType.type+"A结点");
                                    deploydetail.put(serverType.id,"A");
                                    // 移除这个虚拟机结点
                                    virtualTypes.remove(p);
                                    p-=1;
                                    break;
                                }
                                if (serverType.Bnode == null) {
                                    serverType.Bnode = virtualType;serverType.cpuNum -= virtualType.cpuNum;serverType.memory -= virtualType.memory;
                                    // System.out.println(virtualType.type + "分配完成"+serverType.type+"B结点");
                                    deploydetail.put(serverType.id,"B");
                                    virtualTypes.remove(p);
                                    p-=1;
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
                                virtualTypes.remove(p);
                                p-=1;
                                break;
                            }
                        }

                        // 迁移策略
                        //if (virtualTypes.size() > 0) {
                        // System.out.println("迁移策略...");
                        //}
                    }
                }
            }


            // 如果不能迁移分配，则去购买新的ECS
            if (virtualTypes.size() > 0) {
                for (int n=0;n<virtualTypes.size();++n) {
                    VirtualType tmp = virtualTypes.get(n);
                    for (int k = 0; k < serverTypes.size(); ++k) {
                        //  如果当前cpu和内存满足要求，就将虚拟机分配到这个服务器结点,并减去相应的服务器资源，下一轮迭代时，重新遍历
                        // 优先分配双结点
                        if (serverTypes.get(k).cpuNum > tmp.cpuNum && serverTypes.get(k).memory > tmp.memory) {
                            ServerType serverType = null;
                            if (tmp.isDeployWithTowNode == 1) {
                                serverType = new ServerType(serverTypes.get(k).type,serverTypes.get(k).cpuNum - tmp.cpuNum,serverTypes.get(k).memory - tmp.memory,serverTypes.get(k).machineConsume,serverTypes.get(k).dayConsume,tmp,tmp);
                                //System.out.println("购买了 " + serverType.type + "双节点部署 " + tmp.type);
                                serverType.id = ECS.size();
                                deploydetail.put(serverType.id,"");
                            }else if (tmp.isDeployWithTowNode == 0) {
                                serverType = new ServerType(serverTypes.get(k).type,serverTypes.get(k).cpuNum - tmp.cpuNum,serverTypes.get(k).memory - tmp.memory,serverTypes.get(k).machineConsume,serverTypes.get(k).dayConsume,tmp,null);
                                //System.out.println("购买了 " + serverType.type + "A节点部署 " + tmp.type);
                                serverType.id = ECS.size();
                                deploydetail.put(serverType.id,"A");
                            }
                            // 记录已经购买的ECS的ID并且分配它
                            buycount = buycount + 1;
                            if (buyDetail.get(serverType.type) == null) {
                                buyDetail.put(serverType.type,1);
                            }else{
                                buyDetail.put(serverType.type,buyDetail.get(serverType.type) + 1);
                            }
                            // 分配它
                            ECS.add(serverType);
                            break;
                        }
                    }
                }
            }
            // 每一天结束都要重置一下map
            virtualTypes.clear();
        }

        // 对所有请求进行资源规划
        stringBuilder.append("(purchase,"+buycount+")\n");
        for (String key : buyDetail.keySet()) {
            stringBuilder.append("(" + key + "," + buyDetail.get(key) + ")\n");
        }
        stringBuilder.append("(migration,0)\n");
        for (Integer key : deploydetail.keySet()) {
            if ((deploydetail.get(key).equals("")) ){
                stringBuilder.append("("+key+")\n");
            }else {
                stringBuilder.append("(" + key +"," + deploydetail.get(key) + ")\n");
            }
        }
    }

}







