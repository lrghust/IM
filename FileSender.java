import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileSender {
    private JFrame frame;
    public JProgressBar progressBar1;
    private JPanel panel1;
    public JLabel label_speed;
    private Timer timer;

    private FileTrans fileTrans;
    private FileWriter file;
    double time=0.2;
    private long prevLen=0;

    public FileSender(FileTrans tFileTrans) {
        frame = new JFrame("FileSender");
        frame.setContentPane(this.panel1);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        progressBar1.setMinimum(0);
        progressBar1.setMaximum(100);
        progressBar1.setValue(0);
        progressBar1.setStringPainted(true);

        fileTrans=tFileTrans;
        try {
            file = new FileWriter("send.txt");
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

        progressBar1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(progressBar1.getValue()==100) {
                    frame.dispose();
                    try{
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
