package edu.tongji.norm;

import java.io.*;
import java.net.Socket;

import static edu.tongji.ServerApp.*;

public class NormDownloadThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final String filename;  //文件名
    private final int blockNum;     //块号
    private final int serverNum;    //服务器号
    private static final int DOWNLOAD_BUFFER_SIZE = 0x40000;  //传输块文件信息时的缓冲区大小

    /**
     * 创建一个提供一般服务器下载服务的对象
     * @param connectionSocket 当前连接的socket
     * @param filename 文件名
     * @param blockNum 块号
     * @param serverNum 服务器号
     */
    public NormDownloadThread(final Socket connectionSocket, final String filename, final int blockNum, final int serverNum) {
        this.connectionSocket = connectionSocket;
        this.filename = filename;
        this.blockNum = blockNum;
        this.serverNum = serverNum;
    }

    /**
     * 一般服务器下载主过程
     */
    @Override
    public void run() {
        try {
            BufferedOutputStream outToClient = new BufferedOutputStream(connectionSocket.getOutputStream(), DOWNLOAD_BUFFER_SIZE);
            BufferedInputStream inFromBlockFile = new BufferedInputStream(new FileInputStream(PATH_SERVER_NORM + serverNum + '/' + filename + '/' + blockNum + ".blk"), DOWNLOAD_BUFFER_SIZE);
            byte[] bytes = new byte[DOWNLOAD_BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inFromBlockFile.read(bytes)) != -1) {
                outToClient.write(bytes, 0, bytesRead);
            }

            inFromBlockFile.close();

            //不关闭套接字，保证客户端完全接收数据。但要关闭写资源保证客户端不再阻塞读取
            outToClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
