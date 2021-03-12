package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description: 工具类
 * @author: 占翔昊
 * @create 2021-03-10 15:34
 **/
public class ToolsBuilder {
    // 用于去除字符串首位的括号和中间的逗号:服务器
    static ServerType SplitServerTypeline(String line) {
        String[] result = null;
        try {
            result = line.substring(1,line.length() - 1)
                    .replace(" ","").split(",");
        }catch (Exception e) {
            e.printStackTrace();
        }
        String type = result[0];
        Integer cpu = Integer.parseInt(result[1]) / 2;
        Integer memory = Integer.parseInt(result[2]) / 2;
        Integer machinecost = Integer.parseInt(result[3]);
        Integer dayusecost = Integer.parseInt(result[4]);
        return new ServerType(type,cpu,memory,cpu,memory,machinecost,dayusecost);
    }

    // 用于去除字符串首位的括号和中间的逗号:虚拟机
    static VirtualType SplitVirtualTypeline(String line) {
        String[] result = null;
        try {
            result = line.substring(1,line.length() - 1)
                    .replace(" ","").split(",");
        }catch (Exception e) {
            e.printStackTrace();
        }
        return new VirtualType(result[0],Integer.parseInt(result[1]),Integer.parseInt(result[2]),Integer.parseInt(result[3]));
    }

}
