import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class FileTrans extends Thread {
    private String sendIp;
    private int sendPort;
    private String sendPath;
    public boolean sendFlag;
    private FileSender fileSender;

    private int recvPort;
    private FileReceiver fileReceiver;
    public boolean recvFlag;
    public String recvPath;
    public FileTrans(String ip, int port, String tSendPath) {
        sendIp=ip;
        sendPort=port;
        sendPath=tSendPath;
        fileSender=new FileSender(this);
        sendFlag=true;
        recvFlag=false;
    };//send

    public FileTrans(int port){
        recvPort=port;
        fileReceiver=new FileReceiver(this);
        recvFlag=false;
        sendFlag=false;
    };//receive

    public void run(){
        while (true){
            try {
                sleep(500);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            if(sendFlag){
                if(send(sendIp,sendPort,sendPath))
                    break;
            }
            if(recvFlag){
                receive(recvPort,recvPath);
                break;
            }
        }
    }

    private boolean send(String ip, int port, String path){
        try {
            RDT rdt = new RDT(ip, port);
            if(!rdt.isConnected()) return false;
            File file=new File(path);
            long MByte=file.length()/(1024);
            //BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(rdt.getOutputStream()));
            //writer.write(String.valueOf(MByte)+"\n");
            //writer.flush();
            rdt.writeLine(String.valueOf(MByte)+"\n");
            FileInputStream fileIn=new FileInputStream(path);
            byte[] sendPacket=new byte[1024];
            int num=0;
            int len=-1;
            while((len=fileIn.read(sendPacket))!=-1){
                //DataOutputStream out=new DataOutputStream(rdt.getOutputStream());
                rdt.write(Arrays.copyOfRange(sendPacket,0,len));
                //out.flush();
                num++;
                fileSender.progressBar1.setValue((int)((double)num/MByte*100));
            }
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void receive(int port, String path){
        try{
            ServerRDT serverRdt=new ServerRDT(port);
            RDT rdt=serverRdt.accept();
            long MByte;
            //BufferedReader reader=new BufferedReader(new InputStreamReader(rdt.getInputStream()));
            String tmp=rdt.readLine();
            MByte=Long.parseLong(tmp);
            int num=0;
            byte[] recvPacket=new byte[1024];
            //DataInputStream in=new DataInputStream(rdt.getInputStream());
            FileOutputStream out=new FileOutputStream(path);
            int len=-1;
            while((len=rdt.read(recvPacket))!=-1){
                num++;
                out.write(Arrays.copyOfRange(recvPacket,0,len));
                fileReceiver.progressBar1.setValue((int)((double)num/MByte*100));
            }
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
