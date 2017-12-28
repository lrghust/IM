import javax.swing.*;

public class ServerUI {
    private JTextArea textArea1;
    private JPanel panel1;
    private JList list1;
    private DefaultListModel defaultListModel;

    public ServerUI(){
        JFrame frame = new JFrame("ServerUI");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        defaultListModel=new DefaultListModel();

        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
    }

    public void showText(String str){
        textArea1.append(str);
        textArea1.setCaretPosition(textArea1.getText().length());
    }

    public void addUser(String id, String ip, int port){
        defaultListModel.addElement(id+" "+ip+" "+String.valueOf(port));
        list1.setModel(defaultListModel);
    }

    public void deleteUser(int index){
        defaultListModel.remove(index);
        list1.setModel(defaultListModel);
    }
}
