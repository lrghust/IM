import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;

public class FileReceiver {
    private JFrame frame;
    private JTextField textField_filepath;
    private JPanel panel1;
    public JProgressBar progressBar1;
    private JButton button_receive;
    private JButton button_choosefile;
    private JTextField textField_filename;
    public JLabel label_speed;

    private FileTrans fileTrans;


    public FileReceiver(FileTrans tFileTrans) {
        frame = new JFrame("FileReceiver");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        fileTrans=tFileTrans;
        progressBar1.setMinimum(0);
        progressBar1.setMaximum(100);
        progressBar1.setValue(0);
        progressBar1.setStringPainted(true);
        button_choosefile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc=new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
                jfc.showDialog(new JLabel(), "选择");
                String savePath=jfc.getSelectedFile().getPath();
                textField_filepath.setText(savePath);
            }
        });
        button_receive.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(textField_filepath.getText().isEmpty()||textField_filename.getText().isEmpty()){
                    showMessage("请输入接收路径与文件名！");
                    return;
                }
                fileTrans.recvPath=textField_filepath.getText()+"\\"+textField_filename.getText();
                Thread tReceive=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        fileTrans.receive();
                    }
                });
                tReceive.start();
            }
        });
        progressBar1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(progressBar1.getValue()==100)
                    frame.dispose();
            }
        });
    }

    public void showMessage(String str){
        JOptionPane.showMessageDialog(frame, str);
    }

}
