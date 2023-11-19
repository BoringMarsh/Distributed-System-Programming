package edu.tongji;

/**
 * 有向边类
 */
public class Edge {
    //起始url
    private final String src;
    //到达url
    private final String dst;

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

    public Edge (final String src, final String dst) {
        this.src = src;
        this.dst = dst;
    }
}
