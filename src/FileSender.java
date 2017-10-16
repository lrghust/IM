import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class FileSender {
    private JFrame frame;
    public JProgressBar progressBar1;
    private JPanel panel1;

    private FileTrans fileTrans;

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
