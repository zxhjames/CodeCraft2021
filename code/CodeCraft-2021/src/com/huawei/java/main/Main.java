package com.huawei.java.main;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.huawei.java.main.ToolsBuilder.*;

public class Main {

    public static void main(String[] args) {
        // TODO: read standard input
        // TODO: process
        // TODO: write standard output
        // TODO: System.out.flush()


        Long beginTime = System.currentTimeMillis();
        // load file to memory from disk
        String root =  System.getProperty("user.dir");
        String train_data_1 = root + "/training-1.txt";
        String train_data_2 = root + "/training-2.txt";

        int N = 0; // 用于采购的服务器类型数量
        List<ServerType> serverTypeList = new ArrayList<>();
        int M = 0; // 售卖的虚拟机的类型数量
        List<VirtualType> virtualTypeList = new ArrayList<>();
        int T = 0; // 未来出现请求的天数
        int R = 0; // 一天中的每一轮
        List<List<RequestData>> requestDataList = new ArrayList<>();

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

            // 列表转为map (key->Type Value->VirtualType）
            Map<String,VirtualType> virtualTypeMap = virtualTypeList.stream().collect(Collectors.toMap(VirtualType::getType, Function.identity()));
            // 请求数据
            T = Integer.parseInt(bufferedReader.readLine());
            for (int i = 0; i< T;++i) {
                R = Integer.parseInt(bufferedReader.readLine());
                for (int j = 0;j<R;++j) {
                    // print from here
                }
            }
            bufferedReader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        Long endTime = System.currentTimeMillis();
        System.out.println("总耗时: " + (endTime - beginTime));
        // 垃圾回收
        GC();
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
                          List<ServerType> serverTypes) {

        Map<String,List<VirtualType>> requestVirtualMachine = new HashMap<>();//用于存储每天的虚拟机请求
        List<ServerType> ECS = new ArrayList<>(); // 已有的ECS服务器


        for (int i = 0;i<count;++i) {

            String[] result = null;
            String line = null;
            try {
                line = bufferedReader.readLine();
                result = line.substring(1,line.length() - 1)
                        .replace(" ","").split(",");

                if ("add".equals(result[0])) {
                    // 添加服务器,first fit
                    if (null == requestVirtualMachine.get(result[1])) {
                        List<VirtualType> virtualTypes = new ArrayList<>();
                        virtualTypes.add(virtualTypeMap.get(result[1]));
                        requestVirtualMachine.put(result[1],virtualTypes);
                    }else {
                        requestVirtualMachine.get(result[1]).add(virtualTypeMap.get(result[1]));
                    }

                } else if ("del".equals(result[0])) {
                    // 删除服务器
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            // 首次适应 first fit
            for (String key : requestVirtualMachine.keySet()) {
                // 当日的所有虚拟机请求数据
                VirtualType virtualType = requestVirtualMachine.get(key).get(0);
                // 记录需要移除的虚拟机
                List<VirtualType> removeObj = requestVirtualMachine.get(virtualType.type);
                //在已有的ECS中进行迁移或者分配
                if (ECS.size()>0) {
                    // 如果满足结点要求就分配，否则进行迁移
                    for (ServerType serverType : ECS) {
                        // 单点部署
                        if (virtualType.isDeployWithTowNode == 0) {
                            // A结点满足就直接分配A结点,否则分配B结点
                            if (serverType.cpuNum >= virtualType.cpuNum && serverType.memory > virtualType.memory) {
                                if (serverType.Anode == null) {
                                    serverType.Anode = virtualType;
                                    serverType.cpuNum -= virtualType.cpuNum;
                                    serverType.memory -= virtualType.memory;
                                    System.out.println(virtualType.type + "分配完成至"+serverType.type+"A结点");
                                    // 移除这个虚拟机结点
                                    removeObj.remove(removeObj.size()-1);
                                    continue;
                                }
                                if (serverType.Bnode == null) {
                                    serverType.Bnode = virtualType;
                                    serverType.cpuNum -= virtualType.cpuNum;
                                    serverType.memory -= virtualType.memory;
                                    System.out.println(virtualType.type + "分配完成"+serverType.type+"B结点");
                                    removeObj.remove(removeObj.size()-1);
                                    continue;
                                }
                            }
                        }

                        // 多点部署
                        if (virtualType.isDeployWithTowNode == 1 && serverType.cpuNum >= virtualType.cpuNum && serverType.memory >= virtualType.memory) {
                            if (serverType.Anode == null && serverType.Bnode == null) {
                                serverType.Anode = serverType.Bnode = virtualType;
                                serverType.cpuNum -= virtualType.cpuNum;
                                serverType.memory -= virtualType.memory;
                                System.out.println(virtualType.type + "分配完成至" + serverType.type + "双结点");
                                removeObj.remove(removeObj.size()-1);
                            }
                        }

                        // 考虑对剩下的虚拟机进行迁移 迁移策略?
                    }
                }

                // 如果不能迁移分配，则去购买新的ECS
                for (int k = 0;k < serverTypes.size(); ++k) {
                    //  如果当前cpu和内存满足要求，就将虚拟机分配到这个服务器结点,并减去相应的服务器资源，下一轮迭代时，重新遍历
                    if (serverTypes.get(k).cpuNum > virtualType.cpuNum) {

                    }
                }
            }



            // 每一天结束都要重置一下map
            requestVirtualMachine.clear();
        }

        // 对所有请求进行资源规划



        }

    }







}