import java.io.IOException;
import java.net.*;
import java.util.*;

public class RDT {
    private boolean isConnected;
    public DatagramSocket localSoc;
    private String remoteIp;
    public InetAddress remoteAddr;
    public int remotePort;

    public LinkedList<Packet> receiveBuf;
    public LinkedList<Packet> sendbuf;
    public LinkedList<PacTime> waitBuf;

    public int receiveWinBegin;
    public int sendWinBegin;
    private int sendIndex;

    private int timeout=10000;
    private int closeTimeOut=30000;

    public boolean isClose;

    public RDT(String ip, int port){
        try {
            localSoc = new DatagramSocket();
            remoteIp=ip;
            remotePort=port;
            isConnected = false;
            receiveBuf=new LinkedList<>();
            sendbuf=new LinkedList<>();
            waitBuf=new LinkedList<>();
            receiveWinBegin=0;
            sendWinBegin=0;
            sendIndex=0;
            isClose=false;
            shake();
        }catch (SocketException e){
            e.printStackTrace();
        }
    }

    public RDT(String ip, int port, boolean connected){
        isConnected=connected;
        try {
            localSoc = new DatagramSocket();
            remoteIp=ip;
            remotePort=port;
            receiveBuf=new LinkedList<>();
            sendbuf=new LinkedList<>();
            waitBuf=new LinkedList<>();
            receiveWinBegin=0;
            sendWinBegin=0;
            sendIndex=0;
            isClose=false;
            shakeACK();
        }catch (SocketException e){
            e.printStackTrace();
        }
    }

    public boolean isConnected(){return isConnected;}

    private void shake(){
        try {
            //send shake packet
            Packet packet = new Packet();
            packet.setShake();
            packet.setCheckSum();
            remoteAddr = InetAddress.getByName(remoteIp);
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
            localSoc.send(udpPacket);
            //receive shake ACK
            byte[] recvBytes=new byte[1030];
            DatagramPacket recvPacket = new DatagramPacket(recvBytes,recvBytes.length);
            localSoc.setSoTimeout(500);
            try {
                localSoc.receive(recvPacket);
            }catch (SocketTimeoutException ste){
                return;
            }
            localSoc.setSoTimeout(0);
            packet=new Packet(recvPacket.getData());
            if(packet.isACK()){
                isConnected=true;
                Thread rp=new ReceivePacket(this);
                rp.start();
                Thread sp=new SendPacket(this);
                sp.start();
            }
            //TODO else
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void shakeACK(){
        try {
            Packet packet = new Packet();
            packet.setACK((byte)0);
            packet.setCheckSum();
            remoteAddr = InetAddress.getByName(remoteIp);
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
            localSoc.send(udpPacket);
            Thread rp=new ReceivePacket(this);
            rp.start();
            Thread sp=new SendPacket(this);
            sp.start();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    public void writeLine(String data){
        write(data.getBytes());
    }

    public void write(byte[] data){
        int totalLen=data.length;
        int curLen=0;
        while(totalLen>1024){
            Packet packet=new Packet();
            packet.setIndex((byte)sendIndex++);
            sendIndex%=128;
            packet.setData(Arrays.copyOfRange(data,curLen,curLen+1024));
            packet.setCheckSum();
            sendbuf.offer(packet);
            totalLen-=1024;
            curLen+=1024;
        }
        Packet packet=new Packet();
        packet.setIndex((byte)sendIndex++);
        sendIndex%=128;
        packet.setData(Arrays.copyOfRange(data,curLen,curLen+totalLen));
        packet.setCheckSum();
        sendbuf.offer(packet);
    }

    public String readLine(){
        StringBuilder line=new StringBuilder();
        while(true){
            if(receiveBuf.isEmpty()){
                try {
                    Thread.sleep(timeout);
                    if(receiveBuf.isEmpty())
                        return null;
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            Packet packet=receiveBuf.poll();
            int index=packet.getIndex();
            if(index-receiveWinBegin>63 || index<receiveWinBegin){
                byte[] data=packet.getData();
                line.append(new String(data));
                if(data[data.length-1]==0xa){
                    return line.toString();
                }
            }
            else {
                receiveBuf.addFirst(packet);
                try {
                    Thread.sleep(10);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public int read(byte[] data){
        int needLen=data.length;
        int curLen=0;
        while(true){
            if(receiveBuf.isEmpty()){
                try {
                    Thread.sleep(timeout);
                    if(receiveBuf.isEmpty())
                        return -1;
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            Packet packet=receiveBuf.poll();
            int index=packet.getIndex();
            if(index-receiveWinBegin>63 || index<receiveWinBegin){
                int pacLen=packet.length();
                if(curLen+pacLen>needLen){
                    receiveBuf.addFirst(packet);
                    return curLen;
                }
                System.arraycopy(packet.getData(),0,data,curLen,pacLen);
                curLen+=pacLen;
                if(curLen==needLen)
                    return curLen;
            }
            else {
                receiveBuf.addFirst(packet);
                try {
                    Thread.sleep(10);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void close(){
        try {
            //send FIN
            Packet packet = new Packet();
            packet.setFIN();
            packet.setCheckSum();
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
            localSoc.send(udpPacket);
            //wait ACK
            localSoc.receive(udpPacket);
            packet=new Packet(udpPacket.getData());
            if(packet.isACK()){
                //wait FIN
                localSoc.receive(udpPacket);
                packet=new Packet(udpPacket.getData());
                if(packet.isFIN()){
                    //send ACK
                    packet = new Packet();
                    packet.setACK((byte)0);
                    packet.setCheckSum();
                    udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
                    localSoc.send(udpPacket);
                    //wait timeout
                    Thread.sleep(closeTimeOut);
                    //close
                    localSoc.close();
                    isClose=true;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException ine){
            ine.printStackTrace();
        }
    }
}

class SendPacket extends Thread{
    public RDT rdt;
    private LinkedList<Packet> sendBuf;
    public LinkedList<PacTime> waitBuf; //resend
    private int sendWinBegin;
    SendPacket(RDT tRdt){
        rdt=tRdt;
        sendBuf=rdt.sendbuf;
        waitBuf=rdt.waitBuf;
        sendWinBegin=rdt.sendWinBegin;
        Thread timer=new Timer(this);
        timer.start();
    }
    public void run(){
        while(true){
            if(rdt.isClose) return;
            if(sendBuf.isEmpty()){
                try {
                    sleep(10);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                continue;
            }

            if(sendBuf.getFirst().getIndex()<=sendWinBegin+63){
                try {
                    Packet packet = sendBuf.poll();
                    PacTime pacTime=new PacTime(packet);
                    DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                            rdt.remoteAddr, rdt.remotePort);
                    rdt.localSoc.send(udpPacket);
                    pacTime.time=System.currentTimeMillis();
                    waitBuf.offer(pacTime);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            else{
                try {
                    sleep(10);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}

class ReceivePacket extends Thread{
    private RDT rdt;
    private LinkedList<Packet> receiveBuf;
    private int receiveWinBegin;
    ReceivePacket(RDT tRdt){
        rdt=tRdt;
        receiveBuf=rdt.receiveBuf;
        receiveWinBegin=rdt.receiveWinBegin;
    }

    public void run(){
        while(true){
            if(rdt.isClose) return;
            try {
                byte[] recvBytes = new byte[1030];
                DatagramPacket recvPacket = new DatagramPacket(recvBytes, recvBytes.length);
                rdt.localSoc.receive(recvPacket);
                Packet packet = new Packet(recvPacket.getData());
                if(packet.checkSum()){
                    //fin
                    if(packet.isFIN()){
                        //send ACK
                        Packet finPacket=new Packet();
                        packet.setACK((byte)0);
                        packet.setCheckSum();
                        DatagramPacket finUdpPacket=new DatagramPacket(packet.getBytes(),packet.getBytes().length,rdt.remoteAddr,rdt.remotePort);
                        rdt.localSoc.send(finUdpPacket);
                        //send FIN
                        packet=new Packet();
                        packet.setFIN();
                        packet.setCheckSum();
                        finUdpPacket=new DatagramPacket(packet.getBytes(),packet.getBytes().length,rdt.remoteAddr,rdt.remotePort);
                        rdt.localSoc.send(finUdpPacket);
                        //receive ack
                        rdt.localSoc.receive(finUdpPacket);
                        packet=new Packet(finPacket.getBytes());
                        if(packet.isACK()){
                            rdt.localSoc.close();
                            rdt.isClose=true;
                        }
                    }
                    //ack
                    if(packet.isACK()){
                        int ackIndex=packet.getAck();
                        if(ackIndex==rdt.sendWinBegin){
                            rdt.sendWinBegin++;
                            rdt.sendWinBegin%=128;
                            rdt.waitBuf.removeIf(p->p.packet.getIndex()==ackIndex);
                            //todo fast resend
                        }
                        continue;
                    }
                    //data packet
                    if(packet.getIndex()>=receiveWinBegin) {
                        //in queue with order
                        inQueue(packet);
                    }
                    if(packet.getIndex()==receiveWinBegin) {
                        receiveWinBegin++;
                        receiveWinBegin%=128;
                    }
                    //send ack
                    byte ackIndex=packet.getIndex();
                    packet=new Packet();
                    packet.setACK(ackIndex);
                    packet.setCheckSum();
                    DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                                                                  rdt.remoteAddr, rdt.remotePort);
                    rdt.localSoc.send(udpPacket);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    private void inQueue(Packet packet){
        int index=packet.getIndex();
        ListIterator<Packet> iter=receiveBuf.listIterator(receiveBuf.size());
        while(iter.hasPrevious()){
            int prevIndex=iter.previous().getIndex();
            if(index>prevIndex || prevIndex-index>63){
                receiveBuf.add(iter.nextIndex()+1,packet);
                return;
            }
        }
        receiveBuf.addFirst(packet);
        return;
    }
}

class Timer extends Thread{
    private SendPacket sendPacket;
    private static long timeout=1000;
    Timer(SendPacket sender){
        sendPacket=sender;
    }
    public void run(){
        while(true){
            if(sendPacket.rdt.isClose) return;
            try {
                sleep(100);
                if(sendPacket.waitBuf.isEmpty()) continue;
                if((System.currentTimeMillis()-sendPacket.waitBuf.getFirst().time)>timeout)
                    resend();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    private void resend(){
        try {
            Packet packet = sendPacket.waitBuf.getFirst().packet;
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                    sendPacket.rdt.remoteAddr, sendPacket.rdt.remotePort);
            sendPacket.rdt.localSoc.send(udpPacket);
            sendPacket.waitBuf.getFirst().time=System.currentTimeMillis();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

class PacTime{
    public Packet packet;
    public long time;
    PacTime(Packet pac){
        packet=pac;
    }
}