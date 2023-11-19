package edu.tongji.map;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import static edu.tongji.ServerApp.*;

public class MapDeleteThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final String filename;          //文件名
    private final File mapFile;             //映射文件

    /**
     * 创建一个提供map服务器删除服务的对象
     * @param connectionSocket 当前连接的socket
     * @param filename 文件名
     */
    public MapDeleteThread(final Socket connectionSocket, final String filename) {
        this.connectionSocket = connectionSocket;
        this.filename = filename;
        this.mapFile = new File(PATH_SERVER_MAP + '/' + filename + ".mp");
    }

    /**
     * map服务器删除主过程
     */
    @Override
    public void run() {
        boolean ret = true;

        if (mapFile.exists() && !mapFile.delete()) {  //删除映射文件，并判断是否成功
            System.out.println(filename + "的映射文件删除失败！");
            ret = false;
        }

        try {
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            outToClient.writeBoolean(ret);  //回传布尔值，让客户端读，保证客户端在删除结束后进行后续操作
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
