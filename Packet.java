import java.util.Arrays;

public class Packet {
    /*
    第0字节第0位标志握手，第1位标志ack，第2位fin
    第0字节第4～7位和第1字节共12位表示序号
    第2、3字节表示检验和
    第4、5字节表示数据长度
     */
    private byte[] packet;
    private int dataLength=4994;
    public Packet(){//构造一个全零的报文
        packet=new byte[6+dataLength];
        Arrays.fill(packet,(byte) 0);
    }
    public Packet(byte[] pack){
        packet=pack;
    }//使用字节数组构造报文


    public void setShake(){//设置报文为握手报文，请求连接
        packet[0]|= 0x80;
    }
    public boolean isShake(){
        return (packet[0]&0x80)==0x80;
    }//判断报文是否为握手报文
    public void setIndex(int index){//设置报文的序号
        packet[0]|=(index>>8)&0xf;
        packet[1]=(byte) (index&0xff);
    }
    public int getIndex(){//获取报文的序号
        return ((packet[0]&0xf)<<8)|(packet[1]&0xff);
    }


    public void setACK(int index){//设置报文为具有index序号的ACK
        packet[0]|=0x40;
        setIndex(index);
    }
    public int getAck(){//获取报文ACK序号，即序号
        return getIndex();
    }
    public boolean isACK(){
        return (packet[0]&0x40)==0x40;
    }//判断报文是否为ACK


    public void setData(byte[] data){//设置报文数据段
        System.arraycopy(data,0,packet,6,data.length);
        packet[4]=(byte) ((data.length>>8)&0xff);
        packet[5]=(byte) (data.length&0xff);
    }
    public int length(){//获取报文数据段实际长度
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
    public void setCheckSum(){//计算并设置报文检验和
        int sum=countSum();
        sum=~sum;
        packet[2]=(byte)((sum&0x0000ff00)>>8);
        packet[3]=(byte)sum;
    }

    public boolean checkSum(){//判断报文检验和是否正确
        int sum=countSum();
        if(sum==0xffff) return true;
        else return false;
    }

    public void setFIN(){//设置报文为断开连接报文
        packet[0]|=0x20;
    }

    public boolean isFIN(){//判断报文是否为断开连接报文
        return (packet[0]&0x20)==0x20;
    }


    public byte[] getBytes() {
        return packet;
    }//获取整个报文的二进制数据
    public byte[] getData(){//获取报文数据段的二进制数据
        byte[] tmp=new byte[length()];
        System.arraycopy(packet,6,tmp,0,length());
        return tmp;
    }
}
