import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    public Socket serverSoc;
    private BufferedReader reader;
    private PrintWriter writer;
    private String userName;
    private String offlinefrom;
    private String offlineto;
    public ServerUI ui;

    private enum State{wait, login, signup, findkey, queryaddr, offline, text}

    public ArrayList<User> Users;

    public Server(int port) throws IOException {
        ui=new ServerUI();
        Users=new ArrayList<>();
        serverSocket = new ServerSocket(port);
        while(true){
            serverSoc = serverSocket.accept();
            Thread tServer=new Server(this);
            tServer.start();
        }
    }

    public Server(Server tServer) throws IOException{
        ui=tServer.ui;
        Users=tServer.Users;
        serverSoc=tServer.serverSoc;
        reader = new BufferedReader(new InputStreamReader(serverSoc.getInputStream()));
        writer = new PrintWriter(serverSoc.getOutputStream());
    }

    public void run() {
        State state=State.wait;
        while(true) {
            try {
                String inStr,outStr;
                inStr=reader.readLine();
                if(inStr==null){
                    break;
                }
                switch (state) {
                    case wait: {
                        switch (inStr) {
                            case "LOGIN":
                                state = State.login;
                                break;
                            case "SIGNUP":
                                state = State.signup;
                                break;
                            case "FINDKEY":
                                state = State.findkey;
                                break;
                            case "QUERYADDR":
                                state = State.queryaddr;
                                break;
                            case "CLOSE":{
                                deleteUser(userName);
                                ui.showText("用户"+userName+"下线\n");
                                return;
                            }
                            case "OFFLINE":{
                                state=State.offline;
                            }
                            default:
                                break;
                        }
                        break;
                    }
                    case login: {
                        String[] pair = inStr.split(" ");
                        ui.showText("尝试登陆："+"ID:"+pair[0]+" IP:"+pair[2]+"\n");
                        int check = checkPair(pair[0], pair[1]);
                        if (check == 0) {
                            outStr = "LOGIN:ok";
                            ui.showText(pair[0]+"密码正确！\n");
                            String ip=serverSoc.getInetAddress().getHostAddress();
                            if(!saveLoginUser(pair[0],ip,Integer.parseInt(pair[2]))) {
                                outStr = "LOGIN:erroralready";
                                ui.showText(pair[0]+"已登陆，不能重复登录！\n");
                            }
                            userName=pair[0];
                            writer.println(outStr);
                            writer.flush();
                            if(outStr.equals("LOGIN:ok")) {
                                sendOfflineText(userName);

                            }
                            state=State.wait;
                            break;
                        }
                        else if (check == 1) {
                            outStr = "LOGIN:errorkey";
                            ui.showText(pair[0]+"密码错误！\n");
                        }
                        else {
                            outStr = "LOGIN:errorid";
                            ui.showText("用户名"+pair[0]+"不存在！\n");
                        }
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case signup: {
                        String []pair = inStr.split(" ");
                        boolean check = savePair(pair[0], pair[1]);
                        if(check) {
                            outStr = "SIGNUP:ok";
                            ui.showText("用户"+pair[0]+"注册成功!\n");
                        }
                        else {
                            outStr = "SIGNUP:errorid";
                            ui.showText("注册失败！用户"+pair[0]+"已存在\n");
                        }
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case findkey: {
                        String key = getKey(inStr);
                        if (key == null) {
                            outStr = "FINDKEY:errorid";
                            ui.showText("找回密码失败！用户名"+inStr+"不存在!\n");
                        }
                        else {
                            outStr = "FINDKEY:" + key;
                            ui.showText("用户"+inStr+"找回密码成功！\n");
                        }
                        writer.println(outStr);
                        writer.flush();
                        state = State.wait;
                        break;
                    }
                    case queryaddr:{
                        String addr=queryAddr(inStr);
                        if(addr.equals("UNEXIST")) {
                            outStr = "QUERYADDR:errorid";
                            ui.showText("用户"+userName+"尝试连接用户"+inStr+"失败，用户不存在！\n");
                        }
                        else {
                            outStr = "QUERYADDR:"+addr;
                            ui.showText("用户"+userName+"连接用户"+inStr+"成功，地址："+addr+"\n");
                        }
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case offline:{
                        String[] names=inStr.split(" ");
                        offlinefrom=names[0];
                        offlineto=names[1];
                        ui.showText(offlinefrom+"向"+offlineto+"发送离线消息：\n");
                        outStr="OFFLINE:ok";
                        writer.println(outStr);
                        writer.flush();
                        state=State.text;
                        break;
                    }
                    case text:{
                        String mark=inStr.split(":")[0];
                        saveOfflineText(inStr.substring(mark.length()+1,inStr.length()));
                        break;
                    }
                    default:
                        break;
                }
            }catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private int checkPair(String id, String key) throws IOException{
        File file=new File("/Users/lrg/users.dat");
        if(!file.exists()) file.createNewFile();
        FileInputStream fin=new FileInputStream("/Users/lrg/users.dat");
        byte[] oneUser=new byte[16];
        String line;
        while(fin.read(oneUser)!=-1) {
            line=decrypt(oneUser);
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) {
                if(key.equals(pair[1])) return 0;//right
                else return 1;//key is error
            }
        }
        fin.close();
        return 2;//no such id
    }

    private boolean savePair(String id, String key) throws IOException{
        File file=new File("/Users/lrg/users.dat");
        if(!file.exists()) file.createNewFile();
        FileInputStream fin=new FileInputStream("/Users/lrg/users.dat");
        byte[] oneUser=new byte[16];
        String line;
        while(fin.read(oneUser)!=-1) {
            line=decrypt(oneUser);
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) return false;
        }
        fin.close();
        FileOutputStream fout = new FileOutputStream("/Users/lrg/users.dat",true);
        oneUser=encrypt(id+" "+key);
        fout.write(oneUser);
        fout.close();
        return true;
    }

    private boolean saveLoginUser(String id, String ip, int port) throws IOException{
        for(int i=0;i<Users.size();i++){
            if(Users.get(i).id.equals(id)){
                return false;
            }
        }
        ui.addUser(id,ip,port);
        Users.add(new User(id,ip,port));
        return true;
    }

    private void deleteUser(String id) throws IOException{
        int i;
        for(i=0;i<Users.size();i++){
            if(Users.get(i).id.equals(id)){
                break;
            }
        }
        Users.remove(i);
        ui.deleteUser(i);
    }

    private String getKey(String id) throws IOException{
        File file=new File("/Users/lrg/users.dat");
        if(!file.exists()) file.createNewFile();
        FileInputStream fin=new FileInputStream("/Users/lrg/users.dat");
        byte[] oneUser=new byte[16];
        String line;
        while(fin.read(oneUser)!=-1) {
            line=decrypt(oneUser);
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) return pair[1];
        }
        fin.close();
        return null;
    }

    private String queryAddr(String id) throws IOException{
        File file=new File("/Users/lrg/users.dat");
        if(!file.exists()) file.createNewFile();
        FileInputStream fin=new FileInputStream("/Users/lrg/users.dat");
        byte[] oneUser=new byte[16];
        String line;
        while(fin.read(oneUser)!=-1) {
            line = decrypt(oneUser);
            String[] pair = line.split(" ");
            if (id.equals(pair[0])) {
                for (int i = 0; i < Users.size(); i++) {
                    User user = Users.get(i);
                    if (user.id.equals(id)) return user.ip + " " + String.valueOf(user.port);
                }
                return "OFFLINE";
            }
        }
        fin.close();
        return "UNEXIST";
    }

    private void saveOfflineText(String str) throws IOException{
        File file=new File("/Users/lrg/"+offlineto+".dat");
        if(!file.exists()) file.createNewFile();
        FileOutputStream fout = new FileOutputStream("/Users/lrg/"+offlineto+".dat",true);
        byte[] text=encrypt(offlinefrom+":"+str);
        fout.write(text);
        ui.showText(offlinefrom+" to "+offlineto+": "+str+"\n");
        fout.close();
    }

    private void sendOfflineText(String id) throws IOException{
        String filepath="/Users/lrg/"+id+".dat";
        File file=new File(filepath);
        if(file.exists()){
            FileInputStream fin=new FileInputStream(filepath);
            byte[] text=new byte[16];
            String line;
            while(fin.read(text)!=-1) {
                line=decrypt(text);
                writer.println("OFFLINETEXT:"+line);
                writer.flush();
                ui.showText("向"+id+"发送离线消息:"+line+'\n');
            }
            fin.close();
            FileWriter fw=new FileWriter(file);
            fw.write("");
            fw.flush();
            fw.close();
        }
        writer.println("OFFLINEEND:end");
        writer.flush();
    }

    private byte[] encrypt(String content){
        try {
            String password = "lrgnetserver";
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password.getBytes());
            kgen.init(128, random);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");// 创建密码器
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
            byte[] result = cipher.doFinal(byteContent);
            return result; // 加密
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String decrypt(byte[] content){
        try{
            String password = "lrgnetserver";
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(password.getBytes());
            kgen.init(128, random);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            Cipher cipher = Cipher.getInstance("AES");// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
            byte[] result = cipher.doFinal(content);
            return new String(result); // 加密
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String []args) throws IOException{
        int port = 9000;
        Server server = new Server(port);
    }
}
 class User{
    public String id;
    public String ip;
    public int port;
    public User(String id, String ip, int port){
        this.id=id;
        this.ip=ip;
        this.port=port;
     }
 }
