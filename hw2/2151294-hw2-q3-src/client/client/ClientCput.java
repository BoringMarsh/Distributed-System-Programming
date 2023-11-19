package edu.tongji.client;

import java.io.*;
import java.util.Random;
import static edu.tongji.client.ClientTool.*;

public class ClientCput {
    private static String filename;  //文件名
    private static File originFile;  //原文件
    private static File blockFile;   //块文件（逐块上传时，暂存每一块信息）
    private static int fileBlockCount;  //文件总块数

    /**
     * 禁止实例化
     */
    private ClientCput() {

    }

    /**
     * 初始化ClientCput，进行下载准备工作
     * @param fileName 文件名
     */
    public static void setClientCput(final String fileName) {
        filename = fileName;
        originFile = new File(CLIENT_PATH + '/' + fileName);
        blockFile = new File(BLOCK_PATH + '/' + fileName + ".blk");
        fileBlockCount = (int) Math.ceil((double) originFile.length() / BLOCK_SIZE);
    }

    /**
     * 检查映射文件状态，若存在则删除原有文件。无论如何都新建映射文件并写入文件头
     * @return 映射文件状态
     */
    private static MapFileStatus checkMapFile() {
        long startTime = 0;
        MapFileStatus mapFileStatus = readMapFileStatus(filename);  //读取映射文件状态

        switch (mapFileStatus) {
            case noexist -> {
                System.out.println("文件未上传，准备从头传输...");
                uploadMapFileHeader(filename, fileBlockCount);  //连接到PORT_MAP_UPLOAD端口，续传且传块数，只写文件头
                return mapFileStatus;
            }
            case part -> {
                System.out.println("检测到有断点存在，准备续传...");
                return mapFileStatus;
            }
            case error -> {
                System.out.println("查询状态时发生错误！");
                return mapFileStatus;
            }
            case complete -> {
                System.out.println("文件完整上传，准备重新传输...");
                startTime = System.currentTimeMillis();
            }
        }

        readMapFileAndDeleteBlocks(filename);  //删除文件的所有块
        if (!deleteMapFile(filename)) {  //先删除现有映射文件
            System.out.println("映射文件删除失败！");
            return MapFileStatus.error;
        }
        uploadMapFileHeader(filename, fileBlockCount);  //删除成功后，相当于未上传，建立映射文件并写入文件头

        //原来deleteMapFile方法无返回值，调用后直接接上uploadMapFileHeader方法。但新的映射文件总是没有文件头。
        //有可能是DELETE端口工作量大，还没等到deleteMapFile执行完，新的文件头就已经写入了。这时新文件头会跟着旧文件一起删除
        //解决方法：
        //  ①DELETE端口回传（比如回传一个布尔值）
        //  ②客户端阻塞读回传的东西后，再结束deleteMapFile函数，确保映射文件删除完毕后再进行后续操作

        System.out.println("删除用时：" + (System.currentTimeMillis() - startTime) + "ms");
        return MapFileStatus.complete;
    }

    /**
     * 按每块进行如下操作：
     *   ①从原文件读取块数据到.blk文件
     *   ②给该块分配服务器
     *   ③将该块传输给服务器
     *   ④接到服务器回复后，将传输记录存入.mp文件
     * @param startPoint 断点
     * @return 处理总用时
     */
    private static long operatePerBlock(final int startPoint) {
        System.out.println("文件正在进行逐块处理、记录和传输...");
        Random dispatcher = new Random();
        final long startTime = System.currentTimeMillis();

        try {
            //创建唯一的临时blk文件
            if (!blockFile.exists() && !blockFile.createNewFile()) {
                System.out.println("临时blk文件创建失败！");
                return 1;
            }

            //缓冲区方式读取原文件，并从断点处读取
            BufferedInputStream inFromOriginFile = new BufferedInputStream(new FileInputStream(originFile), BUFFER_SIZE);
            inFromOriginFile.skipNBytes((long) startPoint * BLOCK_SIZE);
            //备用缓冲区及读取字节数
            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;

            for (int i = startPoint; i < fileBlockCount; i++) {
                //①从原文件读取块数据到.blk文件
                RandomAccessFile rBlockFile = new RandomAccessFile(blockFile, "rw");
                rBlockFile.setLength(0);
                rBlockFile.close();

                BufferedOutputStream outToBlockFile = new BufferedOutputStream(new FileOutputStream(blockFile), BUFFER_SIZE);

                while((bytesRead = inFromOriginFile.read(bytes)) != -1) {
                    outToBlockFile.write(bytes, 0, bytesRead);

                    if (blockFile.length() == BLOCK_SIZE)
                        break;
                }

                outToBlockFile.close();

                //②给该块分配服务器
                final int serverNum = dispatcher.nextInt(0, SERVER_NUM);

                //③将该块传输给服务器
                uploadBlock(true, serverNum, filename, i);

                //④接到服务器回复后，将传输记录存入映射文件
                uploadMapFileContent(filename, i, serverNum);
            }

            inFromOriginFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final long timeElapse = System.currentTimeMillis() - startTime;
        System.out.println("处理用时：" + timeElapse + "ms");
        return timeElapse;
    }

    /**
     * filename.blk块文件是分块工作的临时产物，需要清除
     */
    private static void deleteTemp() {
        System.out.println("清除临时文件中...");

        if (!blockFile.delete())
            System.out.println("临时块文件删除失败！");
    }

    /**
     * 续传方式上传主过程
     * @return 逐块处理并上传部分所用时间（用于计算上传速度）
     */
    public static long cput() {
        if (!originFile.exists()) {
            System.out.println("文件" + filename + "不存在，请将其放入client目录下");
            return 1;
        }

        fileSize = originFile.length();  //计算上传大小
        MapFileStatus status = checkMapFile();  //检查映射文件状态
        int startPoint = 0;

        switch (status) {
            case noexist, complete -> {}
            case part -> startPoint = readMapFileAndGetBreakPoint(filename);  //有断点时读取断点
            case error -> {
                return 1;
            }
        }

        //逐块完成相关工作
        final long timeElapse = operatePerBlock(startPoint);

        //传输完成后，删除所有临时文件
        deleteTemp();

        return timeElapse;
    }
}
