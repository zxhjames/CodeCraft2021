package com.huawei.java.main;

/**
 * @program: SDK_java
 * @description:
 * @author: 占翔昊
 * @create 2021-03-10 15:33
 **/
// 请求数据
public class RequestData {
    String type;
    String id;

    public RequestData(String type, String id) {
        this.type = type;
        this.id = id;
    }

    @Override
    public String toString() {
        return "RequestData{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
