import java.net.*;
import java.io.*;
public class Client extends Thread {
    private ServerSocket listenSoc;
    private Socket localSoc;
    private Socket loginSoc;
    private PrintWriter writer;
    private BufferedReader reader;
    private IM im;
    private Dialog dialog;
    private Login gLogin;
    private String serverIp="localhost";
    private int serverPort=10000;
    private String remoteIp;
    private int remotePort;


    public Client(){//初始
        try {
            loginSoc=new Socket(serverIp, serverPort);
            writer = new PrintWriter(loginSoc.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(loginSoc.getInputStream()));
            gLogin = new Login(this);
            run();
            loginSoc.close();
            gLogin.close();

            int port = 8888;
            listenSoc = new ServerSocket(port);
            im = new IM(this);
            im.showText("IP:" + listenSoc.getInetAddress().getHostName() + " Port:" + listenSoc.getLocalPort() + "\n");
            while (true) {
                localSoc = listenSoc.accept();
                im.showText("建立连接："+localSoc.getInetAddress().getHostAddress()+"/"+String.valueOf(localSoc.getPort())+"\n");
                Thread tClient = new Client(localSoc, im);
                tClient.start();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public Client(Socket tLocalSoc, IM tim){//对话
        try {
            writer = new PrintWriter(tLocalSoc.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(tLocalSoc.getInputStream()));
            localSoc = tLocalSoc;
            remoteIp=localSoc.getInetAddress().getHostAddress();
            remotePort=localSoc.getPort();
            im=tim;
            dialog=new Dialog(this);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void run(){
        while(true){
            try {
                String inStr;
                inStr = reader.readLine();
                if(inStr==null) {
                    localSoc.close();
                    break;
                }
                String []group=inStr.split(":");
                String mark=group[0];
                String context=inStr.substring(mark.length()+1,inStr.length());
                switch (mark){
                    case "LOGIN":{
                        if(context.equals("ok")){
                            gLogin.showMessage("登录成功！");
                            return;
                        }
                        else if(context.equals("errorid")){
                            gLogin.showMessage("用户名不存在！");
                        }
                        else if(context.equals("errorkey")){
                            gLogin.showMessage("密码错误！");
                        }
                        break;
                    }
                    case "SIGNUP":{
                        if(context.equals("ok")){
                            gLogin.showMessage("注册成功！");
                        }
                        else if(context.equals("errorid")){
                            gLogin.showMessage("用户名已存在！");
                        }
                        break;
                    }
                    case "FINDKEY":{
                        if(context.equals("errorid")){
                            gLogin.showMessage("用户名不存在！");
                        }
                        else{
                            gLogin.showMessage("密码："+context);
                        }
                        break;
                    }
                    case "TEXT":{
                        dialog.showText(remoteIp+"/"+String.valueOf(remotePort)+":"+context+"\n");
                        break;
                    }
                    default:break;
                }
            }catch(IOException e){
                System.out.println(e.getMessage());
                break;
            }
        }
    }

    public void connect(String ip, int port){
        try {
            localSoc = new Socket(ip,port);
            im.showText("建立连接："+localSoc.getInetAddress().getHostAddress()+"／"+String.valueOf(localSoc.getPort())+"\n");
            Thread tClient=new Client(localSoc, im);
            tClient.start();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String str){
        writer.println(str);
        writer.flush();
    }

    public void close(){
        try {
            localSoc.close();
            im.showText("连接结束\n");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String []args) throws IOException{
        Client client=new Client();
    }
}
