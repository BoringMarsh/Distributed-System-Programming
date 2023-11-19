package edu.tongji.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static edu.tongji.client.ClientTool.*;

public class ClientCheck {
    private static final Map<String, List<Integer>>[] serverMaps = new HashMap[SERVER_NUM];  //每个服务器上文件的存储映射信息

    /**
     * 禁止实例化
     */
    private ClientCheck() {

    }

    /**
     * 初始化ClientCheck，进行检测准备工作
     */
    public static void initClientCheck() {
        for (int i = 0; i < SERVER_NUM; i++) {
            serverMaps[i] = new HashMap<>();
        }
    }

    /**
     * 显示一个文件的各块的存储信息，并将其记录到serverMaps中
     * @param filename 文件名
     */
    private static void showFileInfo(final String filename) {
        System.out.println("\n************************************************************************************");
        System.out.println("文件" + filename + "存储信息：");
        System.out.println("************************************************************************************");

        //访问map服务器，获得每个文件映射文件内容，并解析成该文件每一块到服务器号的映射
        final Map<Integer, Integer> blockServerMap = readMapFileAndGetMapInfo(filename);
        final int blockCount = blockServerMap.keySet().size();
        List<Integer> blockList = new ArrayList<>();

        //为了ID从小至大，先得到map键的个数，再从0开始遍历
        for (int blockNum = 0; blockNum < blockCount; blockNum++) {
            final int serverNum = blockServerMap.get(blockNum);
            blockList.add(serverNum);
            //System.out.println("  块号：" + blockNum + " 所在服务器：" + serverNum);

            //将信息记录到serverMaps中
            serverMaps[serverNum].putIfAbsent(filename, new ArrayList<>());
            serverMaps[serverNum].get(filename).add(blockNum);
        }

        //每行输出10块的信息，方便查看
        final int lineCount = (int) Math.ceil((double) blockCount / 10);
        for (int i = 0; i < lineCount ; i++) {
            System.out.print(i == 0 ? "" : i);
            System.out.print(0);
            System.out.print('~');
            System.out.print(i == 0 ? "" : i);
            System.out.print((Math.min((i + 1) * 10, blockCount) - 1) % 10);
            System.out.print(": ");
            System.out.println(blockList.subList(i * 10, Math.min((i + 1) * 10, blockCount)));
        }
    }

    /**
     * 显示一个服务器上存了哪些文件的哪些块
     * @param serverNum 服务器号
     */
    private static void showServerInfo(final int serverNum) {
        System.out.println("\n------------------------------------------------------------------------------------");
        System.out.println("服务器" + serverNum + "存储信息：");
        System.out.println("------------------------------------------------------------------------------------");

        for (String filename: serverMaps[serverNum].keySet()) {
            System.out.println("文件" + filename + "：");

            //对每个文件，每行输出30个块号，方便查看
            final int blockCount = serverMaps[serverNum].get(filename).size();
            final int lineCount = (int) Math.ceil((double) blockCount / 30);
            for (int i = 0; i < lineCount ; i++) {
                System.out.println(serverMaps[serverNum].get(filename).subList(i * 30, Math.min((i + 1) * 30, blockCount)));
            }
        }
    }

    /**
     * 检测主过程
     */
    public static void check(final List<String> setFilenames) {
        //检查各服务器是否连通
        System.out.println("检查各服务器是否连通...");
        if (!checkConnection()) {
            System.out.println("检查已中止");
            return;
        }

        //访问map服务器，获得所有文件名
        final List<String> allFilenames = getFilenames();

        //若指定了文件名，则根据文件名进行信息查询
        if (setFilenames.size() != 0) {
            for (String filename: setFilenames) {
                if (allFilenames.contains(filename)) {
                    showFileInfo(filename);
                }
                else {
                    System.out.println("\n************************************************************************************");
                    System.out.println("文件" + filename + "不存在！");
                    System.out.println("************************************************************************************");
                }
            }
        }
        else {
            for (String filename: allFilenames) {
                showFileInfo(filename);
            }
        }

        //遍历serverMaps，打印出每个服务器上，各个文件块的从小至大ID序列
        for (int i = 0; i < SERVER_NUM; i++) {
            showServerInfo(i);
        }
    }
}
