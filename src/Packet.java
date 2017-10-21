import java.util.Arrays;

public class Packet {
    /*
    第1字节第一位标志握手，第1～7位表示序号，
    第2字节第一位表示握手ack，第1～7位表示ack号，
    第三、四字节表示检验和，
    第五、六字节表示数据长度
     */
    private byte[] packet;
    public Packet(){
        packet=new byte[1030];
        Arrays.fill(packet,(byte) 0);
    }
    public Packet(byte[] pack){
        packet=pack;
    }


    public void setShake(){
        packet[0]|=(byte) 0x80;
    }
    public boolean isShake(){
        return (packet[0]&0x80)==0x80;
    }
    public void setIndex(byte index){
        packet[0]&=0;
        packet[0]|=(byte) (0x7f&index);
    }
    public byte getIndex(){ return (byte)(packet[0]&0x7f); }


    public void setACK(byte index){
        packet[1]&=0;
        packet[1]|=(byte) 0x80;
        packet[1]|=(byte) (0x7f&index);
    }
    public int getAck(){
        return (byte)(packet[1]&0x7f);
    }
    public boolean isACK(){
        return (packet[1]&0x80)==0x80;
    }


    public void setData(byte[] data){
        System.arraycopy(data,0,packet,6,data.length);
        packet[4]=(byte) ((data.length>>8)&0xff);
        packet[5]=(byte) (data.length&0xff);
    }
    public int length(){
        int length=0;
        length=packet[4];
        length<<=8;
        length|=packet[5];
        return length;
    }
    private int countSum(){
        int sum=0;
        for(int i=0;i<packet.length;i+=2){
            int data=packet[i+1];
            data&=0x000000ff;
            sum+=data;

            data=packet[i];
            data&=0x000000ff;
            data<<=8;
            sum+=data;
            while(true) {
                int overflow = (sum & 0xffff0000) >> 16;
                if(overflow!=0) sum+=overflow;
                else break;
            }
        }
        return sum;
    }
    public void setCheckSum(){
        int sum=countSum();
        sum=~sum;
        packet[2]=(byte)((sum&0x0000ff00)>>8);
        packet[3]=(byte)sum;
    }

    public boolean checkSum(){
        int sum=countSum();
        if((short)sum==0xffff) return true;
        else return false;
    }


    public byte[] getBytes() {
        return packet;
    }
    public byte[] getData(){
        byte[] tmp=new byte[length()];
        System.arraycopy(packet,6,tmp,0,length());
        return tmp;
    }
}