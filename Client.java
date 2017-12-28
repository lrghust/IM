import java.net.*;
import java.io.*;
public class Client extends Thread {
    private Socket serverSoc;
    public PrintWriter serverSocWriter;
    public BufferedReader serverSocReader;
    private ServerSocket listenSoc;
    public Socket localSoc;

    private IM uiIm;
    private Login uiLogin;

    private String serverIp="10.11.37.134";
    private int serverPort=9000;
    public int listenPort;
    public String localUserName;


    public Client(){//初始
        try {
            //login
            serverSoc=new Socket();
            InetSocketAddress inetSocketAddress=new InetSocketAddress(serverIp,serverPort);
            serverSoc.connect(inetSocketAddress,5000);
            listenSoc = new ServerSocket(0);
            listenPort=listenSoc.getLocalPort();
            serverSocWriter = new PrintWriter(serverSoc.getOutputStream());
            serverSocReader = new BufferedReader(new InputStreamReader(serverSoc.getInputStream()));
            uiLogin = new Login(this);
            receiveFromServer();
            uiLogin.close();
            //start
            uiIm = new IM(this);
            uiIm.showText("Welcome, "+ localUserName +"!\n");
            receiveOfflineText();
            while (true) {
                localSoc = listenSoc.accept();
                Thread tDialog = new Dialog(this, uiIm);
                tDialog.start();
            }
        }catch (ConnectException ce){
            System.out.println(ce.getMessage());
            uiLogin = new Login(this);
            uiLogin.showMessage("连接服务器失败！");
            uiLogin.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public boolean receiveFromServer(){
        while(true){
            try {
                String inStr;
                inStr = serverSocReader.readLine();
                if(inStr==null) {
                    serverSoc.close();
                    return false;
                }
                String []group=inStr.split(":");
                String mark=group[0];
                String context=group[1];
                switch (mark){
                    case "LOGIN":{
                        if(context.equals("ok")){
                            uiLogin.showMessage("登录成功！");
                            localUserName=uiLogin.userName;
                            return true;
                        }
                        else if(context.equals("errorid")){
                            uiLogin.showMessage("用户名不存在！");
                        }
                        else if(context.equals("errorkey")){
                            uiLogin.showMessage("密码错误！");
                        }
                        else if(context.equals("erroralready")){
                            uiLogin.showMessage("用户已上线！");
                        }
                        break;
                    }
                    case "SIGNUP":{
                        if(context.equals("ok")){
                            uiLogin.showMessage("注册成功！");
                        }
                        else if(context.equals("errorid")){
                            uiLogin.showMessage("用户名已存在！");
                        }
                        break;
                    }
                    case "FINDKEY":{
                        if(context.equals("errorid")){
                            uiLogin.showMessage("用户名不存在！");
                        }
                        else{
                            uiLogin.showMessage("密码："+context);
                        }
                        break;
                    }
                    default:break;
                }
            }catch(IOException e){
                e.printStackTrace();
                return false;
            }
        }
    }

    public void receiveOfflineText(){
        while(true){
            try{
                String inStr=serverSocReader.readLine();
                String[] group=inStr.split(":");
                String mark=group[0];
                String context=inStr.substring(mark.length()+1,inStr.length());
                switch (mark){
                    case "OFFLINETEXT":{
                        uiIm.showText("离线消息："+context+"\n");
                        break;
                    }
                    case "OFFLINEEND":return;
                    default:break;
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public boolean connect(String id){
        try {
            send("QUERYADDR");
            send(id);
            String info=serverSocReader.readLine();
            String[] group=info.split(":");
            String addr=group[1];
            String[] ip_port=new String[2];
            if(addr.equals("errorid")) return false;
            else if(addr.equals("OFFLINE")) {
                localSoc = new Socket(serverIp,serverPort);
                Thread tDialog=new Dialog(this,uiIm,id);
                tDialog.start();
                return true;
            }
            else {
                ip_port=addr.split(" ");
                localSoc = new Socket(ip_port[0],Integer.parseInt(ip_port[1]));
                Thread tDialog=new Dialog(this,uiIm);
                tDialog.start();
                return true;
            }
        }catch(IOException e) {
            if(e.getMessage().equals("Connection reset"))
                uiIm.showText("与服务器连接中断！\n");
            else e.printStackTrace();
            return false;
        }
    }

    public void send(String str){
        serverSocWriter.println(str);
        serverSocWriter.flush();
    }

    public static void main(String []args) throws IOException{
        Client client=new Client();
    }
}
