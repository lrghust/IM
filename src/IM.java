import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IM extends Thread{
    private static JFrame frame;
    private JButton button_send;
    private JTextField textField1;
    private JTextArea textArea1;
    private JPanel panel1;
    private JButton button_connect;
    private JTextField textField_ip;
    private JTextField textField_port;

    private static Client client;

    public IM(Client tClient) {
        frame = new JFrame("IM");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        client=tClient;

        button_connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String strIP=textField_ip.getText();
                String strPort=textField_port.getText();
                client.connect(strIP,Integer.parseInt(strPort));
            }
        });

    }

    public void showText(String str){
        textArea1.append(str);
        textArea1.setCaretPosition(textArea1.getText().length());
    }

}
