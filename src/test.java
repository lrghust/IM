import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class test {
    public static void main(String []args) throws Exception{
        LinkedList<Integer> list=new LinkedList<>();
        list.offer(1);
        list.offer(2);
        list.offer(3);
        ListIterator<Integer> iter=list.listIterator();
        iter.next();
        iter.remove();
        System.out.println(list.get(0));

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
