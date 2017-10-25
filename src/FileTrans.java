import java.io.*;
import java.util.Arrays;

public class FileTrans {
    private String sendIp;
    private int sendPort;
    private String sendPath;
    private FileSender fileSender;

    private int recvPort;
    private FileReceiver fileReceiver;
    public String recvPath;
    private RDT recvRDT;
    public FileTrans(String ip, int port, String tSendPath) {
        sendIp=ip;
        sendPort=port;
        sendPath=tSendPath;
        fileSender=new FileSender(this);
    }//send

    public FileTrans(int port){
        recvPort=port;
        fileReceiver=new FileReceiver(this);
    }//receive

    public boolean send(){
        String ip=sendIp;
        int port=sendPort;
        String path=sendPath;
        try {
            RDT rdt;
            while(true) {
                rdt = new RDT(ip, port);
                if (rdt.isConnected()) break;
            }
            File file=new File(path);
            long MByte=file.length()/(1024);
            //BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(rdt.getOutputStream()));
            //writer.write(String.valueOf(MByte)+"\n");
            //writer.flush();
            rdt.writeLine(String.valueOf(MByte)+"\n");
            FileInputStream fileIn=new FileInputStream(path);
            byte[] sendPacket=new byte[1024];
            int num=0;
            int len;
            long timeBegin=System.currentTimeMillis();
            int numBegin=0;
            while((len=fileIn.read(sendPacket))!=-1){
                //DataOutputStream out=new DataOutputStream(rdt.getOutputStream());
                rdt.write(Arrays.copyOfRange(sendPacket,0,len));
                //out.flush();
                num++;
                fileSender.progressBar1.setValue((int)((double)num/MByte*100));
                double sendTime=(System.currentTimeMillis()-timeBegin)/1000.;
                if(sendTime>0.5) {
                    fileSender.label_speed.setText(String.format("%.2f",(num-numBegin) / sendTime) + "KB/s");
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
        int port=recvPort;
        String path=recvPath;
        try{
            ServerRDT serverRdt=new ServerRDT(port);
            recvRDT=serverRdt.accept();
            long MByte;
            //BufferedReader reader=new BufferedReader(new InputStreamReader(rdt.getInputStream()));
            String tmp=recvRDT.readLine();
            MByte=Long.parseLong(tmp.split("\n")[0]);
            //MByte=200000;
            int num=0;
            byte[] recvPacket=new byte[1024];
            //DataInputStream in=new DataInputStream(rdt.getInputStream());
            FileOutputStream out=new FileOutputStream(path);
            int len;
            long timeBegin=System.currentTimeMillis();
            int numBegin=0;
            while((len=recvRDT.read(recvPacket))!=-1){
                num++;
                out.write(Arrays.copyOfRange(recvPacket,0,len));
                fileReceiver.progressBar1.setValue((int) ((double) num / MByte * 100));
                double recvTime=(System.currentTimeMillis()-timeBegin)/1000.;
                if(recvTime>0.5) {
                    fileReceiver.label_speed.setText(String.format("%.2f",(num-numBegin) / recvTime) + "KB/s");
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
