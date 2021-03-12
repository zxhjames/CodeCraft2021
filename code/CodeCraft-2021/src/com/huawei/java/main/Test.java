package com.huawei.java.main;

import sun.jvm.hotspot.jdi.IntegerTypeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 10:46
 **/
public class Test {
    public static void main(String[] args) {
        //testSpliter();
        //testGroup(0);
        testVir();
        System.gc();
    }

    // 字符串分隔
    static void testSpliter() {
        String str = "(host3NX39, 148, 322, 57061, 71)";
        System.out.println(ToolsBuilder.SplitServerTypeline(str).AcpuNum);
    }

    // 对每天的购买的服务器进行分组
    static void testGroup(int tc){
        List<Node> nodeList = new ArrayList<>();
        nodeList.add(new Node("A",1));
        nodeList.add(new Node("A",2));
        nodeList.add(new Node("B",1));
        nodeList.add(new Node("C",1));
        nodeList.add(new Node("A",1));
        nodeList.add(new Node("D",1));
        nodeList.add(new Node("C",1));
        nodeList.add(new Node("A",1));
        nodeList.add(new Node("B",1));
        nodeList.add(new Node("A",1));
        Map<String, List<Node>> collect = nodeList.stream().collect(Collectors.groupingBy(t -> t.name));
        int count = 0;
        for (String k : collect.keySet()) {
            System.out.println(k);
            List<Node> nodes = collect.get(k);
            for (Node n : nodes) {
                n.id = count++;
                tc++;
                System.out.println(n.id + " " + n.name);
            }
        }
        System.out.println(".........");
        for (Node n : nodeList) {
            System.out.println(n.id + " " + n.name);
        }
        System.out.println(tc);
    }


    static void testVir() {
        // 检索虚拟机参数表
        VirtualType v = new VirtualType("A",1,1,1);
        v.id = "1";
        // 重新分配
        VirtualType virtualType = new VirtualType(v.type,v.cpuNum,v.memory,v.isDeployWithTowNode);


        virtualType.id = "2";
        System.out.println(v.id + " " + virtualType.id);
    }
    static class Node {
        String name;
        Integer id;

        public Node(String name, Integer id) {
            this.name = name;
            this.id = id;
        }
    }
}
