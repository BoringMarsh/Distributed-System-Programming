package edu.tongji;

import java.util.HashMap;
import java.util.Map;

/**
 * 结点类
 */
public class Node {
    //在每一跳中的id
    private int colId;
    //所在跳数
    private final int skipCount;
    //横坐标
    private double x;
    //纵坐标
    private double y;
    //表示的url
    private final String url;
    //图形化半径
    public static final int RADIUS = 20;
    //url与Node对象的映射
    public static Map<String, Node> nodeMap = new HashMap<>();

    public int getColId() {
        return colId;
    }

    public void setColId(int colId) {
        this.colId = colId;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getUrl() {
        return url;
    }

    public Node(final int colId, final int skipCount, final String url) {
        this.url = url;
        this.skipCount = skipCount;
        this.colId = colId;
        nodeMap.put(url, this);
    }

    @Override
    public String toString() {
        return "Node{" +
                "colId=" + colId +
                ", skipCount=" + skipCount +
                ", x=" + x +
                ", y=" + y +
                ", url='" + url + '\'' +
                '}';
    }

}
