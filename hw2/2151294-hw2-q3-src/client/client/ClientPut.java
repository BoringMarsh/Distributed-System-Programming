package edu.tongji.client;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import static edu.tongji.client.ClientTool.*;

public class ClientPut {
    private static File originFile;     //原文件
    private static int fileBlockCount;  //文件总块数
    private static String filename;     //文件名
    private static Map<Integer, Integer> blockServerMap;  //块到所在服务器的映射信息

    /**
     * 禁止实例化
     */
    private ClientPut() {

    }

    /**
     * 初始化ClientPut，进行上传准备工作
     * @param fileName 文件名
     */
    public static void setClientPut(final String fileName) {
        originFile = new File(CLIENT_PATH + '/' + fileName);
        filename = fileName;
        blockServerMap = new HashMap<>();
    }

    /**
     * 检查映射文件状态，若存在则删除原有文件
     * @return 删除是否成功
     */
    private static boolean deleteOldFile() {
        long startTime;
        MapFileStatus mapFileStatus = readMapFileStatus(filename);  //读取映射文件状态

        switch (mapFileStatus) {
            case noexist -> {
                return true;
            }
            case error -> {
                System.out.println("查询映射文件状态时发生错误！");
                return false;
            }
            default -> {
                System.out.println("检测到服务器上有相同文件存在，删除中...");
                startTime = System.currentTimeMillis();
            }
        }

        readMapFileAndDeleteBlocks(filename);  //删除所有块
        if (!deleteMapFile(filename)) {  //删除原先的映射文件
            System.out.println("映射文件删除失败！");
            return false;
        }

        System.out.println("删除用时：" + (System.currentTimeMillis() - startTime) + "ms");
        return true;
    }

    /**
     * 将文件分块，放到多个.blk文件中
     */
    private static void fileBlocking() {
        fileSize = originFile.length();
        Random dispatcher = new Random();    //分配服务器的随机数生成器
        fileBlockCount = (int) Math.ceil((double) originFile.length() / BLOCK_SIZE);  //计算块数
        System.out.println("文件分块中...");
        final long startTime = System.currentTimeMillis();

        //使用缓冲区读方式打开原文件
        try (BufferedInputStream inFromOriginFile = new BufferedInputStream(new FileInputStream(originFile), BUFFER_SIZE)) {
            byte[] bytes = new byte[BUFFER_SIZE];  //读缓冲区

            for (int i = 0; i < fileBlockCount; i++) {
                File blockFile = new File(BLOCK_PATH + '/' + filename + i + ".blk");  //分块文件

                //存在则删除，不存在则创建
                if (blockFile.exists() && !blockFile.delete()) {
                    System.out.println(blockFile.getName() + "已存在，删除失败！");
                    return;
                }
                else if (!blockFile.createNewFile()) {
                    System.out.println(blockFile.getName() + "创建失败！");
                    return;
                }

                BufferedOutputStream outToBlockFile = new BufferedOutputStream(new FileOutputStream(blockFile), BUFFER_SIZE);  //缓冲区写方式打开分块文件
                blockServerMap.put(i, dispatcher.nextInt(0, SERVER_NUM));  //随机给该块分配服务器
                int bytesRead;  //读取字节数

                while ((bytesRead = inFromOriginFile.read(bytes)) != -1) {
                    outToBlockFile.write(bytes, 0, bytesRead);

                    if (blockFile.length() >= BLOCK_SIZE)
                        break;
                }

                outToBlockFile.close();
            }

            System.out.println("分块用时：" + (System.currentTimeMillis() - startTime) + "ms（" + fileBlockCount + "块）");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 按照分配将分块文件发到服务器
     * @return 传输时间（ms）
     */
    private static long sendToServer() {
        System.out.println("开始向服务器传输...");
        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < fileBlockCount; i++) {
            uploadBlock(false, blockServerMap.get(i), filename, i);  //将该块上传
            File delFile = new File(BLOCK_PATH + '/' + filename + i + ".blk");  //删除块文件

            if (!delFile.delete())
                System.out.println("分块文件" + delFile.getName() + "删除失败！");
        }

        final long timeElapse = System.currentTimeMillis() - startTime;
        System.out.println("传输用时：" + timeElapse + "ms");
        return timeElapse;
    }

    /**
     * 上传主过程
     * @return 上传部分所用时间（用于计算上传速度）
     */
    public static long put() {
        if (!originFile.exists()) {
            System.out.println("文件" + filename + "不存在，请将其放入client目录下！");
            return 1;
        }

        if (!deleteOldFile())  //若服务器上有该文件信息，删除原有文件
            return 1;

        fileBlocking();  //文件分块
        uploadMapFileStream(filename, fileBlockCount, blockServerMap);  //连接到映射文件服务器的UPLOAD端口，将分配信息写入mp文件中
        return sendToServer();  //将分块文件发到服务器
    }
}
