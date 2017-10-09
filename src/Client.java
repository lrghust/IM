import java.net.*;
import java.io.*;
public class Client {

    public Socket clientSoc;
    public PrintWriter writer;
    public BufferedReader reader;
    public String ip;
    public int port;

    public Client() {
        ip = "localhost";
        port = 8888;
    }

    public void connect(){
        try {
            clientSoc = new Socket(ip, port);//获取client对象
            writer = new PrintWriter(clientSoc.getOutputStream());
            reader=new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String str){
        writer.println(str);
        writer.flush();
    }

    public String receive(){
        String inStr;
        try {
            inStr = reader.readLine();
        }catch(IOException e) {
            e.printStackTrace();
            inStr="Error";
        }
        return inStr;
    }
    public void close(){
        try {
            clientSoc.close();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
