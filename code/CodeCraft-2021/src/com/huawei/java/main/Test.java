package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 10:46
 **/
public class Test {
    public static void main(String[] args) {
        testSpliter();
    }

    // 字符串分隔
    static void testSpliter() {
        String str = "(host3NX39, 148, 322, 57061, 71)";
        System.out.println(ToolsBuilder.SplitServerTypeline(str).cpuNum);
    }
}
