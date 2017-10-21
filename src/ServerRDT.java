import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerRDT {
    private DatagramSocket serverSoc;
    public ServerRDT(int port){
        try {
            serverSoc = new DatagramSocket(port);

        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public RDT accept(){
        while(true) {
            try {
                byte[] recvBytes = new byte[1030];
                DatagramPacket recvPacket = new DatagramPacket(recvBytes, recvBytes.length);
                serverSoc.receive(recvPacket);
                Packet packet=new Packet(recvPacket.getData());
                if(packet.isShake()) {
                    InetAddress addr = recvPacket.getAddress();
                    String remoteIp = addr.getHostAddress();
                    int remotePort = recvPacket.getPort();

                    Packet sendPacket = new Packet();
                    sendPacket.setACK((byte)0);
                    sendPacket.setCheckSum();

                    RDT rdt = new RDT(remoteIp, remotePort, true);
                    return rdt;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
