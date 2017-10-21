
public class test {
    public static void main(String []args) throws Exception{
        byte[] tmp=new byte[1024];
        receive(tmp);
        System.out.println(tmp[100]);
    }

    private static void receive(byte[] arr){
        byte[] tmp=new byte[3];
        tmp[0]=(byte)0;
        tmp[1]=(byte)0;
        tmp[2]=(byte)0;
        System.arraycopy(tmp,0,arr,0,3);
    }
}
