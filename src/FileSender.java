import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class FileSender {
    private JFrame frame;
    public JProgressBar progressBar1;
    private JPanel panel1;
    public JLabel label_speed;
    private Timer timer;

    private FileTrans fileTrans;
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

        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(fileTrans.totalBytes!=0) {
                    long recvBytes = fileTrans.curLen - prevLen;
                    progressBar1.setValue((int) (1. * fileTrans.curLen / fileTrans.totalBytes * 100));
                    label_speed.setText(String.format("%.2f", (1. * recvBytes / 1024)) + "KB/s");
                    prevLen = fileTrans.curLen;
                }
            }
        };
        timer=new Timer(1000,taskPerformer);
        timer.start();

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
