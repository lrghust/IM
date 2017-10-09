import java.net.*;
import java.io.*;
public class Server extends Thread {
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(1000000000);
    }

    public void run() {
        while(true) {
            try {
                System.out.println("等待远程连接，端口号为：" + serverSocket.getLocalPort() + "...");
                Socket server = serverSocket.accept();
                System.out.println("远程主机地址：" + server.getRemoteSocketAddress());
                BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                PrintWriter writer=new PrintWriter(server.getOutputStream());
                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
                String inStr,outStr;
                while(true){
                    inStr=in.readLine();
                    if(!inStr.equals("quit")){
                        System.out.println("Client:"+inStr);
                    }
                    else break;
                    outStr=br.readLine();
                    writer.println(outStr);
                    writer.flush();
                }
                out.writeUTF(server.getLocalSocketAddress() + "\nGoodbye!");
                server.close();
            }catch(SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            }catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
    public static void main(String [] args) {
        int port = 8888;
        try {
            Thread t = new Server(port);
            t.run();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
