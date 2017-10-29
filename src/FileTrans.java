import java.io.*;
import java.util.Arrays;

public class FileTrans {
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
    public FileTrans(String ip, int port, String tSendPath) {
        sendIp=ip;
        sendPort=port;
        sendPath=tSendPath;
        fileSender=new FileSender(this);
    }//send

    public FileTrans(){
        serverRdt=new ServerRDT();
        recvPort=serverRdt.port;
        fileReceiver=new FileReceiver(this);
    }//receive

    public boolean send(){
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
            while((len=fileIn.read(sendPacket))!=-1){
                System.out.printf("send:%d bytes\n",len);
                rdt.write(Arrays.copyOfRange(sendPacket,0,len));
                curLen+=len;
            }
            rdt.close();
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void receive(){
        String path=recvPath;
        System.out.println(String.valueOf(recvPort));
        try{
            recvRDT=serverRdt.accept();
            String tmp=recvRDT.readLine();
            totalBytes=Long.parseLong(tmp.split("\n")[0]);
            curLen=0;
            byte[] recvPacket=new byte[dataLength];
            FileOutputStream out=new FileOutputStream(path);
            int len;
            while((len=recvRDT.read(recvPacket))!=-1){
                System.out.printf("receive:%d bytes\n",len);
                curLen+=len;
                out.write(Arrays.copyOfRange(recvPacket,0,len));
            }
            System.out.println("recvend");
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
