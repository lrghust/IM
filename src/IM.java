import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class IM {
    private static JFrame frame;
    private JButton button1;
    private JTextField textField1;
    private JTextArea textArea1;
    private JPanel panel1;

    private String strC2S;
    public Client client;

    public IM() {
        button1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                strC2S=textField1.getText();
                client.send(strC2S);
                textArea1.append("Client:"+strC2S+"\n");
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                client.send("quit");
                client.close();
            }
        });
    }

    public void run(){
        client=new Client();
        textArea1.append("连接到主机：" + client.ip + " ，端口号：" + client.port + "\n");
        client.connect();
        textArea1.append("远程主机地址：" + client.clientSoc.getRemoteSocketAddress()+"\n");
        while(true){
            String strS2C=client.receive();
            textArea1.append("Server:" + strS2C+"\n");
        }
    }


    public static void main(String[] args) {
        frame = new JFrame("IM");
        IM gui=new IM();
        frame.setContentPane(gui.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        gui.run();
    }
}
