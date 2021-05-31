package cn.localhost01;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Program {

    public static void main(String[] args) throws Exception {

        int width = 550, height = 500;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        int screenW = (int) toolkit.getScreenSize().getWidth();

        final JFrame jFrame = new JFrame("IP段整理工具 By localhost01");

        final String defaultText = "[10.12.23.45 10.12.255.255] or [10.12.23.45/16]";
        final JTextArea ipText = new JTextArea(27, 40);
        ipText.setText(defaultText);
        ipText.setFont(new Font("宋体", 0, 12));

        JRadioButton jRadioButton = new JRadioButton("导出到文件");

        JButton jButton = new JButton("一键转换");
        jButton.setFont(new Font("宋体", 0, 12));

        final JTextArea outText = new JTextArea(27, 40);
        outText.setFont(new Font("宋体", 0, 12));

        JLabel jLabel = new JLabel("Trip：若处理内容较多，推荐选择“导出到文件”！ ");
        jLabel.setFont(new Font("宋体", 0, 12));

        ipText.addFocusListener(new FocusListener() {

            @Override public void focusLost(FocusEvent e) {
                if ("".equals(ipText.getText())) ipText.setText(defaultText);
            }

            @Override public void focusGained(FocusEvent e) {
                if (defaultText.equals(ipText.getText())) ipText.setText("");
            }
        });

        jButton.addActionListener((ActionEvent e) -> {

            if (jRadioButton.isSelected()) {
                file = new File("out.txt");
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException ignore) {
                        JOptionPane.showMessageDialog(jFrame, "创建文件失败！");
                        return;
                    }
                }
            }

            String[] ipRangeArray = ipText.getText().split("\n");

            List<Future<String[]>> futureList = new ArrayList<>();
            Future<String[]> future;
            for (String ipRange : ipRangeArray) {
                future = pool4IpArray.submit(new doIPArray(ipRange));
                futureList.add(future);
            }

            try {
                OutputStream ops = null;
                if (file != null) ops = new FileOutputStream(file);

                for (Future<String[]> ft : futureList) {
                    String[] batchResult = ft.get();
                    for (String result : batchResult) {
                        if (ops != null) ops.write(result.getBytes());
                        else outText.append(result);
                    }
                }
                if (ops != null) ops.close();
            } catch (IOException ignore) {//out put to file exception
            } catch (InterruptedException | ExecutionException ignore) {
                JOptionPane.showMessageDialog(jFrame, "程序运行错误！");
                return;
            }

            if (jRadioButton.isSelected()) outText.setText("---output to out.txt---");
            JOptionPane.showMessageDialog(jFrame, "完成！");
            Toolkit.getDefaultToolkit().beep();

        });

        JPanel jPanel = new JPanel(new FlowLayout());
        jPanel.add(jLabel);
        jPanel.add(jButton);
        jPanel.add(jRadioButton);

        JPanel jPanel2 = new JPanel(new FlowLayout());
        jPanel2.add(new JScrollPane(ipText));
        jPanel2.add(new JScrollPane(outText));

        jFrame.setBounds((screenW - width) / 2, 150, width, height);
        jFrame.setLayout(new BorderLayout());
        jFrame.add(jPanel, BorderLayout.NORTH);
        jFrame.add(jPanel2, BorderLayout.CENTER);
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
    }

    private final static long[] BIT_256 = new long[] {256 * 256 * 256, 256 * 256, 256, 1};

    private static File file;

    private static ExecutorService pool4IpArray = Executors.newFixedThreadPool(100);
    private static ExecutorService pool4SingleIp = Executors.newFixedThreadPool(50);

    /**
     * Description: 为指定IP段生成IP列表<BR>
     *
     * @param ipRange ip段，格式应为：xxx.xxx.xxx.xxx yyy.yyy.yyy.yyy，中间为任意多个空格隔开
     * @return String[] 防止生成的ip过多导致内存溢出，使用数组分批保存
     * @throws Exception
     * @author ran.chunlin
     * @date 2018/1/5 20:11
     * @version 1.0
     */
    private static String[] format(String ipRange) {
        //0.兼容ip+掩码格式
        ipRange = ipRange == null ? "" : ipRange.trim();
        if (ipRange.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d+")) {
            String[] ipAndMask = ipRange.split("/");
            ipRange = IpUtil.getBeginIpStr(ipAndMask[0], ipAndMask[1]) + " " + IpUtil
                    .getEndIpStr(ipAndMask[0], ipAndMask[1]);
        }

        //1.检查ip是否合乎规范
        if (!ipRange.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} +\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))
            return new String[0];//IP段格式不正确！

        String[] ips = ipRange.split(" +");

        //2.得到前后两个ip的数字数组
        String[] beforeIpNums_str = ips[0].split("\\.");
        String[] afterIpNums_str = ips[1].split("\\.");

        long beforeIPSum = 0, afterIPSum = 0;
        for (int i = 0; i < 4; i++) {
            beforeIPSum += Integer.parseInt(beforeIpNums_str[i]) * BIT_256[i];
            afterIPSum += Integer.parseInt(afterIpNums_str[i]) * BIT_256[i];
        }

        //3.开始整理
        List<Future<String>> futureList = new ArrayList<>();
        Future<String> future;

        int period = 255 * 255;
        int batch = (int) ((afterIPSum - beforeIPSum) / period + ((afterIPSum - beforeIPSum) % period == 0 ? 0 : 1));
        if (batch < 1) return new String[0];

        for (long i = 0; i < batch; i++) {
            future = pool4SingleIp.submit(new doSingleIP(beforeIPSum + i * period + 1,
                    Math.min(beforeIPSum + period * (i + 1), afterIPSum)));
            futureList.add(future);
        }

        String[] result = new String[batch];

        try {
            result[0] = ips[0] + "\n" + futureList.get(0).get();
            for (int i = 1; i < batch; i++)
                result[i] = futureList.get(i).get();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }

        return result;
    }

    private static class doIPArray implements Callable<String[]> {
        private String ipRange;

        private doIPArray(String ipRange) {
            this.ipRange = ipRange;
        }

        @Override public String[] call() {
            return format(ipRange);
        }
    }

    private static class doSingleIP implements Callable<String> {
        private long beforeIPSum, afterIPSum;

        private doSingleIP(long beforeIPSum, long afterIPSum) {
            this.beforeIPSum = beforeIPSum;
            this.afterIPSum = afterIPSum;
        }

        @Override public String call() {
            StringBuilder result = new StringBuilder();
            for (long tmp = beforeIPSum; tmp <= afterIPSum; tmp++) {
                result.append(
                        (tmp / BIT_256[0]) % 256 + "." + (tmp / BIT_256[1]) % 256 + "." + (tmp / BIT_256[2]) % 256 + "."
                                + tmp % 256 + "\n");
            }
            return result.toString();
        }
    }

}
