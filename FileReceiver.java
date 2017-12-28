import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileReceiver {
    private JFrame frame;
    private JTextField textField_filepath;
    private JPanel panel1;
    public JProgressBar progressBar1;
    private JButton button_receive;
    private JButton button_choosefile;
    private JTextField textField_filename;
    public JLabel label_speed;
    private Timer timer;

    private FileTrans fileTrans;
    private FileWriter file;
    double time=0.2;
    private long prevLen;

    private boolean receiveLock=false;


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
        prevLen=0;

        try {
            file = new FileWriter("receive.txt");
        }catch (IOException e){
            e.printStackTrace();
        }

        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(progressBar1.getValue()==100)
                    return;
                if(fileTrans.totalBytes!=0) {
                    long recvBytes = fileTrans.curLen - prevLen;
                    progressBar1.setValue((int) (1. * fileTrans.curLen / fileTrans.totalBytes * 100));
                    label_speed.setText(String.format("%.2f", (5. * recvBytes / 1024)) + "KB/s");
                    try {
                        file.write(String.format("%.1f %.2f\n", time, (5. * recvBytes / 1024)));
                        time+=0.2;
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    prevLen = fileTrans.curLen;
                }
            }
        };
        timer=new Timer(200,taskPerformer);
        timer.start();
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
                if(receiveLock){
                    showMessage("不能重复接收！");
                    return;
                }
                if(textField_filepath.getText().isEmpty()||textField_filename.getText().isEmpty()){
                    showMessage("请输入接收路径与文件名！");
                    return;
                }
                fileTrans.recvPath=textField_filepath.getText()+File.separator+textField_filename.getText();
                Thread tReceive=new Thread(new Runnable() {
                    public void run() {
                        fileTrans.receive();
                    }
                });
                tReceive.start();
                receiveLock=true;
            }
        });
        progressBar1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(progressBar1.getValue()==100) {
                    frame.dispose();
                    try {
                        file.close();
                    }catch (IOException io){
                        io.printStackTrace();
                    }
                }
            }
        });
    }

    public void showMessage(String str){
        JOptionPane.showMessageDialog(frame, str);
    }

}
