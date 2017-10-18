import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RDT {
    private boolean isConnected;
    private static int listenPort=11000;
    private DatagramSocket localSoc;
    private DatagramSocket listenSoc;
    private String remoteIp;
    private int remotePort;
    public RDT(String ip, int port){
        try {
            localSoc = new DatagramSocket();
            listenSoc= new DatagramSocket(listenPort++);
            remoteIp=ip;
            remotePort=port;

            isConnected = false;
        }catch (SocketException e){
            e.printStackTrace();
        }
    }
    public OutputStream getOutputStream(){return null;}
    public InputStream getInputStream(){return null;}
    public boolean isConnected(){return isConnected;}
}
