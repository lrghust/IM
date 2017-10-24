import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

public class RDT {
    private boolean isConnected;
    public DatagramSocket localSoc;
    private String remoteIp;
    public InetAddress remoteAddr;
    public int remotePort;

    //public LinkedList<Packet> receiveBuf;
    //public LinkedList<Packet> sendBuf;
    //public LinkedList<PacTime> waitBuf;
    public LinkedBlockingDeque<Packet> receiveBuf;
    public LinkedBlockingDeque<Packet> sendBuf;
    public LinkedBlockingDeque<PacTime> waitBuf;


    public int receiveWinBegin;
    public int sendWinBegin;
    private int sendIndex;
    private int recvIndex;

    private int closeTimeOut=30000;

    public boolean isClose;

    public RDT(String ip, int port){
        try {
            localSoc = new DatagramSocket();
            remoteIp=ip;
            remotePort=port;
            isConnected = false;
            receiveBuf=new LinkedBlockingDeque<>();
            sendBuf=new LinkedBlockingDeque<>();
            waitBuf=new LinkedBlockingDeque<>();
            receiveWinBegin=0;
            sendWinBegin=0;
            sendIndex=0;
            recvIndex=0;
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
            receiveBuf=new LinkedBlockingDeque<>();
            sendBuf=new LinkedBlockingDeque<>();
            waitBuf=new LinkedBlockingDeque<>();
            receiveWinBegin=0;
            sendWinBegin=0;
            sendIndex=0;
            recvIndex=0;
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
            remoteAddr=recvPacket.getAddress();
            remoteIp=remoteAddr.getHostAddress();
            remotePort=recvPacket.getPort();
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
            packet.setACK(0);
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
            packet.setIndex(sendIndex++);
            sendIndex%=128;
            packet.setData(Arrays.copyOfRange(data,curLen,curLen+1024));
            packet.setCheckSum();
            sendBuf.offer(packet);
            totalLen-=1024;
            curLen+=1024;
        }
        Packet packet=new Packet();
        packet.setIndex(sendIndex++);
        sendIndex%=128;
        packet.setData(Arrays.copyOfRange(data,curLen,curLen+totalLen));
        packet.setCheckSum();
        while(sendBuf.size()>128){
            try {
                Thread.sleep(1);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        sendBuf.offer(packet);
    }

    public String readLine(){
        StringBuilder line=new StringBuilder();
        while(true){
            if(isClose) return null;
            Packet packet;
            if(receiveBuf.isEmpty()||(packet=getPacketInOrder(recvIndex))==null){
                try {
                    Thread.sleep(1);
                    continue;
                }catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
            }
            recvIndex++;
            recvIndex%=128;
            byte[] data=packet.getData();
            line.append(new String(data));
            if(data[data.length-1]==0xa){
                return line.toString();
            }
        }
        return null;
    }

    public int read(byte[] data){
        int needLen=data.length;
        int curLen=0;
        while(true){
            if(isClose) return -1;
            Packet packet;
            if(receiveBuf.isEmpty()||(packet=getPacketInOrder(recvIndex))==null){
                try {
                    Thread.sleep(1);
                    continue;
                }catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
            }
            recvIndex++;
            recvIndex%=128;
            int pacLen=packet.length();
            if(curLen+pacLen>needLen){
                receiveBuf.addFirst(packet);

                if(recvIndex==0) recvIndex=127;
                else recvIndex--;
                return curLen;
            }
            System.arraycopy(packet.getData(),0,data,curLen,pacLen);
            curLen+=pacLen;
            if(curLen==needLen)
                return curLen;
        }
        return -1;
    }

    public void resend(){
        try {
            Packet packet = waitBuf.getFirst().packet;
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                    remoteAddr, remotePort);
            localSoc.send(udpPacket);
            System.out.printf("resend:%d waitlist:%d\n",packet.getIndex(),waitBuf.size());
            waitBuf.getFirst().time=System.currentTimeMillis();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private Packet getPacketInOrder(int recvIndex){
        Iterator<Packet> iter=receiveBuf.iterator();
        int i=0;
        while(iter.hasNext()){
            Packet packet=iter.next();
            if(packet.getIndex()==recvIndex) {
                iter.remove();
                return packet;
            }
            if(++i==64)
                return null;
        }
        return null;
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
    SendPacket(RDT tRdt){
        rdt=tRdt;
        Thread timer=new Timer(rdt);
        timer.start();
    }

    private boolean checkIndex(int index, int winBegin){
        if(winBegin<65){
            return (index<=(winBegin+63))&&(index>=winBegin);
        }
        else{
            return ((index>=winBegin)&&(index<128))||((index>=0)&&(index<=((winBegin+63)%128)));
        }
    }

    public void run(){
        while(true){
            if(rdt.isClose) return;
            if(rdt.sendBuf.isEmpty()){
                try {
                    sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                continue;
            }

            if(checkIndex(rdt.sendBuf.getFirst().getIndex(),rdt.sendWinBegin)){
                try {
                    sleep(0,1);
                    Packet packet = rdt.sendBuf.poll();
                    DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                            rdt.remoteAddr, rdt.remotePort);
                    rdt.localSoc.send(udpPacket);
                    //store in resend queue
                    PacTime pacTime=new PacTime(packet);
                    pacTime.time=System.currentTimeMillis();
                    rdt.waitBuf.offer(pacTime);
                    System.out.printf("sendindex:%d waitlist:%d\n",packet.getIndex(),rdt.waitBuf.size());
                }catch (IOException e){
                    e.printStackTrace();
                }catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
            else{
                try {
                    sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}

class ReceivePacket extends Thread{
    private RDT rdt;
    ReceivePacket(RDT tRdt){
        rdt=tRdt;
    }

    private boolean checkIndex(int index, int winBegin){
        if(winBegin<65){
            return (index<=(winBegin+63))&&(index>=winBegin);
        }
        else{
            return ((index>=winBegin)&&(index<128))||((index>=0)&&(index<=((winBegin+63)%128)));
        }
    }

    private boolean removeResendPacket(int index){
        Iterator<PacTime> iter=rdt.waitBuf.iterator();
        int i=0;
        while(iter.hasNext()){
            PacTime pacTime=iter.next();
            if(pacTime.packet.getIndex()==index) {
                iter.remove();
                System.out.printf("removefromwaitlist:%d\n",index);
                return true;
            }
            if(++i==64)
                return false;
        }
        return false;
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
                        packet.setACK(0);
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
                    else if(packet.isACK()){
                        int ackIndex=packet.getAck();
                        if(ackIndex==rdt.sendWinBegin){
                            rdt.sendWinBegin++;
                            rdt.sendWinBegin%=128;
                            //todo fast resend
                        }
                        removeResendPacket(ackIndex);
                        System.out.printf("ackindex:%d sendwin:%d waitlist:%d\n",ackIndex,rdt.sendWinBegin,rdt.waitBuf.size());
                        //rdt.waitBuf.removeIf(p->p.packet.getIndex()==ackIndex);
                    }
                    else {//data packet
                        if (checkIndex(packet.getIndex(), rdt.receiveWinBegin)) {
                            //in queue
                            rdt.receiveBuf.offer(packet);
                            if (packet.getIndex() == rdt.receiveWinBegin) {
                                rdt.receiveWinBegin++;
                                rdt.receiveWinBegin %= 128;
                            }
                        }
                        //send ack
                        int ackIndex = packet.getIndex();
                        packet = new Packet();
                        packet.setACK(ackIndex);
                        packet.setCheckSum();
                        DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                                rdt.remoteAddr, rdt.remotePort);
                        rdt.localSoc.send(udpPacket);
                        System.out.printf("index:%d receivewin:%d sendack:%d\n",packet.getIndex(),rdt.receiveWinBegin,ackIndex);
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    //todo
    /*
    private void inQueue(Packet packet){
        int index=packet.getIndex();
        ListIterator<Packet> iter=rdt.receiveBuf.listIterator(rdt.receiveBuf.size());
        while(iter.hasPrevious()){
            int prevIndex=iter.previous().getIndex();
            if(index>prevIndex || prevIndex-index>63){
                rdt.receiveBuf.add(iter.nextIndex()+1,packet);
                return;
            }
        }
        rdt.receiveBuf.addFirst(packet);
    }*/
}

class Timer extends Thread{
    private RDT rdt;
    private static long timeout=1;
    Timer(RDT tRdt){
        rdt=tRdt;
    }
    public void run(){
        while(true){
            if(rdt.isClose) return;
            try {
                sleep(1);
                if(rdt.waitBuf.isEmpty()) continue;
                if((System.currentTimeMillis()-rdt.waitBuf.getFirst().time)>timeout)
                    rdt.resend();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
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