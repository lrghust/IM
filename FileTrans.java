import java.io.*;
import java.util.Arrays;

public class FileTrans extends Thread{
    private Dialog dialog;
    private int dataLength=4994;
    private String sendIp;
    private int sendPort;
    private String sendPath;
    private FileSender fileSender;

    public int recvPort;
    private FileReceiver fileReceiver;
    public String recvPath;
    private RDT recvRDT;
    private ServerRDT serverRdt;

    public long curLen=0;
    public long totalBytes=0;
    public long totalTime=0;
    public FileTrans(String ip, int port, Dialog tDialog) {
        dialog=tDialog;
        sendIp=ip;
        sendPort=port;
        sendPath=tDialog.sendFilePath;
        fileSender=new FileSender(this);
    }//send

    public FileTrans(Dialog tDialog){
        dialog=tDialog;
        serverRdt=new ServerRDT();
        recvPort=serverRdt.port;
        fileReceiver=new FileReceiver(this);
    }//receive

    public void run(){
        String ip=sendIp;
        int port=sendPort;
        String path=sendPath;
        System.out.println(String.valueOf(sendPort));
        try {
            RDT rdt=new RDT(ip,port);
            while(true) {
                rdt.shake();
                if (rdt.isConnected()) break;
            }
            File file=new File(path);
            totalBytes=file.length();
            rdt.writeLine(String.valueOf(totalBytes)+"\n");
            FileInputStream fileIn=new FileInputStream(path);
            byte[] sendPacket=new byte[dataLength];
            curLen=0;
            int len;
            long initTime=System.currentTimeMillis();
            while((len=fileIn.read(sendPacket))!=-1){
                //System.out.printf("send:%d bytes\n",len);
                rdt.write(Arrays.copyOfRange(sendPacket,0,len));
                curLen+=len;
            }
            totalTime=(System.currentTimeMillis()-initTime)/1000;
            dialog.uiIm.showText("文件发送完毕，平均速度为"+String.valueOf(totalBytes/1024/totalTime)+"KB/s\n",dialog.dialogId);
            rdt.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void receive(){
        String path=recvPath;
        System.out.println(String.valueOf(recvPort));
        try{
            recvRDT=serverRdt.accept();
            String tmp=recvRDT.readLine();
            totalBytes=Long.parseLong(tmp.split("\n")[0]);//接收文件总长度
            curLen=0;
            byte[] recvPacket=new byte[dataLength];
            FileOutputStream out=new FileOutputStream(path);
            int len;
            long initTime=System.currentTimeMillis();
            while((len=recvRDT.read(recvPacket))!=-1){
                System.out.printf("receive:%d bytes\n",len);
                curLen+=len;
                out.write(Arrays.copyOfRange(recvPacket,0,len));
            }
            System.out.println("recvend");
            out.close();
            totalTime=(System.currentTimeMillis()-initTime)/1000;
            dialog.uiIm.showText("文件接收完毕，平均速度为"+String.valueOf(totalBytes/1024/totalTime)+"KB/s\n",dialog.dialogId);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
