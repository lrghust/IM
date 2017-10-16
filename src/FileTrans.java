import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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
            Socket socket = new Socket(ip, port);
            File file=new File(path);
            long MByte=file.length()/(1024);
            BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(String.valueOf(MByte)+"\n");
            writer.flush();
            FileInputStream fileIn=new FileInputStream(path);
            byte[] sendPacket=new byte[1024];
            int num=0;
            while((fileIn.read(sendPacket))!=-1){
                DataOutputStream out=new DataOutputStream(socket.getOutputStream());
                out.write(sendPacket);
                out.flush();
                num++;
                fileSender.progressBar1.setValue((int)((double)num/MByte*100));
            }
        }catch (IOException e){
            if(!e.getMessage().equals("Connection refused (Connection refused)"))
                e.printStackTrace();
            return false;
        }
        return true;
    }

    private void receive(int port, String path){
        try{
            ServerSocket serverSoc=new ServerSocket(port);
            Socket socket=serverSoc.accept();
            long MByte;
            BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String tmp=reader.readLine();
            MByte=Long.parseLong(tmp);
            int num=0;
            byte[] recvPacket=new byte[1024];
            DataInputStream in=new DataInputStream(socket.getInputStream());
            FileOutputStream out=new FileOutputStream(path);
            while((in.read(recvPacket))!=-1){
                num++;
                out.write(recvPacket);
                fileReceiver.progressBar1.setValue((int)((double)num/MByte*100));
            }
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
