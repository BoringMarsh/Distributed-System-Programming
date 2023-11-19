package edu.tongji.client;

import java.io.File;
import java.io.IOException;
import static edu.tongji.client.ClientTool.*;

public class ClientGet {
    private static String filename;        //文件名
    private static File blockFileTempDir;  //块文件临时目录
    private static File originFile;        //目标文件
    private static MapFileStatus mapFileStatus;  //映射文件状态

    /**
     * 禁止实例化
     */
    private ClientGet() {

    }

    /**
     * 初始化ClientGet，进行下载准备工作
     * @param fileName 文件名
     */
    public static void setClientGet(final String fileName) {
        filename = fileName;
        blockFileTempDir = new File(DOWNLOAD_BASE_PATH + '/' + filename + ".temp");
        originFile = new File(DOWNLOAD_BASE_PATH + '/' + filename);
    }

    /**
     * 检查本地有无同名文件存在，若存在则可能有重复下载的情况，删除该文件并重新创建
     * @return 是否可以继续
     */
    private static boolean checkOldOriginFile() {
        try {
            if (originFile.exists() && !originFile.delete()) {
                System.out.println("无法删除download文件夹中的原有文件！");
                return false;
            }
            else if (!originFile.createNewFile()) {
                System.out.println("原文件" + filename + "创建失败！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 读取映射文件状态，并从服务器下载块文件到块文件临时目录中
     * @return 下载用时（ms）
     */
    private static long downloadBlockFile() {
        System.out.println("开始下载分块文件...");
        final long startTime = System.currentTimeMillis();
        mapFileStatus = readMapFileStatus(filename);  //读取映射文件状态

        if (mapFileStatus != MapFileStatus.complete) {
            switch (mapFileStatus) {
                case noexist -> System.out.println("服务器未找到文件" + filename);
                case part -> System.out.println("文件" + filename + "不完整（尝试用cput指令继续上传？）");
                case error -> System.out.println("查询状态时发生错误！");
            }

            return 1;
        }

        try {
            readMapFileAndDownloadBlocks(filename);  //下载所有块
            final long receiveTimeElapse = System.currentTimeMillis() - startTime;
            System.out.println("下载用时：" + receiveTimeElapse + "ms");
            return receiveTimeElapse;
        } catch (Exception e) {  //任何异常都需停止
            e.printStackTrace();
            System.out.println("下载遇到了错误，已停止");
            return 1;
        }
    }

    /**
     * 下载主过程
     * @return 下载部分所用时间（用于计算下载速度）
     */
    public static long get() {
        if (!blockFileTempDir.exists() && !blockFileTempDir.mkdir()) {  //创建块文件临时目录
            System.out.println("存放块文件的临时文件夹创建失败！");
            return 1;
        }

        if (!checkOldOriginFile())  //检查本地有无重名文件
            return 1;

        final long receiveTimeElapse = downloadBlockFile();  //读取映射文件状态并下载

        if (mapFileStatus != MapFileStatus.complete) {
            if (!originFile.delete())
                System.out.println("原文件" + filename + "删除失败！");
            if (!blockFileTempDir.delete())
                System.out.println("存放块文件的临时文件夹删除失败！");

            return 1;
        }

        mergeBlock(blockFileTempDir, originFile);  //拼装块文件
        deleteTemp(blockFileTempDir);  //删除临时文件

        return receiveTimeElapse;
    }
}
