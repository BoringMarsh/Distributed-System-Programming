package edu.tongji.map;

import java.io.*;
import java.net.Socket;
import static edu.tongji.ServerApp.*;

public class MapReadThread implements Runnable {
    private final Socket connectionSocket;  //当前连接的socket
    private final boolean ifCheck;          //是否返回映射文件状态
    private final boolean ifGetBlockCount;  //是否返回文件总块数
    private final boolean ifGetBreakPoint;  //是否返回断点
    private final boolean ifReadContent;    //是否返回映射文件内容
    private boolean ifStream;    //是否返回映射文件所有内容
    private int blockNum;        //续传方式时所需的块号
    private final File mapFile;  //映射文件
    private static final int MAP_DOWNLOAD_BUFFER_SIZE = 0x400;  //传输映射文件信息时的缓冲区大小

    /**
     * 创建一个提供map服务器读取服务的对象
     * @param connectionSocket 当前连接的socket
     * @param ifCheck 是否返回映射文件状态
     * @param ifGetBlockCount 是否返回文件总块数
     * @param ifGetBreakPoint 是否返回断点
     * @param ifReadContent 是否读取映射文件内容
     * @param filename 文件名
     */
    public MapReadThread(final Socket connectionSocket, final boolean ifCheck, final boolean ifGetBlockCount, final boolean ifGetBreakPoint, final boolean ifReadContent, final String filename) {
        this.connectionSocket = connectionSocket;
        this.ifCheck = ifCheck;
        this.ifGetBlockCount = ifGetBlockCount;
        this.ifGetBreakPoint = ifGetBreakPoint;
        this.ifReadContent = ifReadContent;
        this.mapFile = new File(PATH_SERVER_MAP + '/' + filename + ".mp");

        try {
            if (ifReadContent) {  //若要读取映射文件内容，则继续读取
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                this.ifStream = inFromClient.readBoolean();  //读取是否返回映射文件所有内容

                if (!ifStream)
                    this.blockNum = inFromClient.readInt();  //若不返回所有内容，则读取块号

                //所有以socket的I/O流建立的对象，都会随着socket关闭而释放，在确定传输结束之前绝对不要主动close!!!
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * map服务器读取主过程
     */
    @Override
    public void run() {
        try {
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            //①若ifCheck为真，则检测映射文件状态并返回（如果状态是noexist则后续不会返回任何数据）
            if (!mapFile.exists()) {
                if (ifCheck)
                    outToClient.writeInt(MapFileStatus.noexist.ordinal());

                return;
            }
            else if (ifCheck) {
                RandomAccessFile rMapFile = new RandomAccessFile(mapFile, "rw");
                final int fileBlockCount = rMapFile.readInt();

                //根据记录条数是否等于文件头中总块数来判断文件是否完整
                if ((mapFile.length() - Integer.BYTES) / Integer.BYTES / 2 != fileBlockCount)
                    outToClient.writeInt(MapFileStatus.part.ordinal());
                else
                    outToClient.writeInt(MapFileStatus.complete.ordinal());

                rMapFile.close();
            }

            //②若ifGetBlockCount为真，则返回文件总块数
            if (ifGetBlockCount) {
                RandomAccessFile rMapFile = new RandomAccessFile(mapFile, "r");
                final int fileBlockCount = rMapFile.readInt();  //读取总块数
                outToClient.writeInt(fileBlockCount);  //返回总块数
                rMapFile.close();
            }

            //③若ifGetBreakPoint为真，则返回文件上传断点
            if (ifGetBreakPoint) {
                RandomAccessFile rMapFile = new RandomAccessFile(mapFile, "r");
                rMapFile.skipBytes(Integer.BYTES);  //跳过映射文件头

                if (mapFile.length() > Integer.BYTES * 2) {
                    rMapFile.seek(mapFile.length() - Integer.BYTES * 2);  //倒数第二个int数就是最后一个传输成功的块号
                    final int ret = rMapFile.readInt();  //读取最后一个传输成功块的号码
                    outToClient.writeInt(ret + 1);       //返回断点
                }
                else {
                    outToClient.writeInt(0);  //长度不足，没有一块上传，断点为0
                }

                rMapFile.close();
            }

            //④若ifReadContent为真，则查看ifStream
            if (!ifReadContent)
                return;

            //⑤若ifStream为真，则跳过文件头返回映射文件所有信息
            if (ifStream) {
                BufferedInputStream inFromMapFile = new BufferedInputStream(new FileInputStream(mapFile), MAP_DOWNLOAD_BUFFER_SIZE);
                byte[] bytes = new byte[MAP_DOWNLOAD_BUFFER_SIZE];
                int bytesRead;

                inFromMapFile.skipNBytes(Integer.BYTES);  //跳过文件头
                while ((bytesRead = inFromMapFile.read(bytes)) != -1) {
                    outToClient.write(bytes, 0, bytesRead);
                }

                inFromMapFile.close();  //确认传输结束，关闭流
                connectionSocket.shutdownOutput();
            }
            //⑥若ifStream不为真，则以随机访问方式打开映射文件，并读取blockNum那一块对应的服务器信息并返回
            else {
                RandomAccessFile rMapFile = new RandomAccessFile(mapFile, "rw");
                rMapFile.seek(((blockNum + 1) * 2L) * Integer.BYTES);
                outToClient.writeInt(rMapFile.readInt());
                rMapFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
