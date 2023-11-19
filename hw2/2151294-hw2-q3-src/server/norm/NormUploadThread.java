package edu.tongji.norm;

import java.io.*;
import java.net.Socket;
import static edu.tongji.ServerApp.*;

public class NormUploadThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final boolean ifContinue;  //是否为续传方式
    private final String filename;     //文件名
    private final File serverDir;      //该文件在本服务器上的块文件目录
    private final File blockFile;      //服务器上的块文件
    private static final int UPLOAD_BUFFER_SIZE = 0x40000;  //传输块文件信息时的缓冲区大小

    /**
     * 创建一个提供一般服务器上传服务的对象
     * @param connectionSocket 当前连接的socket
     * @param ifContinue 是否为续传方式
     * @param filename 文件名
     * @param blockNum 块号
     * @param serverNum 服务器号
     */
    public NormUploadThread(final Socket connectionSocket, final boolean ifContinue, final String filename, final int blockNum, final int serverNum) {
        this.connectionSocket = connectionSocket;
        this.ifContinue = ifContinue;
        this.filename = filename;
        this.serverDir = new File(PATH_SERVER_NORM + serverNum + '/' + filename);
        this.blockFile = new File(PATH_SERVER_NORM + serverNum + "/" + filename +"/" + blockNum + ".blk");
    }

    /**
     * 一般服务器上传主过程
     */
    @Override
    public void run() {
        try {
            //该文件的块文件目录不存在时，创建新的目录
            if (!serverDir.exists() && !serverDir.mkdir()) {
                System.out.println("文件夹" + filename + "创建失败！");
                return;
            }

            //读取客户端上传的块文件信息并写入
            BufferedOutputStream outToBlockFile = new BufferedOutputStream(new FileOutputStream(blockFile), UPLOAD_BUFFER_SIZE);
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream(), UPLOAD_BUFFER_SIZE);
            byte[] bytes = new byte[UPLOAD_BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inFromClient.read(bytes)) != -1) {
                outToBlockFile.write(bytes, 0, bytesRead);
            }

            //续传方式时要确保本块传输完成后再传下一块，因此要回传信息给客户端，以便客户端继续上传
            if (ifContinue) {
                outToClient.writeBoolean(true);
            }

            inFromClient.close();
            outToClient.close();
            outToBlockFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
