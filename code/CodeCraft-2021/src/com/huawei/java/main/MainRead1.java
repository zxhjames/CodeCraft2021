package com.huawei.java.main;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.huawei.java.main.ToolsBuilder.*;

public class MainRead1 {

    public static void main(String[] args) {
        // TODO: read standard input
        // TODO: process
        // TODO: write standard output
        // TODO: System.out.flush()


       // Long beginTime = System.currentTimeMillis();
        // load file to memory from disk
        String root =  System.getProperty("user.dir");
        String train_data_1 = root + "/training-1.txt";
       // String train_data_1 = root + "/training-2.txt";

        int N = 0; // 用于采购的服务器类型数量
        List<ServerType> serverTypeList = new ArrayList<>();
        int M = 0; // 售卖的虚拟机的类型数量
        List<VirtualType> virtualTypeList = new ArrayList<>();
        int T = 0; // 未来出现请求的天数
        int R = 0; // 一天中的每一轮
        List<List<RequestData>> requestDataList = new ArrayList<>();
        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS组
       // Map<String,List<RequestData>> Pool = new HashMap<>(); // 用于记录已购买的服务器资源



        // read to buffer
        File file1 = new File(train_data_1);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file1));
            String line = null;
            // 按行读取

            // 读取N行的每一种类型的服务器
            N = Integer.parseInt(bufferedReader.readLine());
            for (int i = 0; i < N; ++i) {
                serverTypeList.add(SplitServerTypeline(bufferedReader.readLine()));
            }

            // todo 开始对容量进行规划,对所有服务器按照成本进行升序排序,对所有的虚拟机按照cpu数进行降序排序
            List<ServerType> serverTypes = serverTypeList.stream().
                    sorted(Comparator.comparing(ServerType::getMachineConsume)).collect(Collectors.toList());

            // 读取M行的每一种类型的虚拟机
            M = Integer.parseInt(bufferedReader.readLine());
            for (int i=0;i<M;++i) {
                virtualTypeList.add(SplitVirtualTypeline(bufferedReader.readLine()));
            }

            // 列表转为map (key->Type Value->VirtualType）各种种类的虚拟机
            Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));


            // 请求数据
            T = Integer.parseInt(bufferedReader.readLine());

            for (int i = 0; i< T;++i) {
                R = Integer.parseInt(bufferedReader.readLine());
                // 每一天的调度信息
                Scheduler(bufferedReader,R,virtualTypeMap,serverTypes,ECS);
//                if (i == 100) {
//                    System.out.println("结束");
//                    break;
//                }
            }
            System.out.flush();
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Long endTime = System.currentTimeMillis();
       // System.out.println("总耗时: " + (endTime - beginTime));
        // 垃圾回收
        //GC();
    }



    /**
     * 调度策略
     * @param bufferedReader 输入的请求
     * @Param count 请求数量
     * @param virtualTypeMap 虚拟机参数映射
     * @param serverTypes ECS参数
     */
    static void Scheduler(BufferedReader bufferedReader,
                          int count,
                          Map<String,VirtualType> virtualTypeMap,
                          List<ServerType> serverTypes,
                          List<ServerType> ECS) {
        List<VirtualType> virtualTypes = new ArrayList<>(); //用于存储每天的虚拟机请求
        int buycount = 0; // 记录每天购买的服务器数量
        Map<String,Integer> buyDetail = new HashMap<>(); // 详细购买情况
        //int movecount = 0; // 记录迁移的数量
        Map<Integer,String> deploydetail = new HashMap<>(); // 详细的分配情况

        // main loop
        for (int i = 0;i<count;++i) {

            String[] result = null;
            String line = null;
            try {
                line = bufferedReader.readLine();
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
            } catch (IOException e) {
                e.printStackTrace();
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
        System.out.println("(purchase,"+buycount+")");
        for (String key : buyDetail.keySet()) {
            System.out.println("(" + key + "," + buyDetail.get(key) + ")");
        }
        System.out.println("(migration,0)"); // 因为没有考虑迁移 所以不输出
        for (Integer key : deploydetail.keySet()) {
            if ((deploydetail.get(key).equals("")) ){
                System.out.println("("+key+")");
            }else {
                System.out.println("(" + key +"," + deploydetail.get(key) + ")");
            }
        }
    }

    }







