package edu.tongji;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 画图类
 */
public class DirectedGraphPanel extends JPanel {
    private static Queue<Node>[] cols = new Queue[Skip.SKIP_NUM + 1];  //每列结点的队列

    /**
     * 初始化各队列，将各结点通过队列进行跳数的基数排序
     */
    private static void sortNode() {
        for (int i = 0; i < Skip.SKIP_NUM + 1; i++) {
            cols[i] = new LinkedList<>();  //每一列的队列初始化
        }

        for (Node node: Skip.nodes) {
            node.setColId(cols[node.getSkipCount()].size());
            cols[node.getSkipCount()].add(node);
        }

        for (int i = 0; i < Skip.SKIP_NUM + 1; i++) {
            for (Node node: cols[i]) {
                node.setX((node.getSkipCount() + 1) * 1400.0 / (Skip.SKIP_NUM + 2));
                node.setY((node.getColId() + 1) * 950.0 / (cols[i].size() + 1));
            }
        }
    }

    /**
     *
     * @param g2
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2) {
        int ARR_SIZE = 30; // 箭头大小
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int x = (int) (x2 - ARR_SIZE * Math.cos(angle));
        int y = (int) (y2 - ARR_SIZE * Math.sin(angle));

        Polygon arrow = new Polygon();
        arrow.addPoint(x2, y2);
        arrow.addPoint(x + 5, y + 5); // 通过调整这里的数字可以改变箭头的大小和形状
        arrow.addPoint(x + 5, y - 5);

        g2.fill(arrow);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        sortNode();
        Graphics2D g2 = (Graphics2D) g;

        // 绘制节点
        g2.setColor(new Color(25, 104, 131));
        for (int i = 0; i < Skip.SKIP_NUM + 1; i++) {
            while (!cols[i].isEmpty()) {
                final Node node = cols[i].remove();
                g2.fillOval((int) node.getX(), (int) node.getY(), Node.RADIUS, Node.RADIUS);
            }
        }

        // 绘制边
        g2.setColor(Color.BLACK);
        for (Edge edge: Skip.edges) {
            final Node src = Node.nodeMap.get(edge.getSrc());
            final Node dst = Node.nodeMap.get(edge.getDst());

            final float x1 = (float) src.getX() + Node.RADIUS / 2.0F;
            final float y1 = (float) src.getY() + Node.RADIUS / 2.0F;
            final float x2 = (float) dst.getX() + Node.RADIUS / 2.0F;
            final float y2 = (float) dst.getY() + Node.RADIUS / 2.0F;

            // 绘制线段
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1)); // 设置线条粗细
            g2.draw(new Line2D.Float(x1, y1, x2, y2));
            drawArrow(g2, (int) x1, (int) y1, (int) x2, (int) y2);

//            g2.drawLine(
//                    (int) src.getX() + Node.RADIUS / 2,
//                    (int) src.getY() + Node.RADIUS / 2,
//                    (int) dst.getX() + Node.RADIUS / 2,
//                    (int) dst.getY() + Node.RADIUS / 2
//            );
        }

        // 绘制节点标签
        g2.setColor(new Color(25, 104, 131));
        for (Node node: Skip.nodes) {
            final String label = node.getUrl().length() > 40 ? node.getUrl().substring(0, 37) + "..." : node.getUrl();
            g2.drawString(label, (int) node.getX(), (int) node.getY());
        }
    }

    public static void draw() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Directed Graph");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            DirectedGraphPanel panel = new DirectedGraphPanel();
            frame.getContentPane().add(panel);

            frame.setSize(1400, 1000);
            frame.setVisible(true);
        });
    }
}
