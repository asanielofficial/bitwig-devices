import java.io.*;
import java.util.*;
public class Tree {
  static class Prop { int pid; String pname; String type; Object val; }
  static class Obj { int cid; String cname; List<Prop> props = new ArrayList<>(); }
  static class OList { List<Object> items = new ArrayList<>(); }
  static class Ref { int id; Obj target; }

  // ---------- read ----------
  static com.bitwig.base.serial.PPR r;
  static Obj readObj() throws Exception {
    com.bitwig.base.serial.YEs c = r.Anf();
    Obj o = new Obj(); o.cid=c.beI(); o.cname=c.Gef();
    while(!r.Gef()){
      com.bitwig.base.serial.Mlq k=r.wGt();
      com.bitwig.base.serial.JSA t=r.MgQ();
      Prop p=new Prop(); p.pid=k.Gef(); p.pname=k.beI(); p.type=t.name(); p.val=readVal(t);
      o.props.add(p);
      r.Heb();
    }
    r.byf();
    return o;
  }
  static Object readVal(com.bitwig.base.serial.JSA t) throws Exception {
    switch(t.name()){
      case "OBJECT": return readObj();
      case "NULL_OBJECT": return null;
      case "OBJECT_LIST": { OList l=new OList(); r.jTs(); while(!r.sCA()){ com.bitwig.base.serial.JSA et=r.tWW(); Prop ip=new Prop(); ip.type=et.name(); ip.val=readVal(et); l.items.add(ip); } r.ZRB(); return l; }
      case "OBJECT_REFERENCE": { Ref x=new Ref(); x.id=r.dvk(); return x; }
      default: return t.tWW(r);
    }
  }
  // ---------- write ----------
  static void writeObj(com.bitwig.base.serial.SJF w, Obj o) {
    w.beI(o.cname!=null && o.cid==0 ? new com.bitwig.base.serial.YEs(o.cname) : new com.bitwig.base.serial.YEs(o.cid));
    for (Prop p: o.props){
      w.beI(p.pname!=null && p.pid==0 ? new com.bitwig.base.serial.Mlq(p.pname) : new com.bitwig.base.serial.Mlq(p.pid));
      writeVal(w,p);
    }
    w.BBg();
  }
  static void writeVal(com.bitwig.base.serial.SJF w, Prop p){
    Object v=p.val;
    switch(p.type){
      case "BYTE": w.beI(((Number)v).intValue()); break;
      case "INT16": w.beI(((Number)v).intValue()); break;
      case "INT32": w.beI(((Number)v).intValue()); break;
      case "BOOL": w.beI(((Boolean)v).booleanValue()); break;
      case "DOUBLE": w.beI(((Double)v).doubleValue()); break;
      case "STRING": w.beI((String)v); break;
      case "COLOR": w.beI((SZB)v); break;
      case "UUID": w.beI((java.util.UUID)v); break;
      case "BYTE_ARRAY": w.beI((byte[])v); break;
      case "NULL_OBJECT": w.unz(); break;
      case "OBJECT": writeObj(w,(Obj)v); break;
      case "OBJECT_REFERENCE": w.Gef(((Ref)v).id); break;
      case "OBJECT_LIST": { w.byf(); for(Object it: ((OList)v).items) writeVal(w,(Prop)it); w.Bqk(); break; }
      default: throw new RuntimeException("unsupported "+p.type);
    }
  }
  public static void main(String[] a) throws Exception {
    com.bitwig.flt.file.format.FloatDeviceContentsFileFormat f = com.bitwig.flt.file.format.FloatDeviceContentsFileFormat.iMb();
    Map<String,Object> meta = new HashMap<>();
    r = f.beI(new FileInputStream(a[0]), (Map)meta);
    Obj root = readObj();
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    f.beI((com.bitwig.base.serial.odK) w -> writeObj(w, root), bo, (Map)meta);
    byte[] out = bo.toByteArray();
    byte[] orig = java.nio.file.Files.readAllBytes(new File(a[0]).toPath());
    int n=Math.min(out.length,orig.length), diff=-1;
    for(int i=0;i<n;i++) if(out[i]!=orig[i]){diff=i;break;}
    System.out.println(new File(a[0]).getName()+" orig="+orig.length+" out="+out.length+" firstdiff="+diff);
    if (a.length>1) java.nio.file.Files.write(new File(a[1]).toPath(), out);
  }
}
