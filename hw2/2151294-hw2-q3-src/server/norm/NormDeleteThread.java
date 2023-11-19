package edu.tongji.norm;

import java.io.File;
import static edu.tongji.ServerApp.*;

public class NormDeleteThread implements Runnable {
    private final String filename;  //文件名
    private final int blockNum;     //块号
    private final int serverNum;    //服务器号

    /**
     * 创建一个提供一般服务器删除服务的对象
     * @param filename 文件名
     * @param blockNum 块号
     * @param serverNum 服务器号
     */
    public NormDeleteThread(final String filename, final int blockNum, final int serverNum) {
        this.filename = filename;
        this.blockNum = blockNum;
        this.serverNum = serverNum;
    }

    /**
     * 一般服务器删除主过程
     */
    @Override
    public void run() {
        //找到块文件
        File blockFile = new File(PATH_SERVER_NORM + serverNum + '/' + filename + '/' + blockNum + ".blk");

        //若存在则删除
        if (blockFile.exists() && !blockFile.delete()) {
            System.out.println("文件" + filename + "的第" + blockNum + "块删除失败！");
        }
    }
}
