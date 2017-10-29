import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class IM extends Thread{
    private static JFrame frame;
    private int tabid;
    private JTextArea textArea1;
    private JPanel panel1;
    private JButton button_connect;
    private JTextField textField_id;
    private JTabbedPane tabbedPane1;
    private JTextField textField1;
    private JButton button_send;
    private JButton button_close;
    private JTextField textField_filepath;
    private JButton button_choosefile;
    private JButton button_sendfile;

    private Client client;
    private ArrayList<Dialog> dialogList;
    private ArrayList<JTextArea> textAreaList;

    public IM(Client tClient) {
        frame = new JFrame("IM");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
        client=tClient;
        tabid=0;
        dialogList=new ArrayList<Dialog>();
        textAreaList=new ArrayList<JTextArea>();

        button_connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String strId=textField_id.getText();
                if(!client.connect(strId)) showMessage("用户不存在！");
            }
        });

        button_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String strC2S=textField1.getText();
                int curtab=tabbedPane1.getSelectedIndex();
                if(!dialogList.get(curtab).isClosed) {
                    dialogList.get(curtab).send("TEXT:" + strC2S);
                    showText("Me:" + strC2S + "\n", curtab);
                }
            }
        });

        button_close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int curtab=tabbedPane1.getSelectedIndex();
                if(curtab==-1){
                    showMessage("无用户连接！");
                }
                else {
                    dialogList.get(curtab).close();
                    for (int i = curtab + 1; i < dialogList.size(); i++) {
                        dialogList.get(i).dialogId--;
                    }
                    dialogList.remove(curtab);
                    tabbedPane1.removeTabAt(curtab);
                    textAreaList.remove(curtab);
                    tabid--;
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                client.serverSocWriter.println("CLOSE");
                client.serverSocWriter.flush();
            }
        });

        button_choosefile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc=new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES );
                jfc.showDialog(new JLabel(), "选择");
                String filePath=jfc.getSelectedFile().getPath();
                textField_filepath.setText(filePath);
            }
        });
        button_sendfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int curTabId=tabbedPane1.getSelectedIndex();
                if(curTabId==-1){
                    showMessage("无用户连接！");
                }
                else {
                    if (dialogList.get(curTabId).isOffline) {
                        showMessage("当前对方用户不在线！");
                        return;
                    }
                    String filePath = textField_filepath.getText();
                    if (filePath.isEmpty()) {
                        showMessage("请选择要发送的文件！");
                        return;
                    }
                    dialogList.get(curTabId).sendFilePath = filePath;
                    String[] splitPath = filePath.split("/");
                    String sendFileName = splitPath[splitPath.length - 1];
                    dialogList.get(curTabId).send("FILE:FILENAME " + sendFileName);
                }
            }
        });
    }

    public int addDialog(String remoteName, Dialog tDialog){
        JTextArea tabTextArea=new JTextArea(10,30);
        tabTextArea.setLineWrap(true);
        tabTextArea.setWrapStyleWord(true);
        JScrollPane tabPane=new JScrollPane(tabTextArea);
        tabbedPane1.addTab(remoteName,tabPane);
        dialogList.add(tDialog);
        textAreaList.add(tabTextArea);
        return tabid++;
    }

    public void showText(String str){
        textArea1.append(str);
        textArea1.setCaretPosition(textArea1.getText().length());
    }

    public void showText(String str, int tabid){
        JTextArea textarea=textAreaList.get(tabid);
        textarea.append(str);
        textarea.setCaretPosition(textarea.getText().length());
    }

    public void showMessage(String str){
        JOptionPane.showMessageDialog(frame, str);
    }

}
