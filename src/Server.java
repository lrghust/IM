import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Server extends Thread {
    private ServerSocket serverSocket;
    private Socket serverSoc;
    private BufferedReader reader;
    private PrintWriter writer;

    private enum State{wait, login, signup, findkey};

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while(true){
            serverSoc = serverSocket.accept();
            Thread tServer=new Server(serverSoc);
            tServer.start();
        }
    }

    public Server(Socket tServerSoc) throws IOException{
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
                            default:
                                break;
                        }
                        break;
                    }
                    case login: {
                        String[] pair = inStr.split(" ");
                        int check = checkPair(pair[0], pair[1]);
                        if (check == 0) outStr = "LOGIN:ok";
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
                    case findkey:
                        String key=getKey(inStr);
                        if(key==null) outStr="FINDKEY:errorid";
                        else outStr="FINDKEY:"+key;
                        writer.println(outStr);
                        writer.flush();
                        state=State.wait;
                        break;
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

    private String getKey(String id) throws IOException{
        BufferedReader usersReader=new BufferedReader(new FileReader("/Users/lrg/users.txt"));
        String line;
        while((line=usersReader.readLine())!=null) {
            String[] pair = line.split(" ");
            if(id.equals(pair[0])) return pair[1];
        }
        return null;
    }

    public static void main(String []args) throws IOException{
        int port = 10000;
        Server server = new Server(port);
    }
}
