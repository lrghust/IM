import java.io.*;
import java.net.Socket;

public class Dialog extends Thread{
    private Client client;
    private Socket localSoc;
    private IM uiIm;
    public int dialogId;
    private PrintWriter writer;
    private BufferedReader reader;
    private String remoteUserName;
    public boolean isOffline;
    public String sendFilePath;

    public Dialog(Client tClient, IM im) throws IOException{
        client=tClient;
        uiIm=im;
        localSoc=client.localSoc;
        writer = new PrintWriter(localSoc.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(localSoc.getInputStream()));
        writer.println("USERNAME:"+client.localUserName);
        writer.flush();
        isOffline=false;
    }

    public Dialog(Client tClient, IM im, String id) throws IOException{
        remoteUserName=id;
        client=tClient;
        uiIm=im;
        localSoc=client.localSoc;
        writer = new PrintWriter(localSoc.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(localSoc.getInputStream()));
        send("OFFLINE");
        send(client.localUserName+" "+remoteUserName);
        isOffline=true;
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
                    case "OFFLINE":{
                        uiIm.showText("向"+remoteUserName+"发送离线消息\n");
                        dialogId=uiIm.addDialog(remoteUserName,this);
                        break;
                    }
                    case "TEXT":{
                        uiIm.showText(remoteUserName+":"+context+"\n",dialogId);
                        break;
                    }
                    case "FILE":{
                        String []fileGroup=context.split(" ");
                        if(fileGroup[0].equals("FILENAME")){
                            uiIm.showText("接收文件"+fileGroup[1]+"\n",dialogId);
                            FileTrans fileTrans=new FileTrans();
                            send("FILE:PORT "+String.valueOf(fileTrans.recvPort));
                        }
                        else if(fileGroup[0].equals("PORT")){
                            String ip=localSoc.getInetAddress().getHostAddress();
                            int port=Integer.parseInt(fileGroup[1]);
                            FileTrans fileTrans=new FileTrans(ip,port,sendFilePath);
                            fileTrans.send();
                        }
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
            if(isOffline) uiIm.showText("向"+ remoteUserName +"发送离线消息结束！\n");
            else uiIm.showText("与"+ remoteUserName +"连接结束！\n");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
