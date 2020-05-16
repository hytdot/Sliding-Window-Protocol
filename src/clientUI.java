import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class clientUI {
    public static void main(String[] args) {
        JFrame f = new JFrame("滑动窗口协议模拟_接收端");
        f.setBounds(800, 120, 600, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        f.getContentPane().add(panel, BorderLayout.NORTH);
        JButton button1 = new JButton("连接服务器");
        panel.add(button1);

        // 可滚动面板
        JScrollPane scrollPane = new JScrollPane();
        f.getContentPane().add(scrollPane, BorderLayout.CENTER);
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane.setViewportView(textArea);

        f.setVisible(true);

        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client client = new client();
                client.open(textArea);
            }
        });
    }
}
