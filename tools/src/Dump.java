import java.io.*;
import java.util.*;
public class Dump {
  static com.bitwig.base.serial.PPR r;
  static PrintStream out;
  static void ind(int d){ for(int i=0;i<d;i++) out.print("  "); }
  static void obj(int d) throws Exception {
    com.bitwig.base.serial.YEs c = r.Anf();
    out.println("{C"+c.beI()+(c.Gef()!=null?"("+c.Gef()+")":""));
    while(!r.Gef()){
      com.bitwig.base.serial.Mlq k=r.wGt();
      com.bitwig.base.serial.JSA t=r.MgQ();
      ind(d+1); out.print("P"+k.Gef()+(k.beI()!=null?"("+k.beI()+")":"")+" "+t.beI()+" = ");
      val(t,d+1);
      r.Heb();
    }
    r.byf();
    ind(d); out.println("}");
  }
  static void val(com.bitwig.base.serial.JSA t,int d) throws Exception {
    switch(t.name()){
      case "OBJECT": obj(d); break;
      case "NULL_OBJECT": out.println("null"); break;
      case "OBJECT_LIST": {
        out.println("[");
        r.jTs();
        while(!r.sCA()){ com.bitwig.base.serial.JSA et=r.tWW(); ind(d+1); val(et,d+1); }
        r.ZRB();
        ind(d); out.println("]"); break; }
      case "OBJECT_REFERENCE": out.println("ref#"+r.dvk()); break;
      case "RELATIVE_REFERENCE": out.println("rel#"+r.PJ3()); break;
      case "EXTERNAL_OBJECT_REFERENCE": { int k=r.mEL(); out.println("ext#"+k+(k==0?" "+r.BCC():"")); break; }
      case "STRING_TO_OBJECT_MAP": { out.println("map{"); r.IvP(); while(!r.fmw()){ String key=r.iFu(); ind(d+1); out.print(key+": "); com.bitwig.base.serial.JSA et=r.tWW(); val(et,d+1);} r.neH(); ind(d); out.println("}"); break; }
      default: Object v=t.tWW(r);
        if (v instanceof double[]) out.println(Arrays.toString((double[])v)); else if (v instanceof float[]) out.println(Arrays.toString((float[])v)); else if (v instanceof int[]) out.println(Arrays.toString((int[])v)); else if (v instanceof byte[]) out.println("byte["+((byte[])v).length+"]"); else if (v instanceof String[]) out.println(Arrays.toString((String[])v)); else if (v instanceof short[]) out.println(Arrays.toString((short[])v)); else if (v instanceof long[]) out.println(Arrays.toString((long[])v)); else if (v instanceof boolean[]) out.println(Arrays.toString((boolean[])v)); else out.println(String.valueOf(v));
    }
  }
  public static void main(String[] a) throws Exception {
    out = new PrintStream(new FileOutputStream(a[1]));
    com.bitwig.flt.file.format.FloatDeviceContentsFileFormat f = com.bitwig.flt.file.format.FloatDeviceContentsFileFormat.iMb();
    Map<String,Object> meta = new HashMap<>();
    r = f.beI(new FileInputStream(a[0]), (Map)meta);
    try { obj(0); } catch (Throwable e){ out.println("ERR "+e); System.out.println("ERR "+e); StackTraceElement[] s=e.getStackTrace(); for(int i=0;i<Math.min(6,s.length);i++) System.out.println("  at "+s[i]); }
    out.close();
  }
}
