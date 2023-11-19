package edu.tongji;

import java.io.File;
import java.util.*;
import edu.tongji.client.*;
import static edu.tongji.client.ClientTool.*;

public class ClientApp {
    /**
     * 初始化客户端
     * @return 是否成功
     */
    private static boolean initialize() {
        for (int i = 0; i <= SERVER_NUM; i++) {  //添加服务器地址（最后一个是map服务器地址）
            hosts.add("100.80.100.58");
        }

        File clientDir = new File(CLIENT_PATH);  //客户端目录
        File blockDir = new File(BLOCK_PATH);    //客户端分块文件目录
        File downloadDir = new File(DOWNLOAD_BASE_PATH);  //客户端下载目录

        if (!clientDir.exists() && !clientDir.mkdir()) {  //创建客户端目录
            System.out.println("客户端目录创建失败，初始化已中止");
            return false;
        }

        if (!blockDir.exists() && !blockDir.mkdir()) {  //创建客户端分块文件目录
            System.out.println("临时块文件目录创建失败，初始化已中止");
            return false;
        }

        if (!downloadDir.exists() && !downloadDir.mkdir()) {  //创建客户端下载目录
            System.out.println("下载目录创建失败，初始化已中止");
            return false;
        }

        System.out.println("客户端初始化完成");
        return true;
    }

    /**
     * 客户端主程序
     * @param args 主函数所需参数
     */
    public static void main(String[] args) {
        if (!initialize())  //初始化
            return;

        System.out.println("Client App Running...");
        Scanner input = new Scanner(System.in);
        boolean ifLoop = true;

        while (ifLoop) {
            System.out.println("\n----------------------------------------------------");
            System.out.print("Your Command: ");
            List<String> sentence = Arrays.asList(input.nextLine().split(" "));  //用空格隔开并读取指令
            String command = sentence.get(0);
            String filename = "";

            if (sentence.size() > 1) {  //第二个参数为文件名
                filename = sentence.get(1);
            }

            switch (command) {
                case "put" -> {
                    if (Objects.equals(filename, "")) {
                        System.out.println("文件名不能为空");
                        continue;
                    }

                    ClientPut.setClientPut(filename);
                    final long sendTimeElapse = ClientPut.put();
                    System.out.println("上传速度：" + formatSize((double) fileSize / sendTimeElapse * 1024) + "/s");
                }
                case "get" -> {
                    if (Objects.equals(filename, "")) {
                        System.out.println("文件名不能为空");
                        continue;
                    }

                    ClientGet.setClientGet(filename);
                    final long receiveTimeElapse = ClientGet.get();
                    System.out.println("下载速度：" + formatSize((double) fileSize / receiveTimeElapse * 1024) + "/s");
                }
                case "cput" -> {
                    if (Objects.equals(filename, "")) {
                        System.out.println("文件名不能为空");
                        continue;
                    }

                    ClientCput.setClientCput(filename);
                    final long processTimeElapse = ClientCput.cput();
                    System.out.println("处理速度：" + formatSize((double) fileSize / processTimeElapse * 1024) + "/s");
                }
                case "cget" -> {
                    if (Objects.equals(filename, "")) {
                        System.out.println("文件名不能为空");
                        continue;
                    }

                    ClientCget.setClientCget(filename);
                    final long processTimeElapse = ClientCget.cget();
                    System.out.println("下载速度：" + formatSize((double) fileSize / processTimeElapse * 1024) + "/s");
                }
                case "check" -> {
                    ClientCheck.initClientCheck();

                    if (Objects.equals(filename, ""))
                        ClientCheck.check(new ArrayList<>());
                    else {
                        ClientCheck.check(sentence.subList(1, sentence.size()));
                    }
                }
                case "exit" -> ifLoop = false;
                default -> System.out.println("未知指令，请重试");
            }
        }
    }
}
