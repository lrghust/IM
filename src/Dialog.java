import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Dialog {
    private JTextArea textArea1;
    private JPanel panel1;
    private JTextField textField1;
    private JButton Button;
    private Client client;

    public Dialog(Client tClient) {
        JFrame frame = new JFrame("Dialog");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        client=tClient;
        Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String strC2S=textField1.getText();
                client.send("TEXT:"+strC2S);
                showText("Me:"+strC2S+"\n");
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                client.close();
            }
        });
    }

    public void showText(String str){
        textArea1.append(str);
        textArea1.setCaretPosition(textArea1.getText().length());
    }
}
