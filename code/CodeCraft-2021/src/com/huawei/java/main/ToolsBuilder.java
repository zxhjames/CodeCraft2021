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
        return new ServerType(result[0],Integer.parseInt(result[1]),Integer.parseInt(result[2]),Integer.parseInt(result[3]),Integer.parseInt(result[4]));
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

    // 处理请求数据行
    static RequestData SplitRequestline(String line) {
        String[] result = null;
        try{
            result = line.substring(1,line.length() - 1)
                    .replace(" ","").split(",");

            if ("add".equals(result[0])) {
                // 添加服务器
            } else if ("del".equals(result[0])) {
                // 删除服务器
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return new RequestData("type",result[1]);
    }

    // 打印对象
    static void PrintObj(Object ...obj) {
        for (Object o : obj) {
            System.out.println(o.toString());
        }
    }

    // 内存回收
    static void GC() {
        System.gc();
    }
}
