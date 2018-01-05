package cn.localhost01;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Program {

	private final static  long[] BIT_256=new long[]{256*256*256,256*256,256,1};

	/**
	 * Description: 为指定IP段生成IP列表<BR>
	 *
	 * @author ran.chunlin
	 * @date 2018-01-05 16:53
	 * @param ipRange ip段，格式应为：xxx.xxx.xxx.xxx yyy.yyy.yyy.yyy，中间为任意多个空格隔开
	 * @param separator 生成的ip之间的分隔符
	 * @param outText 要显示的控件
	 * @return java.lang.String
	 * @throws Exception
	 * @version 1.0
	 */
	public static void format(String ipRange,String separator,JTextArea outText) {
		ipRange = ipRange == null ? "" : ipRange.trim();

		//1.检查ip是否合乎规范
		if (!ipRange.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\ +\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))
			return;//IP段格式不正确！

		String[] ips = ipRange.split("\\ +");

		if (ips[0].compareTo(ips[1]) > 0)
			return;//起始IP应小于终止IP！

		//2.得到前后两个ip的数字数组
		String[] beforeIpNums_str = ips[0].split("\\.");
		String[] afterIpNums_str = ips[1].split("\\.");

		long beforeIPSum=0l,afterIPSum=0l;
		for(int i=0;i<4;i++){
			beforeIPSum+=Integer.parseInt(beforeIpNums_str[i])*BIT_256[i];
			afterIPSum+=Integer.parseInt(afterIpNums_str[i])*BIT_256[i];
		}

		//3.开始整理
		StringBuilder result=new StringBuilder();
		StringBuilder sb=new StringBuilder();
		for (long tmp=beforeIPSum;tmp<=afterIPSum;tmp++) {
			outText.append(
					(tmp / BIT_256[0]) % 256 + "." + (tmp / BIT_256[1]) % 256 + "." + (tmp / BIT_256[2]) % 256 + "."
							+ tmp % 256 + "\n");
			if (tmp%500==0) {
				outText.append(sb.toString());
				sb=new StringBuilder();
			}
		}
	}


	public static void main(String[] args) throws Exception {

		int width = 550, height = 500;
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		int screenW = (int) toolkit.getScreenSize().getWidth();

		final JFrame jFrame = new JFrame("IP段整理工具 By localhost01");

		final String defaultText = "1.1.1.1 1.1.255.255";
		final JTextArea ipText = new JTextArea(27,40);
		ipText.setText(defaultText);
		ipText.setFont(new Font("宋体", 0, 12));

		JButton jButton = new JButton("一键转换");
		jButton.setFont(new Font("宋体", 0, 12));

		final JTextArea outText = new JTextArea(27,40);
		outText.setFont(new Font("宋体", 0, 12));

		JLabel jLabel = new JLabel("Trip：为运行流畅，尽量将IP相差量保持在C、D段！");
		jLabel.setFont(new Font("宋体", 0, 12));

		ipText.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if ("".equals(ipText.getText()))
					ipText.setText(defaultText);
			}

			@Override
			public void focusGained(FocusEvent e) {
				if (defaultText.equals(ipText.getText()))
					ipText.setText("");
			}
		});

		jButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String[] ipRangeArray=ipText.getText().split("\n");

				for(String ipRange:ipRangeArray)
					format(ipRange,"\n",outText);
				JOptionPane.showMessageDialog(jFrame, "finished！");
			}
		});

		JPanel jPanel = new JPanel(new FlowLayout());
		jPanel.add(jLabel);
		jPanel.add(jButton);

		JPanel jPanel2 = new JPanel(new FlowLayout());
		jPanel2.add(new JScrollPane(ipText));
		jPanel2.add(new JScrollPane(outText));

		jFrame.setBounds((screenW - width) / 2, 150, width, height);
		jFrame.setLayout(new BorderLayout());
		jFrame.add(jPanel,BorderLayout.NORTH);
		jFrame.add(jPanel2,BorderLayout.CENTER);
		jFrame.setVisible(true);
		jFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
	}

}
