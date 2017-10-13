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

    private String serverIp="localhost";
    private int serverPort=10000;
    public int listenPort=8890;
    public String localUserName;


    public Client(){//初始
        try {
            //login
            serverSoc=new Socket(serverIp, serverPort);
            serverSocWriter = new PrintWriter(serverSoc.getOutputStream());
            serverSocReader = new BufferedReader(new InputStreamReader(serverSoc.getInputStream()));
            uiLogin = new Login(this);
            receiveFromServer();
            uiLogin.close();
            //start
            listenSoc = new ServerSocket(listenPort);
            uiIm = new IM(this);
            uiIm.showText("Welcome, "+ localUserName +"!\n");
            while (true) {
                localSoc = listenSoc.accept();
                Thread tDialog = new Dialog(this, uiIm);
                tDialog.start();
            }
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

    public boolean connect(String id){
        try {
            serverSocWriter.println("QUERYADDR");
            serverSocWriter.flush();
            serverSocWriter.println(id);
            serverSocWriter.flush();
            String addr=serverSocReader.readLine();
            if(addr.equals("QUERYADDR:errorid")) return false;
            String[] ip_port=addr.split(" ");
            localSoc = new Socket(ip_port[0],Integer.parseInt(ip_port[1]));
            Thread tDialog=new Dialog(this,uiIm);
            tDialog.start();
            return true;
        }catch(IOException e) {
            e.printStackTrace();
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
