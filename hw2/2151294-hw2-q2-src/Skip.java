package edu.tongji;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Skip {
    //起始地址
    public static final String SOURCE_SITE = "https://www.tongji.edu.cn";
    //存放html语句的html文件的对象
    public static final File DUMP_FILE = new File("./dump.html");
    //总跳数
    public static final int SKIP_NUM = 3;
    //每个url访问的外部url数
    public static final int EXTERNAL_URL_NUM = 6;
    //总经过的地址数
    public static int urlCount = 1;
    //跳转数
    public static int skipCount = 0;
    //域名匹配：若干字母、数字、下划线、短横线（非空）+一个点+后缀+若干单词
    private static final String PATTERN_TOP = "[\\w-]+\\.(com\\.cn|net\\.cn|gov\\.cn|org\\.nz|org\\.cn|com|net|org|gov|us|cc|biz|info|cn|co|edu)\\b*";
    //地址与入度的映射
    public static Map<String, Integer> inDegreeMap = new HashMap<>();
    //结点集合
    public static List<Node> nodes = new ArrayList<>();
    //边集合
    public static List<Edge> edges = new ArrayList<>();
    //访问记录
    public static Set<String> history = new HashSet<>();

    /**
     * 获取最大入度结点的url
     * @return 最大入度结点的url
     */
    public static String getMaxInDegreeURL() {
        int maxInDegree = -1;
        String maxKey = "";

        for (String key: inDegreeMap.keySet()) {
            final int value = inDegreeMap.get(key);
            if (value > maxInDegree) {
                maxInDegree = value;
                maxKey = key;
            }
        }

        return maxKey;
    }

    /**
     * 访问网站，获取html语句写入文件
     * @param url 地址
     * @throws IOException 文件IO出现问题
     */
    public static void fetchHTML(final String url) throws Exception {
        //保证存放html语句文件存在，并用写方式打开文件
        final boolean res = DUMP_FILE.createNewFile();
        FileWriter writer = new FileWriter(DUMP_FILE);

        //打开文件，尝试连接url并写入
        try {
            URI source = new URI(url);
            URL locator = source.toURL();  //取url
            URLConnection conn = locator.openConnection();  //开启连接
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));  //获得网页读取对象
            String htmlLine;

            while ((htmlLine = reader.readLine()) != null) {  //每读一行写入一行
                writer.write(htmlLine);
            }

            reader.close();  //读取资源关闭
        } catch (Exception e) {
            System.out.println(e.getMessage());
            //System.out.println("连接时发生错误：到" + url);
        }

        writer.close();  //写资源关闭
    }

    /**
     * 从字符串中提取url
     * @param string 输入的字符串
     * @return 提取的url列表
     */
    public static List<String> extractURLs(final String string) {
        //匹配任何有href属性的<a>标签，加入list1中
        Pattern p1 = Pattern.compile("<a(.*?)href=(.*?)>(.*?)</a>");
        Matcher m1 = p1.matcher(string);
        List<String> list1 = new ArrayList<>();
        while (m1.find()) {
            list1.add(m1.group());
        }

        //匹配任何<a>标签中的url部分，去除双引号后加入list2中
        Pattern p2 = Pattern.compile("\"http(.*?)\"");
        Matcher m2 = p2.matcher(list1.toString());
        List<String> list2 = new ArrayList<>();
        while (m2.find()) {
            list2.add(m2.group().replaceAll("^\"|\"$", ""));
        }

        return list2;
    }

    /**
     * 取一个网址的域名
     * @param url 网址
     * @return 网址的域名
     */
    public static String getDomain(final String url) {
        String ret = url;
        Pattern pattern = Pattern.compile(PATTERN_TOP, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            ret = matcher.group();
        }

        return ret;
    }

    /**
     * 从文件中提取外部url
     * @param url 地址
     * @return 外部url的列表
     * @throws IOException 文件IO出现问题
     */
    public static List<String> externalURLs(final String url) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(DUMP_FILE));  //获取读取对象
        StringBuilder builder = new StringBuilder();  //可变字符串，用于拼接从文件中读取出的内容
        String fileLine;

        while ((fileLine = reader.readLine()) != null) {  //每读一行，就把这行拼到builder上
            builder.append(fileLine).append("\n");
        }

        reader.close();  //读取资源关闭
        List<String> urls = extractURLs(builder.toString());  //提取url
        List<String> ret = new ArrayList<>();

        //遍历urls，将域名各不相同的链接放入列表并返回
        for (String link: urls) {
            final String originDomain = getDomain(url);  //初始域名
            final String linkDomain = getDomain(link);   //当前链接域名

            //若当前链接的域名与初始域名不相同，且未曾出现在返回列表中，则加入返回列表
            if (!Objects.equals(originDomain, linkDomain) && !ret.contains(link)) {
                ret.add(link);
            }
        }

        return ret;
    }

    /**
     * 递归访问外部url，共SKIP_NUM跳，每跳最多EXTERNAL_URL_NUM个地址
     * @param urls 外部的全部url
     * @param skipNum 跳数
     * @param startSite 本跳起始地址
     * @throws IOException 文件IO出现问题
     */
    public static void recursive(List<String> urls, final int skipNum, String startSite) throws Exception {
        //若跳数超过了最大跳数，则返回
        if (skipNum > SKIP_NUM)
            return;

        //当前网址已访问的外部url数
        int externalUrlCount = 0;

        for (String url: urls) {
            //若当前网址已访问的外部url数超过限制，则返回
            if (externalUrlCount >= EXTERNAL_URL_NUM)
                return;
            //若当前网址已访问过，则访问下一候选url
//            else if (history.contains(url))
//                continue;
            //否则访问过的外部url数自增
            else
                externalUrlCount++;

            //跳转计数，记录每一次跳转的信息并加入历史记录
            skipCount++;
            System.out.println(skipCount + ": " + startSite + " --> " + url);
            //history.add(url);

            //更新入度表
            //若入度表没有目的地的url，说明跳转到了一个新的地址，加入新键值对
            //根据跳数加入结点队列，以便后续绘图
            if (!inDegreeMap.containsKey(url)) {
                urlCount++;
                inDegreeMap.put(url, 1);
                nodes.add(new Node(nodes.size(), skipNum, url));
            }
            //若有目的地url，对应入度+1
            else
                inDegreeMap.put(url, inDegreeMap.get(url) + 1);

            edges.add(new Edge(startSite, url));  //添加边
            fetchHTML(url);  //提取目的地的html语句
            List<String> externalUrls = externalURLs(url);  //获得目的地的外部url

            recursive(externalUrls, skipNum + 1, url);  //递归访问
        }
    }

    public static void main(String[] args) throws Exception {
        //添加起始地址的结点
        nodes.add(new Node(0, 0, SOURCE_SITE));

        //计时开始
        final long startTime = System.currentTimeMillis();

        //提取起始地址的html语句，并获得起始地址的外部url
        fetchHTML(SOURCE_SITE);
        List<String> urls = externalURLs(SOURCE_SITE);

        //递归访问
        recursive(urls, 1, SOURCE_SITE);

        //计时结束，打印用时
        final double timeElapse = (System.currentTimeMillis() - startTime) / 1000.0;

        System.out.println("结点数：" + nodes.size());
        System.out.println("有向边数：" + edges.size());
        System.out.println("最大入度url：" + getMaxInDegreeURL());
        System.out.println("用时：" + timeElapse + "s");

        //删除存放html的文件
        final boolean res = DUMP_FILE.delete();

        //图形化显示
        DirectedGraphPanel.draw();
    }
}
