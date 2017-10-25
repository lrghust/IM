import java.io.*;
import java.util.Arrays;

public class FileTrans {
    private int dataLength=4094;
    private String sendIp;
    private int sendPort;
    private String sendPath;
    private FileSender fileSender;

    public int recvPort;
    private FileReceiver fileReceiver;
    public String recvPath;
    private RDT recvRDT;
    private ServerRDT serverRdt;
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
            long MByte=file.length()/1024;
            //BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(rdt.getOutputStream()));
            //writer.write(String.valueOf(MByte)+"\n");
            //writer.flush();
            rdt.writeLine(String.valueOf(MByte)+"\n");
            FileInputStream fileIn=new FileInputStream(path);
            byte[] sendPacket=new byte[dataLength];
            int num=0;
            int len;
            long timeBegin=System.currentTimeMillis();
            int numBegin=0;
            while((len=fileIn.read(sendPacket))!=-1){
                //DataOutputStream out=new DataOutputStream(rdt.getOutputStream());
                rdt.write(Arrays.copyOfRange(sendPacket,0,len));
                //out.flush();
                num++;
                fileSender.progressBar1.setValue((int)Math.ceil(num*(1.*dataLength/1024)/MByte*100));
                double sendTime=(System.currentTimeMillis()-timeBegin)/1000.;
                if(sendTime>0.5) {
                    fileSender.label_speed.setText(String.format("%.2f",(num-numBegin)*(1.*dataLength/1024) / sendTime) + "KB/s");
                    timeBegin=System.currentTimeMillis();
                    numBegin=num;
                }
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
            long MByte;
            //BufferedReader reader=new BufferedReader(new InputStreamReader(rdt.getInputStream()));
            String tmp=recvRDT.readLine();
            MByte=Long.parseLong(tmp.split("\n")[0]);
            //MByte=200000;
            int num=0;
            byte[] recvPacket=new byte[dataLength];
            //DataInputStream in=new DataInputStream(rdt.getInputStream());
            FileOutputStream out=new FileOutputStream(path);
            int len;
            long timeBegin=System.currentTimeMillis();
            int numBegin=0;
            while((len=recvRDT.read(recvPacket))!=-1){
                num++;
                out.write(Arrays.copyOfRange(recvPacket,0,len));
                fileReceiver.progressBar1.setValue((int)Math.ceil(num*(1.*dataLength/1024) / MByte * 100));
                double recvTime=(System.currentTimeMillis()-timeBegin)/1000.;
                if(recvTime>0.5) {
                    fileReceiver.label_speed.setText(String.format("%.2f",(num-numBegin)*(1.*dataLength/1024) / recvTime) + "KB/s");
                    timeBegin = System.currentTimeMillis();
                    numBegin = num;
                }
            }
            System.out.println("recvend");
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
