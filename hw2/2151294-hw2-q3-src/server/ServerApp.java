package edu.tongji;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import edu.tongji.map.*;
import edu.tongji.norm.*;

public class ServerApp {
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
    public static final String PATH_SERVER_MAP = "./serverMap";  //map服务器目录
    public static final String PATH_SERVER_NORM = "./server";    //一般服务器目录前缀（后面加上服务器号组成完整路径）
    public enum MapFileStatus {
        noexist,
        part,
        complete
    }  //映射文件状态的枚举类型

    /**
     * 创建一般服务器上传端口的线程
     * @param serverNum 服务器号
     */
    private static void createNormUploadThread(final int serverNum) {
        try (ServerSocket serverSocket = new ServerSocket(PORT_BASE_UPLOAD + serverNum)) {
            while (true) {
                System.out.println("Upload Server " + serverNum + " Waiting on Port " + (10086 + serverNum) + "...");    //阻塞等待连接
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final boolean ifContinue = inFromClient.readBoolean();  //读取是否为续传方式
                final String filename = inFromClient.readUTF();  //读取文件名
                final int blockNum = inFromClient.readInt();     //读取块号
                System.out.println("上传文件" + filename + "的第" + blockNum + "块" + "，是否续传：" + ifContinue);
                new NormUploadThread(connectionSocket, ifContinue, filename, blockNum, serverNum).run();  //创建服务对象并运行主过程

                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一般服务器下载端口的线程
     * @param serverNum 服务器号
     */
    private static void createNormDownloadThread(final int serverNum) {
        try (ServerSocket serverSocket = new ServerSocket(PORT_BASE_DOWNLOAD + serverNum)) {
            while (true) {
                System.out.println("Download Server " + serverNum + " Waiting on Port " + (10000 + serverNum) +"...");    //阻塞等待连接
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final String filename = inFromClient.readUTF();  //读取文件名
                final int blockNum = inFromClient.readInt();     //读取块号
                System.out.println("下载文件" + filename + "的第" + blockNum + "块");
                new NormDownloadThread(connectionSocket, filename, blockNum, serverNum).run();  //创建服务对象并运行主过程

                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一般服务器删除端口的线程
     * @param serverNum 服务器号
     */
    private static void createNormDeleteThread(final int serverNum) {
        try (ServerSocket serverSocket = new ServerSocket(PORT_BASE_DELETE + serverNum)) {
            while (true) {
                System.out.println("Delete Server " + serverNum + " Waiting on Port " + (PORT_BASE_DELETE + serverNum) + "...");    //阻塞等待连接
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final String filename = inFromClient.readUTF();  //读取文件名
                final int blockNum = inFromClient.readInt();     //读取块号
                System.out.println("删除文件" + filename + "的第" + blockNum + "块");
                new NormDeleteThread(filename, blockNum, serverNum).run();  //创建服务对象并运行主过程

                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建一般服务器检测端口的线程
     * @param serverNum 服务器号
     */
    private static void createNormCheckThread(final int serverNum) {
        try (ServerSocket serverSocket = new ServerSocket(PORT_BASE_CHECK + serverNum)) {
            while (true) {
                System.out.println("Check Server " + serverNum + " Waiting on Port " + (PORT_BASE_CHECK + serverNum) + "...");    //阻塞等待连接
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建map服务器上传端口的线程
     */
    private static void createMapUploadThread() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_MAP_UPLOAD)) {
            while (true) {
                System.out.println("Map Server Upload Waiting on Port " + PORT_MAP_UPLOAD + "...");
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final boolean ifContinue = inFromClient.readBoolean();  //读取是否为续传方式
                final String filename = inFromClient.readUTF();         //读取文件名
                final int blockCount = inFromClient.readInt();          //读取文件总块数
                System.out.println("上传文件" + filename + "，总块数：" + blockCount + "，是否续传：" + ifContinue);

                new MapUploadThread(connectionSocket, ifContinue, filename, blockCount).run();  //创建服务对象并运行主过程
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建map服务器读取端口的线程
     */
    private static void createMapReadThread() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_MAP_READ)) {
            while (true) {
                System.out.println("Map Server Read Waiting on Port " + PORT_MAP_READ + "...");
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final boolean ifCheck = inFromClient.readBoolean();          //读取是否返回映射文件状态
                final boolean ifGetBlockCount = inFromClient.readBoolean();  //读取是否获取文件块数
                final boolean ifGetBreakPoint = inFromClient.readBoolean();  //读取是否获取断点
                final boolean ifReadContent = inFromClient.readBoolean();    //读取是否返回映射文件内容
                final String filename = inFromClient.readUTF();              //读取文件名
                System.out.println("读取" + filename + "的映射文件。" +
                        "是否返回映射文件状态：" + ifCheck +
                        "，是否返回断点" + ifGetBreakPoint +
                        "，是否获取文件信息" + ifReadContent
                );

                new MapReadThread(connectionSocket, ifCheck, ifGetBlockCount, ifGetBreakPoint, ifReadContent, filename).run();    //创建服务对象并运行主过程
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建map服务器删除端口的线程
     */
    private static void createMapDeleteThread() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_MAP_DELETE)) {
            while (true) {
                System.out.println("Map Server Delete Waiting on Port " + PORT_MAP_DELETE + "...");
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream());
                final String filename = inFromClient.readUTF();  //读取文件名
                System.out.println("删除文件" + filename);

                new MapDeleteThread(connectionSocket, filename).run();  //创建服务对象并运行主过程
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建map服务器检测端口的线程
     */
    private static void createMapCheckThread() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_MAP_CHECK)) {
            while (true) {
                System.out.println("Map Server Check Waiting on Port " + PORT_MAP_CHECK + "...");
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());  //连接成功
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建map服务器获取文件名端口的线程
     */
    private static void createMapNamesThread() {
        try (ServerSocket serverSocket = new ServerSocket(PORT_MAP_NAMES)) {
            while (true) {
                System.out.println("Map Server Names Waiting on Port " + PORT_MAP_NAMES + "...");
                Socket connectionSocket = serverSocket.accept();  //阻塞等待连接

                System.out.println("Welcome Connection From " + connectionSocket.getInetAddress());
                new MapNamesThread(connectionSocket).run();  //创建服务对象并运行主过程
                System.out.println(connectionSocket.getInetAddress() + " disconnected");  //断开连接，重新等待
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器端主程序（所有服务器在本机上）
     * @param args 主函数所需参数
     */
    public static void main(String[] args) {
        try (ExecutorService exec = Executors.newCachedThreadPool()) {  //开线程池，使得服务器多端口异步提供服务
            for (int i = 0; i < SERVER_NUM; i++) {
                final int serverNum = i;

                System.out.println("服务器初始化中...");

                //创建一般服务器目录
                File serverDir = new File(PATH_SERVER_NORM + i);
                if (!serverDir.exists() && !serverDir.mkdir()) {
                    System.out.println("服务器" + i + "目录创建失败，初始化已终止");
                    return;
                }

                exec.execute(() -> createNormUploadThread(serverNum));    //创建一般服务器上传端口的线程
                exec.execute(() -> createNormDownloadThread(serverNum));  //创建一般服务器下载端口的线程
                exec.execute(() -> createNormDeleteThread(serverNum));    //创建一般服务器删除端口的线程
                exec.execute(() -> createNormCheckThread(serverNum));     //创建一般服务器检测端口的线程
            }

            //创建map服务器目录
            File serverDir = new File(PATH_SERVER_MAP);
            if (!serverDir.exists() && !serverDir.mkdir()) {
                System.out.println("服务器map目录创建失败，初始化已终止");
                return;
            }

            exec.execute(ServerApp::createMapUploadThread);  //创建map服务器上传端口的线程
            exec.execute(ServerApp::createMapReadThread);    //创建map服务器读取端口的线程
            exec.execute(ServerApp::createMapDeleteThread);  //创建map服务器删除端口的线程
            exec.execute(ServerApp::createMapCheckThread);   //创建map服务器检测端口的线程
            exec.execute(ServerApp::createMapNamesThread);   //创建map服务器获取文件名端口的线程
        }
    }
}
