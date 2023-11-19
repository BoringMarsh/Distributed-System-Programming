package edu.tongji.map;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import static edu.tongji.ServerApp.*;

public class MapNamesThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final File mapFilesDir;         //所有映射文件所在目录

    /**
     * 创建一个提供map服务器获取文件名服务的对象
     * @param connectionSocket 当前连接的socket
     */
    public MapNamesThread(final Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
        this.mapFilesDir = new File(PATH_SERVER_MAP);
    }

    /**
     * map服务器获取文件名主过程
     */
    @Override
    public void run() {
        try {
            List<File> mapFiles = Arrays.asList(Objects.requireNonNull(mapFilesDir.listFiles()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            if (mapFiles.size() == 0) {  //若无映射文件，回传false
                outToClient.writeBoolean(false);
            }
            else {  //若有映射文件，则返回文件总数，然后逐个返回文件名
                outToClient.writeBoolean(true);
                outToClient.writeInt(mapFiles.size());

                for (File file: mapFiles) {
                    final String name = file.getName();
                    outToClient.writeUTF(name.substring(0, name.length() - ".mp".length()));  //文件名去掉.mp扩展名
                }
            }
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }
}
