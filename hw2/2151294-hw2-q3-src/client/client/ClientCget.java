package edu.tongji.client;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import static edu.tongji.client.ClientTool.*;

public class ClientCget {
    private static String filename;        //文件名
    private static File blockFileTempDir;  //块文件临时目录
    private static File originFile;        //目标文件

    /**
     * 禁止实例化
     */
    private ClientCget() {

    }

    /**
     * 初始化ClientCget，进行下载准备工作
     * @param fileName 文件名
     */
    public static void setClientCget(final String fileName) {
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
     * 读取块文件临时目录并获取断点
     * @return 断点
     */
    private static int getBreakPoint() {
        System.out.println("检测到下载中的文件，获取断点中...");

        try {
            List<File> blockFiles = Arrays.asList(Objects.requireNonNull(blockFileTempDir.listFiles()));
            System.out.println("断点为：" + blockFiles.size());  //断点是最大块号+1，正好是文件数目，避免了排序
            return blockFiles.size();
        } catch (NullPointerException e) {
            System.out.println("断点获取时发生错误！");
            return -1;
        }
    }

    /**
     * 检查文件存储状态
     * @return 是否可以继续
     */
    private static boolean checkMapFileStatus() {
        MapFileStatus mapFileStatus = readMapFileStatus(filename);  //读取文件存储状态

        if (mapFileStatus != MapFileStatus.complete) {
            switch (mapFileStatus) {
                case noexist -> System.out.println("服务器未找到文件" + filename);
                case part -> System.out.println("文件" + filename + "不完整（尝试用cput指令继续上传？）");
                case error -> System.out.println("查询状态时发生错误！");
            }

            return false;
        }

        System.out.println("文件完整");
        return true;
    }

    /**
     * 续传方式下载主过程
     * @return 下载部分所用时间（用于计算下载速度）
     */
    public static long cget() {
        if (!checkMapFileStatus()) {  //检查文件存储状态
            System.out.println("服务器文件状态不正确，下载已停止！");
            return 1;
        }

        int breakPoint;

        if (blockFileTempDir.exists()) {  //若临时目录存在，则获取断点
            breakPoint = getBreakPoint();

            if (breakPoint == -1)
                return 1;
        }
        else {  //若不存在则创建临时目录
            System.out.println("未检测到下载中文件");
            breakPoint = 0;

            if (!blockFileTempDir.mkdir()) {
                System.out.println("存放块文件的临时文件夹创建失败！");
                return 1;
            }
        }

        if (!checkOldOriginFile())  //检查本地有无同名文件存在
            return 1;

        final int fileBlockCount = readMapFileAndGetBlockCount(filename, breakPoint);  //获取断点和总块数
        System.out.println("开始下载...");
        final long startTime = System.currentTimeMillis();

        //从断点开始，逐块下载
        for (int i = breakPoint; i < fileBlockCount; i++) {
            final int serverNum = readMapFileAndGetServerNum(filename, i);  //获取服务器号

            if (serverNum == -1) {
                System.out.println("获取第" + i + "块所在服务器信息时发生错误！");
                return 1;
            }

            downloadBlock(serverNum, filename, i);  //下载块
        }

        final long timeElapse = System.currentTimeMillis() - startTime;
        System.out.println("下载用时：" + timeElapse + "ms");
        mergeBlock(blockFileTempDir, originFile);  //拼装块文件
        deleteTemp(blockFileTempDir);  //删除临时文件
        return timeElapse;
    }
}
