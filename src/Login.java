import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Login {
    private JFrame frame;
    private JButton Button_log;
    private JButton Button_reg;
    private JPanel panel1;
    private JTextField textField_userName;
    private JButton Button_key;
    private JPasswordField passwordField1;

    private Client client;
    public String userName;

    public Login(Client tClient) {
        frame = new JFrame("IM");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        client=tClient;

        Button_log.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                userName= textField_userName.getText();
                String key=new String(passwordField1.getPassword());
                if(userName.isEmpty()||key.isEmpty())
                    showMessage("请输入用户名密码！");
                else {
                    client.send("LOGIN");
                    client.send(userName + " " + key + " " + String.valueOf(client.listenPort));
                }
            }
        });
        Button_reg.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String id= textField_userName.getText();
                String key=new String(passwordField1.getPassword());
                if(id.isEmpty()||key.isEmpty())
                    showMessage("请输入用户名密码！");
                else {
                    client.send("SIGNUP");
                    client.send(id + " " + key);
                }
            }
        });
        Button_key.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String id= textField_userName.getText();
                if(id.isEmpty())
                    showMessage("请输入用户名！");
                else {
                    client.send("FINDKEY");
                    client.send(id);
                }
            }
        });
    }
    public void showMessage(String str){
        JOptionPane.showMessageDialog(frame, str);
    }
    public void close(){
        frame.dispose();
    }
}
