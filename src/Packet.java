import java.util.Arrays;

public class Packet {
    /*
    第0字节第0位标志握手，第1位标志ack，第2位fin
    第0字节第4～7位和第1字节共12位表示序号
    第2、3字节表示检验和
    第4、5字节表示数据长度
     */
    private byte[] packet;
    private int dataLength=4094;
    public Packet(){
        packet=new byte[6+dataLength];
        Arrays.fill(packet,(byte) 0);
    }
    public Packet(byte[] pack){
        packet=pack;
    }


    public void setShake(){
        packet[0]|= 0x80;
    }
    public boolean isShake(){
        return (packet[0]&0x80)==0x80;
    }
    public void setIndex(int index){
        packet[0]|=(index>>8)&0xf;
        packet[1]=(byte) (index&0xff);
    }
    public int getIndex(){
        return ((packet[0]&0xf)<<8)|(packet[1]&0xff);
    }


    public void setACK(int index){
        packet[0]|=0x40;
        setIndex(index);
    }
    public int getAck(){
        return getIndex();
    }
    public boolean isACK(){
        return (packet[0]&0x40)==0x40;
    }


    public void setData(byte[] data){
        System.arraycopy(data,0,packet,6,data.length);
        packet[4]=(byte) ((data.length>>8)&0xff);
        packet[5]=(byte) (data.length&0xff);
    }
    public int length(){
        int length=packet[4];
        length<<=8;
        length|=packet[5]&0xff;
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
                int overflow = (sum>>16) & 0x0000ffff;
                if(overflow!=0) {
                    sum+=overflow;
                    sum&=0xffff;
                }
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
        if(sum==0xffff) return true;
        else return false;
    }

    public void setFIN(){
        packet[0]|=0x20;
    }

    public boolean isFIN(){
        return (packet[0]&0x20)==0x20;
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
