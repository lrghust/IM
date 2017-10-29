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

    public LinkedBlockingDeque<Packet> receiveBuf;
    public LinkedBlockingDeque<Packet> sendBuf;
    public LinkedBlockingDeque<PacTime> waitBuf;


    public int packetLength=5000;
    public int headerLength=6;
    public int winSize=1024;
    public int indexSpace=4096;
    public int receiveWinBegin=0;
    public int sendWinBegin=0;
    private int sendIndex=0;
    private int recvIndex=0;

    public long estimatedRTT=500;
    public long devRTT=0;
    public long resendTimeout=500;
    public int closeTimeOut=30000;

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
            isClose=false;
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
            isClose=false;
            shakeACK();
        }catch (SocketException e){
            e.printStackTrace();
        }
    }

    public boolean isConnected(){return isConnected;}

    public void shake(){
        try {
            //send shake packet
            Packet packet = new Packet();
            packet.setShake();
            packet.setCheckSum();
            remoteAddr = InetAddress.getByName(remoteIp);
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
            localSoc.send(udpPacket);
            //receive shake ACK
            byte[] recvBytes=new byte[packetLength];
            DatagramPacket recvPacket = new DatagramPacket(recvBytes,recvBytes.length);
            localSoc.setSoTimeout(1000);
            try {
                localSoc.receive(recvPacket);
            }catch (SocketTimeoutException ste){
                System.out.printf("port:%d\n",localSoc.getLocalPort());
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
            System.out.printf("ip:%s port:%d\n",remoteIp,remotePort);
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
        while(totalLen>packetLength-headerLength){
            Packet packet=new Packet();
            packet.setIndex(sendIndex++);
            sendIndex%=indexSpace;
            packet.setData(Arrays.copyOfRange(data,curLen,curLen+packetLength));
            packet.setCheckSum();
            sendBuf.offer(packet);
            totalLen-=packetLength;
            curLen+=packetLength;
        }
        Packet packet=new Packet();
        packet.setIndex(sendIndex++);
        sendIndex%=indexSpace;
        packet.setData(Arrays.copyOfRange(data,curLen,curLen+totalLen));
        packet.setCheckSum();
        while(sendBuf.size()>=indexSpace){
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
            recvIndex%=indexSpace;
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
        int iTimeOut=0;
        while(true){
            if(isClose) return -1;
            Packet packet;
            if(receiveBuf.isEmpty()||(packet=getPacketInOrder(recvIndex))==null){
                try {
                    Thread.sleep(1);
                    if(receiveBuf.isEmpty()){
                        if(++iTimeOut==500) {
                            if(curLen!=0) return curLen;
                            iTimeOut=0;
                        }
                    }
                    else iTimeOut=0;
                    continue;
                }catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
            }
            iTimeOut=0;
            recvIndex++;
            recvIndex%=indexSpace;
            int pacLen=packet.length();
            if(curLen+pacLen>needLen){
                receiveBuf.addFirst(packet);
                if(recvIndex==0) recvIndex=indexSpace-1;
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
            PacTime pacTime=waitBuf.poll();
            Packet packet = pacTime.packet;
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                    remoteAddr, remotePort);
            localSoc.send(udpPacket);
            System.out.printf("resend:%d waitlist:%d timeout:%d\n",packet.getIndex(),waitBuf.size(),resendTimeout);
            pacTime.time=System.currentTimeMillis();
            pacTime.isResend=true;
            waitBuf.offer(pacTime);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private Packet getPacketInOrder(int recvIndex){
        Iterator<Packet> iter=receiveBuf.iterator();
        while(iter.hasNext()){
            Packet packet=iter.next();
            if(packet.getIndex()==recvIndex) {
                iter.remove();
                return packet;
            }
        }
        return null;
    }

    public void close(){
        try {
            while(!waitBuf.isEmpty()||!sendBuf.isEmpty()||!receiveBuf.isEmpty()){
                Thread.sleep(1);
            }
            Thread.sleep(1);
            System.out.println("begin close");
            //send FIN
            Packet packet = new Packet();
            packet.setFIN();
            packet.setCheckSum();
            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, remoteAddr, remotePort);
            localSoc.send(udpPacket);
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    public boolean checkIndex(int index, int winBegin){
        if(winBegin+winSize-1<indexSpace){
            return (index<=(winBegin+winSize-1))&&(index>=winBegin);
        }
        else{
            return ((index>=winBegin)&&(index<indexSpace))||((index>=0)&&(index<=((winBegin+winSize-1)%indexSpace)));
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
            try {
                sleep(rdt.resendTimeout/20);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            if(rdt.waitBuf.size()<=128&&rdt.checkIndex(rdt.sendBuf.getFirst().getIndex(),rdt.sendWinBegin)){
                try {
                    Packet packet = rdt.sendBuf.poll();
                    DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                            rdt.remoteAddr, rdt.remotePort);
                    rdt.localSoc.send(udpPacket);
                    //store in resend queue
                    PacTime pacTime=new PacTime(packet);
                    pacTime.time=System.currentTimeMillis();
                    rdt.waitBuf.offer(pacTime);
                    System.out.printf("sendindex:%d waitlist:%d sendwin:%d timeout:%d\n",packet.getIndex(),rdt.waitBuf.size(),rdt.sendWinBegin,rdt.resendTimeout);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}

class ReceivePacket extends Thread{
    private RDT rdt;
    private int sampleFlag=0;
    TreeSet<Integer> overPacket;
    TreeSet<Integer> overAck;

    ReceivePacket(RDT tRdt){
        rdt=tRdt;
        overPacket=new TreeSet<>();
        overAck=new TreeSet<>();
    }

    private boolean removeResendPacket(int index){
        Iterator<PacTime> iter=rdt.waitBuf.iterator();
        while(iter.hasNext()){
            PacTime pacTime=iter.next();
            if(pacTime.packet.getIndex()==index) {
                if(sampleFlag%10==0) {
                    long sampleRTT = System.currentTimeMillis() - pacTime.time;
                    rdt.estimatedRTT = (long) (0.875 * rdt.estimatedRTT + 0.125 * sampleRTT);
                    rdt.devRTT = (long) (0.75 * rdt.devRTT + 0.25 * Math.abs(sampleRTT - rdt.estimatedRTT));
                    rdt.resendTimeout = Math.max(rdt.estimatedRTT + 4 * rdt.devRTT,10);
                }
                iter.remove();
                return true;
            }
        }
        return false;
    }

    public void run(){
        int redundantAck=0;
        while(true){
            if(rdt.isClose) return;
            try {
                byte[] recvBytes = new byte[rdt.packetLength];
                DatagramPacket recvPacket = new DatagramPacket(recvBytes, recvBytes.length);
                rdt.localSoc.receive(recvPacket);
                Packet packet = new Packet(recvPacket.getData());
                if(packet.checkSum()){
                    //fin
                    if(packet.isFIN()){
                        if(packet.isACK()){//fin sender
                            System.out.println("receive fin ack");
                            //wait FIN
                            byte []finBytes=new byte[rdt.packetLength];
                            DatagramPacket udpPacket = new DatagramPacket(finBytes, finBytes.length);
                            rdt.localSoc.receive(udpPacket);
                            packet=new Packet(udpPacket.getData());
                            if(packet.isFIN()){
                                System.out.println("receive fin");
                                //send ACK
                                packet = new Packet();
                                packet.setACK((byte)0);
                                packet.setCheckSum();
                                udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, rdt.remoteAddr, rdt.remotePort);
                                Thread.sleep(100);
                                rdt.localSoc.send(udpPacket);
                                //wait timeout
                                Thread.sleep(rdt.closeTimeOut);
                                //close
                                rdt.localSoc.close();
                                rdt.isClose=true;
                            }
                        }
                        else {//fin receiver
                            System.out.println("receive fin");
                            while (!rdt.receiveBuf.isEmpty()){
                                sleep(500);
                            }
                            sleep(500);
                            //send ACK
                            packet = new Packet();
                            packet.setACK(0);
                            packet.setFIN();
                            packet.setCheckSum();
                            DatagramPacket finUdpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, rdt.remoteAddr, rdt.remotePort);
                            sleep(100);
                            rdt.localSoc.send(finUdpPacket);
                            //send FIN
                            packet = new Packet();
                            packet.setFIN();
                            packet.setCheckSum();
                            finUdpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, rdt.remoteAddr, rdt.remotePort);
                            sleep(100);
                            rdt.localSoc.send(finUdpPacket);
                            //receive ack
                            byte[] finAck = new byte[rdt.packetLength];
                            finUdpPacket = new DatagramPacket(finAck, finAck.length);
                            rdt.localSoc.receive(finUdpPacket);
                            packet = new Packet(finUdpPacket.getData());
                            if (packet.isACK()) {
                                System.out.println("receive fin ack");
                                rdt.localSoc.close();
                                rdt.isClose = true;
                            }
                        }
                    }
                    //ack
                    else if(packet.isACK()){
                        int ackIndex=packet.getAck();
                        if(rdt.checkIndex(ackIndex,rdt.sendWinBegin)) {
                            if(ackIndex==rdt.sendWinBegin){
                                sampleFlag++;
                                while(true) {
                                    rdt.sendWinBegin++;
                                    rdt.sendWinBegin %= rdt.indexSpace;
                                    if(overAck.contains(rdt.sendWinBegin))
                                        overAck.remove(rdt.sendWinBegin);
                                    else break;
                                }
                                //redundantAck=0;
                            }
                            else {
                                overAck.add(ackIndex);
                                //redundantAck++;
                                //if (redundantAck == 3) {
                                    //rdt.resend();
                                    //redundantAck = 0;
                                //}
                            }
                        }
                        removeResendPacket(ackIndex);
                        //System.out.printf("ackindex:%d sendwin:%d waitlist:%d\n",ackIndex,rdt.sendWinBegin,rdt.waitBuf.size());
                        //rdt.waitBuf.removeIf(p->p.packet.getIndex()==ackIndex);
                    }
                    //data packet
                    else {
                        if (rdt.checkIndex(packet.getIndex(), rdt.receiveWinBegin)) {
                            if (packet.getIndex() == rdt.receiveWinBegin) {
                                //in queue
                                rdt.receiveBuf.offer(packet);
                                while(true) {
                                    rdt.receiveWinBegin++;
                                    rdt.receiveWinBegin %= rdt.indexSpace;
                                    if(overPacket.contains(rdt.receiveWinBegin))
                                        overPacket.remove(rdt.receiveWinBegin);
                                    else break;
                                }
                            }
                            else{
                                if(!overPacket.contains(packet.getIndex())) {
                                    overPacket.add(packet.getIndex());
                                    //in queue
                                    rdt.receiveBuf.offer(packet);
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
                            System.out.printf("receivewin:%d sendack:%d\n", rdt.receiveWinBegin, ackIndex);
                        }
                        else if(rdt.checkIndex((packet.getIndex()+rdt.winSize)%rdt.indexSpace,rdt.receiveWinBegin)) {
                            //send ack
                            int ackIndex = packet.getIndex();
                            packet = new Packet();
                            packet.setACK(ackIndex);
                            packet.setCheckSum();
                            DatagramPacket udpPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length,
                                    rdt.remoteAddr, rdt.remotePort);
                            rdt.localSoc.send(udpPacket);
                            System.out.printf("receivewin:%d sendack:%d\n", rdt.receiveWinBegin, ackIndex);
                        }
                    }
                }
            }catch (IOException | InterruptedException e){
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
    Timer(RDT tRdt){
        rdt=tRdt;
    }
    public void run(){
        while(true){
            if(rdt.isClose) return;
            try {
                sleep(Math.max(rdt.resendTimeout/20,1));
                if(rdt.waitBuf.isEmpty()) continue;
                if((System.currentTimeMillis()-rdt.waitBuf.getFirst().time)>rdt.resendTimeout) {
                    boolean flag=false;
                    while ((System.currentTimeMillis() - rdt.waitBuf.getFirst().time) > rdt.resendTimeout) {
                        if(rdt.waitBuf.getFirst().isResend) flag=true;
                        rdt.resend();
                        sleep(Math.max(rdt.resendTimeout/20,1));
                    }
                    if (!flag) {
                        rdt.resendTimeout *= 1.5;
                    }
                }
            }catch (InterruptedException | NoSuchElementException e){
                e.printStackTrace();
            }
        }
    }
}

class PacTime{
    public Packet packet;
    public long time;
    public boolean isResend=false;
    PacTime(Packet pac){
        packet=pac;
    }
}