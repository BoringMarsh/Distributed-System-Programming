package edu.tongji.map;

import java.io.*;
import java.net.Socket;
import static edu.tongji.ServerApp.*;

public class MapUploadThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final boolean ifContinue;       //是否为续传方式
    private final String filename;          //文件名
    private final int blockCount;           //文件总块数
    private int blockNum;                   //块号
    private int serverNum;                  //服务器号
    private final File mapFile;             //映射文件
    private static final int MAP_UPLOAD_BUFFER_SIZE = 0x400;  //传输映射文件信息时的缓冲区大小

    /**
     * 创建一个提供map服务器上传服务的对象
     * @param connectionSocket 当前连接的socket
     * @param ifContinue 是否为续传方式
     * @param filename 文件名
     * @param blockCount 文件总块数
     */
    public MapUploadThread(final Socket connectionSocket, final boolean ifContinue, final String filename, final int blockCount) {
        this.connectionSocket = connectionSocket;
        this.ifContinue = ifContinue;
        this.filename = filename;
        this.mapFile = new File(PATH_SERVER_MAP + '/' + filename + ".mp");
        this.blockCount = blockCount;

        //如果是续传方式，且本次不是写入块文件头，则继续读取块号和所在的服务器号
        try {
            if (ifContinue && blockCount == -1) {
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                this.blockNum = inFromClient.readInt();
                this.serverNum = inFromClient.readInt();
                inFromClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * map服务器上传主过程
     */
    @Override
    public void run() {
        try {
            //映射文件不存在时，创建新的映射文件
            if (!mapFile.exists() && !mapFile.createNewFile()) {
                System.out.println(filename + "的映射文件创建失败！");
                return;
            }

            RandomAccessFile rMapFile = new RandomAccessFile(mapFile, "rw");

            //①续传方式
            if (ifContinue) {
                rMapFile.seek(mapFile.length());  //从映射文件末尾写入

                if (blockCount != -1)  //若总块数不是-1，则写入文件头（即文件总块数）
                    rMapFile.writeInt(blockCount);
                else {  //若总块数是-1，则写入一条记录（块号和所在服务器号）
                    rMapFile.writeInt(blockNum);
                    rMapFile.writeInt(serverNum);
                }

                rMapFile.close();
            }
            //②非续传方式
            else {
                rMapFile.writeInt(blockCount);  //映射文件写入文件头
                rMapFile.close();

                //读取客户端发来的映射文件信息并全部写入
                BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream(), MAP_UPLOAD_BUFFER_SIZE);
                BufferedOutputStream outToMapFile = new BufferedOutputStream(new FileOutputStream(mapFile, true), MAP_UPLOAD_BUFFER_SIZE);
                byte[] bytes = new byte[MAP_UPLOAD_BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inFromClient.read(bytes)) != -1) {
                    outToMapFile.write(bytes, 0, bytesRead);
                }

                inFromClient.close();
                outToMapFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
