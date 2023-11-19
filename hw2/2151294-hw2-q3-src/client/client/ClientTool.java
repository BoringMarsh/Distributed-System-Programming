package edu.tongji.client;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * 一些客户端通用的值或函数
 */
public class ClientTool {
    public static final int PORT_MAP_UPLOAD = 9000;      //map服务器上传端口
    public static final int PORT_MAP_READ = 9001;        //map服务器读取端口
    public static final int PORT_MAP_DELETE = 9002;      //map服务器删除端口
    public static final int PORT_MAP_CHECK = 9003;       //map服务器检测端口
    public static final int PORT_MAP_NAMES = 9004;       //map服务器获取文件名端口
    public static final int PORT_BASE_UPLOAD = 10086;    //普通服务器上传端口起始（第i号服务器的上传端口就是该值+i）
    public static final int PORT_BASE_DOWNLOAD = 10000;  //普通服务器下载端口起始（第i号服务器的下载端口就是该值+i）
    public static final int PORT_BASE_DELETE = 12345;    //普通服务器删除端口起始（第i号服务器的删除端口就是该值+i）
    public static final int PORT_BASE_CHECK = 12306;     //普通服务器检测端口起始（第i号服务器的删除端口就是该值+i）
    public static final int SERVER_NUM = 3;  //服务器数量
    public static List<String> hosts = new ArrayList<>();  //服务器地址
    public static long fileSize = 0;  //文件大小，通常在上传等操作结束后改变值，用于计算速度
    public static final String CLIENT_PATH = "./client";              //客户端目录
    public static final String BLOCK_PATH = CLIENT_PATH + "/blocks";            //客户端分块文件目录
    public static final String DOWNLOAD_BASE_PATH = CLIENT_PATH + "/download";  //客户端下载目录
    public static final int BLOCK_SIZE = 0x100000;    //块大小（1MB）
    public static final int MAP_BUFFER_SIZE = 0x400;  //读取、传输映射文件的缓冲区大小
    public static final int BUFFER_SIZE = 0x40000;    //读取、传输块文件的缓冲区大小
    public enum MapFileStatus {
        noexist,
        part,
        complete,
        error  //客户端独有
    }  //映射文件状态的枚举类型

    /**
     * 禁止实例化
     */
    private ClientTool() {

    }

    /**
     * 格式化文件大小
     * @param size 以字节为单位的文件大小
     * @return 格式化后的字符串
     */
    public static String formatSize(final double size) {
        if (size >= 0 && size < 0x400)
            return size + "B";
        else if (size < 0x100000)
            return String.format("%.2f", (size / 0x400)) + "KB";
        else if (size < 0x40000000)
            return String.format("%.2f", (size / 0x100000)) + "MB";
        else
            return String.format("%.2f", (size / 0x40000000)) + "GB";
    }

    /**
     * 将.temp中的分块文件拼装成目标文件（get与cget专用）
     * @param blockFileTempDir 下载的块文件所在目录
     * @param originFile 拼接后的目标文件
     */
    public static void mergeBlock(final File blockFileTempDir, final File originFile) {
        System.out.println("拼装分块文件中...");
        final long startTime = System.currentTimeMillis();

        try {
            List<File> blockFiles = Arrays.asList(Objects.requireNonNull(blockFileTempDir.listFiles()));

            //blockFiles排序
            blockFiles.sort((b1, b2) -> {
                final int num1 = Integer.parseInt(b1.getName().split("\\.")[0]);
                final int num2 = Integer.parseInt(b2.getName().split("\\.")[0]);

                return Integer.compare(num1, num2);
            });

            BufferedOutputStream outToOriginFile = new BufferedOutputStream(new FileOutputStream(originFile), BUFFER_SIZE);
            BufferedInputStream inFromBlockFile;
            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;

            for (File file : blockFiles) {
                inFromBlockFile = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);

                while ((bytesRead = inFromBlockFile.read(bytes)) != -1) {
                    outToOriginFile.write(bytes, 0, bytesRead);
                }

                inFromBlockFile.close();
            }

            outToOriginFile.flush();
            outToOriginFile.close();
            fileSize = originFile.length();
            System.out.println("拼装用时：" + (System.currentTimeMillis() - startTime) + "ms");
        } catch (NullPointerException | IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * .temp文件夹及其内部的分块文件都是拼装工作的临时产物，需要清除
     * @param blockFileTempDir 下载的块文件所在目录
     */
    public static void deleteTemp(final File blockFileTempDir) {
        System.out.println("清除临时文件中...");
        final long startTime = System.currentTimeMillis();

        try {
            File[] blockFiles = Objects.requireNonNull(blockFileTempDir.listFiles());

            for (File file: blockFiles) {
                if (!file.delete())
                    System.out.println("块文件" + file.getName() + "删除失败！");
            }

            if (!blockFileTempDir.delete())
                System.out.println("存放块文件的临时文件夹删除失败！");

            System.out.println("清除用时：" + (System.currentTimeMillis() - startTime) + "ms");
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：读取映射文件，并返回文件存储状态 </p>
     */
    public static MapFileStatus readMapFileStatus(final String filename) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            DataInputStream inFromMapServerRead = new DataInputStream(mapServerReadSocket.getInputStream());

            outToMapServerRead.writeBoolean(true);    //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(false);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);   //发送是否获取断点
            outToMapServerRead.writeBoolean(false);   //发送是否返回映射文件内容（先不读）
            outToMapServerRead.writeUTF(filename);       //发送文件名

            final MapFileStatus status = MapFileStatus.values()[inFromMapServerRead.readInt()];  //读取文件存储状态

            outToMapServerRead.close();
            inFromMapServerRead.close();
            mapServerReadSocket.close();
            return status;
        } catch (IOException e) {
            e.printStackTrace();
            return MapFileStatus.error;
        }
    }

    /**
     * <p> 连接服务器：指定 </p>
     * <p> 端口：UPLOAD </p>
     * <p> 功能：将一个文件的某块上传到服务器上 </p>
     * @param ifContinue 是否为续传方式
     * @param serverNum 服务器号
     * @param filename 文件名
     * @param blockNum 块号
     */
    public static void uploadBlock(final boolean ifContinue, final int serverNum, final String filename, final int blockNum) {
        try {
            final String currentBlockFilename = ifContinue ? filename + ".blk" : filename + blockNum + ".blk";
            Socket normServerSocket = new Socket(hosts.get(serverNum), PORT_BASE_UPLOAD + serverNum);
            DataOutputStream outToNormServer = new DataOutputStream(normServerSocket.getOutputStream());
            BufferedInputStream inFromBlockFile = new BufferedInputStream(new FileInputStream(BLOCK_PATH + '/' + currentBlockFilename), BUFFER_SIZE);

            outToNormServer.writeBoolean(ifContinue);  //发送是否为续传方式
            outToNormServer.writeUTF(filename);        //发送文件名
            outToNormServer.writeInt(blockNum);        //发送块号

            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inFromBlockFile.read(bytes)) != -1) {
                outToNormServer.write(bytes, 0, bytesRead);
            }

            //关闭输出流，同时也标志着服务器端读到了流的尽头，read()方法返回-1，使得跳出读循环，不再被阻塞
            //弊端是关闭后，除非再次建立socket连接，否则无法再次传输数据
            if (ifContinue) {
                DataInputStream inFromNormServer = new DataInputStream(normServerSocket.getInputStream());
                normServerSocket.shutdownOutput();
                inFromNormServer.readBoolean();  //阻塞读，确认该块写入完成
                inFromBlockFile.close();
                normServerSocket.close();
            }

            inFromBlockFile.close();
            normServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：指定 </p>
     * <p> 端口：DOWNLOAD </p>
     * <p> 功能：去服务器上下载一个文件的某块 </p>
     * @param serverNum 服务器号
     * @param filename 文件名
     * @param blockNum 块号
     */
    public static void downloadBlock(final int serverNum, final String filename, final int blockNum) {
        try {
            Socket normServerSocket = new Socket(hosts.get(serverNum), PORT_BASE_DOWNLOAD + serverNum);
            DataOutputStream outToNormServer = new DataOutputStream(normServerSocket.getOutputStream());

            outToNormServer.writeUTF(filename);  //发送文件名
            outToNormServer.writeInt(blockNum);  //发送块号

            File blockFile = new File(DOWNLOAD_BASE_PATH + '/' + filename + ".temp" + '/' + blockNum + ".blk");  //下载下来的块文件
            BufferedInputStream inFromNormServer = new BufferedInputStream(normServerSocket.getInputStream(), BUFFER_SIZE);
            BufferedOutputStream outToBlockFile = new BufferedOutputStream(new FileOutputStream(blockFile), BUFFER_SIZE);
            byte[] bytesBlockFile = new byte[BUFFER_SIZE];
            int bytesReadBlockFile;

            while ((bytesReadBlockFile = inFromNormServer.read(bytesBlockFile)) != -1) {
                outToBlockFile.write(bytesBlockFile, 0, bytesReadBlockFile);
            }

            outToNormServer.close();
            inFromNormServer.close();
            outToBlockFile.close();
            normServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：指定 </p>
     * <p> 端口：DELETE </p>
     * <p> 功能：在服务器上删除一个文件的某块 </p>
     * @param serverNum 服务器号
     * @param filename 文件名
     * @param blockNum 块号
     */
    public static void deleteBlock(final int serverNum, final String filename, final int blockNum) {
        try {
            Socket normServerSocket = new Socket(hosts.get(serverNum), PORT_BASE_DELETE + serverNum);
            DataOutputStream outToNormServer = new DataOutputStream(normServerSocket.getOutputStream());
            outToNormServer.writeUTF(filename);  //发送文件名
            outToNormServer.writeInt(blockNum);  //发送块号
            outToNormServer.close();
            normServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：UPLOAD </p>
     * <p> 功能：将映射文件头写入map服务器上的映射文件 </p>
     * <p> 限制：续传方式 </p>
     * @param filename 文件名
     * @param blockCount 块数
     */
    public static void uploadMapFileHeader(final String filename, final int blockCount) {
        try {
            Socket mapServerUploadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_UPLOAD);
            DataOutputStream outToMapServerUpload = new DataOutputStream(mapServerUploadSocket.getOutputStream());
            outToMapServerUpload.writeBoolean(true);  //发送是否为续传方式
            outToMapServerUpload.writeUTF(filename);     //发送文件名
            outToMapServerUpload.writeInt(blockCount);   //发送块数
            outToMapServerUpload.close();
            mapServerUploadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：UPLOAD </p>
     * <p> 功能：将映射文件的一条内容写入map服务器上的映射文件 </p>
     * <p> 限制：续传方式 </p>
     * @param filename 文件名
     * @param blockNum 块号
     * @param serverNum 服务器号
     */
    public static void uploadMapFileContent(final String filename, final int blockNum, final int serverNum) {
        try {
            Socket mapServerUploadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_UPLOAD);
            DataOutputStream outToMapServerUpload = new DataOutputStream(mapServerUploadSocket.getOutputStream());
            outToMapServerUpload.writeBoolean(true);  //发送是否为续传方式
            outToMapServerUpload.writeUTF(filename);     //发送文件名
            outToMapServerUpload.writeInt(-1);        //发送块数（-1表示不写入文件头）
            outToMapServerUpload.writeInt(blockNum);     //发送块号
            outToMapServerUpload.writeInt(serverNum);    //发送服务器号
            outToMapServerUpload.close();
            mapServerUploadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：UPLOAD </p>
     * <p> 功能：将映射文件全部内容写入map服务器上的映射文件 </p>
     * <p> 限制：非续传方式 </p>
     * @param filename 文件名
     * @param blockCount 块数
     * @param blockServerMap 块号到所在服务器的映射
     */
    public static void uploadMapFileStream(final String filename, final int blockCount, final Map<Integer, Integer> blockServerMap) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        System.out.println("给各块分配服务器中...");
        final long startTime = System.currentTimeMillis();

        try {
            Socket mapServerUploadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_UPLOAD);
            DataOutputStream outToMapServerUpload = new DataOutputStream(mapServerUploadSocket.getOutputStream());
            outToMapServerUpload.writeBoolean(false);    //发送是否为续传方式
            outToMapServerUpload.writeUTF(filename);        //发送文件名
            outToMapServerUpload.writeInt(blockCount);      //发送块数

            for (int key: blockServerMap.keySet()) {
                buffer.putInt(key);                      //块号
                buffer.putInt(blockServerMap.get(key));  //服务器号

                if (buffer.position() == BUFFER_SIZE) {
                    outToMapServerUpload.write(buffer.array());
                }
            }

            byte[] bytes = new byte[buffer.position()];
            buffer.position(0);
            buffer.get(bytes);
            outToMapServerUpload.write(bytes);
            mapServerUploadSocket.shutdownOutput();  //确认传输完毕后，关闭使得服务器端不再阻塞读
            mapServerUploadSocket.close();
            System.out.println("分配用时：" + (System.currentTimeMillis() - startTime) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：读取map服务器上的映射文件，返回断点及剩余块数 </p>
     * <p> 限制：续传方式 </p>
     * @param filename 文件名
     * @return 断点
     */
    public static int readMapFileAndGetBreakPoint(final String filename) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            DataInputStream inFromMapServerRead = new DataInputStream(mapServerReadSocket.getInputStream());

            outToMapServerRead.writeBoolean(false);  //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(true);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(true);   //发送是否获取断点
            outToMapServerRead.writeBoolean(false);  //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);      //发送文件名

            final int fileBlockCount = inFromMapServerRead.readInt();  //读取文件总块数
            final int breakPoint = inFromMapServerRead.readInt();      //读取断点

            outToMapServerRead.close();
            inFromMapServerRead.close();

            mapServerReadSocket.close();
            fileSize = ((long) (fileBlockCount - breakPoint)) * BLOCK_SIZE;  //根据读取结果计算剩余上传大小
            return breakPoint;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：读取map服务器上的映射文件，返回总块数并计算续传大小 </p>
     * <p> 限制：续传方式 </p>
     * @param filename 文件名
     * @param breakPoint 断点
     * @return 文件总块数
     */
    public static int readMapFileAndGetBlockCount(final String filename, final int breakPoint) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            DataInputStream inFromMapServerRead = new DataInputStream(mapServerReadSocket.getInputStream());

            outToMapServerRead.writeBoolean(false);  //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(true);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);  //发送是否获取断点
            outToMapServerRead.writeBoolean(false);  //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);      //发送文件名

            final int fileBlockCount = inFromMapServerRead.readInt();  //读取文件总块数

            outToMapServerRead.close();
            inFromMapServerRead.close();

            mapServerReadSocket.close();
            fileSize = ((long) (fileBlockCount - breakPoint)) * BLOCK_SIZE;  //根据读取结果计算剩余下载大小
            return fileBlockCount;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：通过map服务器的READ端口读取映射文件，并在每一块所在服务器的DOWNLOAD端口下载块文件 </p>
     * <p> 限制：非续传方式 </p>
     * @param filename 文件名
     */
    public static void readMapFileAndDownloadBlocks(final String filename) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            DataInputStream inFromMapServerRead = new DataInputStream(mapServerReadSocket.getInputStream());

            outToMapServerRead.writeBoolean(false);   //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(false);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);   //发送是否获取断点
            outToMapServerRead.writeBoolean(true);    //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);       //发送文件名
            outToMapServerRead.writeBoolean(true);    //发送是否读取文件全部内容

            ByteBuffer buffer = ByteBuffer.allocate(MAP_BUFFER_SIZE);
            byte[] bytesMap = new byte[MAP_BUFFER_SIZE];
            int bytesReadMap;

            while ((bytesReadMap = inFromMapServerRead.read(bytesMap)) != -1) {
                buffer.put(bytesMap, 0, bytesReadMap);
                buffer.position(0);

                for (int i = 0; i < bytesReadMap >> 3; i++) {
                    final int blockNum = buffer.getInt();
                    final int serverNum = buffer.getInt();

                    downloadBlock(serverNum, filename, blockNum);  //根据读取记录去对应服务器上下载块
                }

                buffer.position(0);
            }

            outToMapServerRead.close();
            inFromMapServerRead.close();
            mapServerReadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：通过map服务器的READ端口读取映射文件，并在每一块所在服务器的DELETE端口删除块文件 </p>
     * @param filename 文件名
     */
    public static void readMapFileAndDeleteBlocks(final String filename) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            BufferedInputStream inFromMapServerRead = new BufferedInputStream(mapServerReadSocket.getInputStream(), MAP_BUFFER_SIZE);
            outToMapServerRead.writeBoolean(false);   //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(false);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);   //发送是否获取断点
            outToMapServerRead.writeBoolean(true);    //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);       //发送文件名
            outToMapServerRead.writeBoolean(true);    //发送是否读取映射文件全部内容

            ByteBuffer buffer = ByteBuffer.allocate(MAP_BUFFER_SIZE);
            byte[] bytes = new byte[MAP_BUFFER_SIZE];
            int bytesRead;

            //服务端返回时已经跳过映射文件的文件头，因此不用在此skip
            while ((bytesRead = inFromMapServerRead.read(bytes)) != -1) {
                buffer.put(bytes, 0, bytesRead);
                buffer.position(0);

                for (int i = 0; i < bytesRead >> 3; i++) {
                    final int blockNum = buffer.getInt();
                    final int serverNum = buffer.getInt();

                    deleteBlock(serverNum, filename, blockNum);  //根据读取记录去对应服务器上删除块
                }

                buffer.position(0);
            }

            outToMapServerRead.close();
            inFromMapServerRead.close();
            mapServerReadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：通过map服务器的READ端口读取映射文件，并在每一块所在服务器的DELETE端口删除块文件 </p>
     * <p> 限制：续传方式 </p>
     * @param filename 文件名
     * @param blockNum 块号
     * @return 该块所在服务器号
     */
    public static int readMapFileAndGetServerNum(final String filename, final int blockNum) {
        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            DataInputStream inFromMapServerRead = new DataInputStream(mapServerReadSocket.getInputStream());
            outToMapServerRead.writeBoolean(false);   //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(false);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);   //发送是否获取断点
            outToMapServerRead.writeBoolean(true);    //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);       //发送文件名
            outToMapServerRead.writeBoolean(false);   //发送是否读取映射文件全部内容
            outToMapServerRead.writeInt(blockNum);       //发送块号

            final int serverNum = inFromMapServerRead.readInt();  //读取某块对应的服务器号

            outToMapServerRead.close();
            inFromMapServerRead.close();
            mapServerReadSocket.close();
            return serverNum;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：DELETE </p>
     * <p> 功能：通过map服务器的DELETE端口删除映射文件 </p>
     * @return 删除是否成功
     */
    public static boolean deleteMapFile(final String filename) {
        boolean ret;

        try {
            Socket mapServerDeleteSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_DELETE);
            DataOutputStream outToMapServerDelete = new DataOutputStream(mapServerDeleteSocket.getOutputStream());
            DataInputStream inFromMapServerDelete = new DataInputStream(mapServerDeleteSocket.getInputStream());
            outToMapServerDelete.writeUTF(filename);    //发送文件名
            ret = inFromMapServerDelete.readBoolean();  //读取服务器回传信息，确保删除已完成
            outToMapServerDelete.close();
            inFromMapServerDelete.close();
            mapServerDeleteSocket.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * <p> 连接服务器：全部 </p>
     * <p> 端口：CHECK </p>
     * <p> 功能：检查与服务器的连接情况 </p>
     * <p> 限制：check </p>
     * @return 服务器状态是否全部正常
     */
    public static boolean checkConnection() {
        boolean ifServerGood = true;

        for (int serverNum = 0; serverNum <= SERVER_NUM; serverNum++) {
            try {
                final boolean ifMapServer = (serverNum == SERVER_NUM);
                Socket testSocket = new Socket(hosts.get(serverNum), ifMapServer ? PORT_MAP_CHECK : PORT_BASE_CHECK + serverNum);
                testSocket.close();
                System.out.println("服务器" + (serverNum == SERVER_NUM ? "map" : serverNum) + "连接成功");
            } catch (Exception e) {
                System.out.println("连接到服务器" + (serverNum == SERVER_NUM ? "map" : serverNum) + "时发生错误");
                ifServerGood = false;
            }
        }

        return ifServerGood;
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：NAMES </p>
     * <p> 功能：获取服务器上所有文件名 </p>
     * <p> 限制：check </p>
     * @return 服务器上所有文件名的列表
     */
    public static List<String> getFilenames() {
        List<String> names = new ArrayList<>();

        try {
            Socket mapServerNamesSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_NAMES);
            DataInputStream inFromMapServerNames = new DataInputStream(mapServerNamesSocket.getInputStream());

            if (inFromMapServerNames.readBoolean()) {  //读取服务器上是否有文件
                final int fileCount = inFromMapServerNames.readInt();  //读取文件总数
                System.out.println("服务器总文件数：" + fileCount);

                for (int i = 0; i < fileCount; i++) {
                    names.add(inFromMapServerNames.readUTF());  //读取文件名
                }
            }

            inFromMapServerNames.close();
            mapServerNamesSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return names;
    }

    /**
     * <p> 连接服务器：map </p>
     * <p> 端口：READ </p>
     * <p> 功能：获取某个文件的每一块到服务器号的映射 </p>
     * <p> 限制：check </p>
     * @param filename 文件名
     * @return 映射的字典
     */
    public static Map<Integer, Integer> readMapFileAndGetMapInfo(final String filename) {
        Map<Integer, Integer> blockServerMap = new HashMap<>();

        try {
            Socket mapServerReadSocket = new Socket(hosts.get(SERVER_NUM), PORT_MAP_READ);
            DataOutputStream outToMapServerRead = new DataOutputStream(mapServerReadSocket.getOutputStream());
            BufferedInputStream inFromMapServerRead = new BufferedInputStream(mapServerReadSocket.getInputStream(), BUFFER_SIZE);

            outToMapServerRead.writeBoolean(false);   //发送是否返回映射文件状态
            outToMapServerRead.writeBoolean(false);   //发送是否获取文件块数
            outToMapServerRead.writeBoolean(false);   //发送是否获取断点
            outToMapServerRead.writeBoolean(true);    //发送是否返回映射文件内容
            outToMapServerRead.writeUTF(filename);       //发送文件名
            outToMapServerRead.writeBoolean(true);    //发送是否读取文件全部内容

            ByteBuffer buffer = ByteBuffer.allocate(MAP_BUFFER_SIZE);
            byte[] bytesMap = new byte[MAP_BUFFER_SIZE];
            int bytesReadMap;

            while ((bytesReadMap = inFromMapServerRead.read(bytesMap)) != -1) {
                buffer.put(bytesMap, 0, bytesReadMap);
                buffer.position(0);

                for (int i = 0; i < bytesReadMap >> 3; i++) {
                    final int blockNum = buffer.getInt();
                    final int serverNum = buffer.getInt();
                    blockServerMap.put(blockNum, serverNum);  //将某块和所在的服务器号放入Map中
                }

                buffer.position(0);
            }

            outToMapServerRead.close();
            inFromMapServerRead.close();
            mapServerReadSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return blockServerMap;
    }
}
