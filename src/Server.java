import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private Socket serverSoc;
    private BufferedReader reader;
    private PrintWriter writer;
    private String userName;
    private String offlinefrom;
    private String offlineto;

    private enum State{wait, login, signup, findkey, queryaddr, offline, text};

    private ArrayList<User> Users;

    public Server(int port) throws IOException {
        Users=new ArrayList<User>();
        serverSocket = new ServerSocket(port);
        while(true){
            serverSoc = serverSocket.accept();
            Thread tServer=new Server(serverSoc, Users);
            tServer.start();
        }
    }

    public Server(Socket tServerSoc, ArrayList<User> tUsers) throws IOException{
        Users=tUsers;
        serverSoc=tServerSoc;
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
                        int check = checkPair(pair[0], pair[1]);
                        if (check == 0) {
                            outStr = "LOGIN:ok";
                            String ip=serverSoc.getInetAddress().getHostAddress();
                            if(!saveLoginUser(pair[0],ip,Integer.parseInt(pair[2]))) outStr = "LOGIN:erroralready";
                            userName=pair[0];
                            writer.println(outStr);
                            writer.flush();
                            sendOfflineText(userName);
                            state=State.wait;
                            break;
                        }
                        else if (check == 1) outStr = "LOGIN:errorkey";
                        else outStr = "LOGIN:errorid";
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case signup: {
                        String []pair = inStr.split(" ");
                        boolean check = savePair(pair[0], pair[1]);
                        if(check) outStr = "SIGNUP:ok";
                        else outStr = "SIGNUP:errorid";
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case findkey: {
                        String key = getKey(inStr);
                        if (key == null) outStr = "FINDKEY:errorid";
                        else outStr = "FINDKEY:" + key;
                        writer.println(outStr);
                        writer.flush();
                        state = State.wait;
                        break;
                    }
                    case queryaddr:{
                        String addr=queryAddr(inStr);
                        if(addr=="UNEXIST") outStr = "QUERYADDR:errorid";
                        else outStr = "QUERYADDR:"+addr;
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
                    }
                    case offline:{
                        String[] names=inStr.split(" ");
                        offlinefrom=names[0];
                        offlineto=names[1];
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
        BufferedReader usersReader=new BufferedReader(new FileReader("/Users/lrg/users.txt"));
        String line;
        while((line=usersReader.readLine())!=null){
            String []pair=line.split(" ");
            if(id.equals(pair[0])){
                if(key.equals(pair[1])) return 0;//right
                else return 1;//key is error
            }
        }
        return 2;//no such id
    }

    private boolean savePair(String id, String key) throws IOException{
        BufferedReader usersReader=new BufferedReader(new FileReader("/Users/lrg/users.txt"));
        String line;
        while((line=usersReader.readLine())!=null) {
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) return false;
        }
        BufferedWriter usersWriter = new BufferedWriter(new FileWriter("/Users/lrg/users.txt",true));
        usersWriter.write(id+" "+key+"\n");
        usersWriter.close();
        return true;
    }

    private boolean saveLoginUser(String id, String ip, int port) throws IOException{
        for(int i=0;i<Users.size();i++){
            if(Users.get(i).id.equals(id)){
                return false;
            }
        }
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
    }

    private String getKey(String id) throws IOException{
        BufferedReader usersReader=new BufferedReader(new FileReader("/Users/lrg/users.txt"));
        String line;
        while((line=usersReader.readLine())!=null) {
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) return pair[1];
        }
        return null;
    }

    private String queryAddr(String id) throws IOException{
        BufferedReader usersReader=new BufferedReader(new FileReader("/Users/lrg/users.txt"));
        String line;
        while((line=usersReader.readLine())!=null) {
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) {
                for(int i=0;i<Users.size();i++){
                    User user=Users.get(i);
                    if(user.id.equals(id)) return user.ip+" "+String.valueOf(user.port);
                }
                return "OFFLINE";
            }
        }
        return "UNEXIST";
    }

    private void saveOfflineText(String str) throws IOException{
        BufferedWriter usersWriter = new BufferedWriter(new FileWriter("/Users/lrg/"+offlineto+".txt",true));
        usersWriter.write(offlinefrom+":"+str+"\n");
        usersWriter.close();
    }

    private void sendOfflineText(String id) throws IOException{
        String filepath="/Users/lrg/"+id+".txt";
        File file=new File(filepath);
        if(file.exists()){
            BufferedReader usersReader=new BufferedReader(new FileReader(filepath));
            String line;
            while((line=usersReader.readLine())!=null) {
                writer.println("OFFLINETEXT:"+line);
                writer.flush();
            }
            FileWriter fw=new FileWriter(file);
            fw.write("");
            fw.flush();
            fw.close();
        }
        writer.println("OFFLINEEND:end");
        writer.flush();
    }

    public static void main(String []args) throws IOException{
        int port = 10000;
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
