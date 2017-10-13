import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Dialog extends Thread{
    private Client client;
    private Socket localSoc;
    private IM uiIm;
    public int dialogId;
    private PrintWriter writer;
    private BufferedReader reader;
    private String remoteUserName;

    public Dialog(Client tClient, IM im) throws IOException{
        client=tClient;
        uiIm=im;
        localSoc=client.localSoc;
        writer = new PrintWriter(localSoc.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(localSoc.getInputStream()));
        writer.println("USERNAME:"+client.localUserName);
        writer.flush();
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
                    case "USERNAME":{
                        remoteUserName=context;
                        uiIm.showText("与"+remoteUserName+"建立连接！\n");
                        dialogId=uiIm.addDialog(remoteUserName,this);
                        break;
                    }
                    case "TEXT":{
                        uiIm.showText(remoteUserName+":"+context+"\n",dialogId);
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

    public void send(String str){
        writer.println(str);
        writer.flush();
    }

    public void close(){
        try {
            localSoc.close();
            uiIm.showText("与"+ remoteUserName +"连接结束！\n");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
