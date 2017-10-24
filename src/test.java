

public class test {
    public static void main(String []args) throws Exception{
        Packet packet=new Packet();
        packet.setFIN();
        packet.setCheckSum();
        boolean i=packet.isFIN();

    }

    public static void tmpfunc(Tmp tmp){
        tmp.a=2;
    }
}

class Tmp{
    public int a;
    Tmp(int i){
        a=i;
    }
}
