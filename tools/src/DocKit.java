import java.io.*; import java.util.*; import java.nio.file.*;

/** Helpers for reading, editing and writing Bitwig device documents (see docs/FILE-FORMAT.md). */
public class DocKit {
  static Tree.Prop get(Tree.Obj o,int pid){ for(Tree.Prop p:o.props) if(p.pid==pid) return p; return null; }
  static Object val(Tree.Obj o,int pid){ Tree.Prop p=get(o,pid); return p==null?null:p.val; }
  static void set(Tree.Obj o,int pid,Object v){ Tree.Prop p=get(o,pid); if(p==null) throw new RuntimeException("no prop "+pid+" on C"+o.cid); p.val=v; }
  static Tree.Obj obj(Tree.Obj o,int pid){ return (Tree.Obj)val(o,pid); }
  static Tree.OList list(Tree.Obj o,int pid){ return (Tree.OList)val(o,pid); }
  static Tree.Prop itemProp(Tree.Obj x){ Tree.Prop p=new Tree.Prop(); p.type="OBJECT"; p.val=x; return p; }

  // deep clone (refs keep their target)
  static Object cloneVal(Object v){
    if(v instanceof Tree.Obj) return cloneObj((Tree.Obj)v);
    if(v instanceof Tree.OList){ Tree.OList n=new Tree.OList(); for(Object i:((Tree.OList)v).items){ Tree.Prop ip=(Tree.Prop)i; Tree.Prop c=new Tree.Prop(); c.pid=ip.pid;c.pname=ip.pname;c.type=ip.type;c.val=cloneVal(ip.val); n.items.add(c);} return n; }
    if(v instanceof Tree.Ref){ Tree.Ref r=new Tree.Ref(); r.id=((Tree.Ref)v).id; r.target=((Tree.Ref)v).target; return r; }
    if(v instanceof byte[]) return ((byte[])v).clone();
    return v;
  }
  static Tree.Obj cloneObj(Tree.Obj o){ Tree.Obj n=new Tree.Obj(); n.cid=o.cid; n.cname=o.cname; for(Tree.Prop p:o.props){ Tree.Prop c=new Tree.Prop(); c.pid=p.pid;c.pname=p.pname;c.type=p.type;c.val=cloneVal(p.val); n.props.add(c);} return n; }

  // preorder over all objects in write order
  static void pre(Object v, List<Tree.Obj> out){
    if(v instanceof Tree.Obj){ Tree.Obj o=(Tree.Obj)v; out.add(o); for(Tree.Prop p:o.props) pre(p.val,out); }
    else if(v instanceof Tree.OList){ for(Object i:((Tree.OList)v).items) pre(((Tree.Prop)i).val,out); }
  }
  static void visitRefs(Object v, java.util.function.Consumer<Tree.Ref> f){
    if(v instanceof Tree.Obj){ for(Tree.Prop p:((Tree.Obj)v).props) visitRefs(p.val,f); }
    else if(v instanceof Tree.OList){ for(Object i:((Tree.OList)v).items) visitRefs(((Tree.Prop)i).val,f); }
    else if(v instanceof Tree.Ref) f.accept((Tree.Ref)v);
  }
  static void resolve(Tree.Obj root){ List<Tree.Obj> l=new ArrayList<>(); pre(root,l); visitRefs(root, r->{ r.target=l.get(r.id-1); }); }
  static void renumber(Tree.Obj root){
    List<Tree.Obj> l=new ArrayList<>(); pre(root,l);
    IdentityHashMap<Tree.Obj,Integer> idx=new IdentityHashMap<>(); for(int i=0;i<l.size();i++) idx.put(l.get(i),i+1);
    visitRefs(root, r->{ Integer i=idx.get(r.target); if(i==null) throw new RuntimeException("dangling ref to C"+(r.target==null?-1:r.target.cid)); if(i>=idx.get(r.target)&&false){} r.id=i; });
  }

  static Tree.Obj findWidget(Object v,int cid,String tip){
    if(v instanceof Tree.Obj){ Tree.Obj o=(Tree.Obj)v; if(o.cid==cid && tip.equals(val(o,6867))) return o; for(Tree.Prop p:o.props){ Tree.Obj r=findWidget(p.val,cid,tip); if(r!=null) return r; } }
    else if(v instanceof Tree.OList){ for(Object i:((Tree.OList)v).items){ Tree.Obj r=findWidget(((Tree.Prop)i).val,cid,tip); if(r!=null) return r; } }
    return null; }
  static Tree.Obj findClass(Object v,int cid){
    if(v instanceof Tree.Obj){ Tree.Obj o=(Tree.Obj)v; if(o.cid==cid) return o; for(Tree.Prop p:o.props){ Tree.Obj r=findClass(p.val,cid); if(r!=null) return r; } }
    else if(v instanceof Tree.OList){ for(Object i:((Tree.OList)v).items){ Tree.Obj r=findClass(((Tree.Prop)i).val,cid); if(r!=null) return r; } }
    return null; }
  static Tree.Obj image(Tree.Obj tpl,int x,int y,int w,int h,String svgText){ Tree.Obj k=cloneObj(tpl); setLayout(k,x,y,w,h); set(k,6259,svgText); return k; }
  static Tree.Obj conn(Tree.Obj target,int port){
    Tree.Obj c=new Tree.Obj(); c.cid=105;
    Tree.Prop a=new Tree.Prop(); a.pid=248;a.type="OBJECT_REFERENCE"; Tree.Ref r=new Tree.Ref(); r.target=target; a.val=r; c.props.add(a);
    Tree.Prop b=new Tree.Prop(); b.pid=249;b.type="BYTE"; b.val=(byte)port; c.props.add(b);
    Tree.Prop d=new Tree.Prop(); d.pid=6443;d.type="DOUBLE"; d.val=0.0; c.props.add(d);
    return c;
  }
  static Tree.Obj findFirst(Tree.OList l,int cid,int idPid,String idVal){
    for(Object i:l.items){ Tree.Obj o=(Tree.Obj)((Tree.Prop)i).val; if(o.cid==cid && (idPid<0 || idVal.equals(val(o,idPid)))) return o; } return null; }
  static Tree.Obj enumAtom(Tree.Obj tpl,String id,String name,String[] labels){ return enumAtom(tpl,id,name,labels,null); }
  static Tree.Obj enumAtom(Tree.Obj tpl,String id,String name,String[] labels,String[] icons){
    Tree.Obj a=cloneObj(tpl); set(a,701,id); set(a,374,name); set(a,821,name);
    Tree.Obj vt=obj(a,702); Tree.OList ents=list(vt,393); Tree.Obj e0=(Tree.Obj)((Tree.Prop)ents.items.get(0)).val; ents.items.clear();
    for(int i=0;i<labels.length;i++){ Tree.Obj e=cloneObj(e0); set(e,650,(byte)i); set(e,651,labels[i]); if(icons!=null) set(e,9823,icons[i]); ents.items.add(itemProp(e)); }
    set(a,457,(byte)0); return a; }
  static Tree.Obj decAtom(Tree.Obj tpl,String id,String name,double def){
    Tree.Obj a=cloneObj(tpl); set(a,701,id); set(a,374,name); set(a,821,name); set(obj(a,702),891,def); set(a,712,def); return a; }
  static Tree.Obj port(Tree.Obj tpl,String name,int width,int idx,int cls,boolean in,String type,int evt){
    Tree.Obj p=cloneObj(tpl); set(p,8557,name); set(p,8559,(byte)width); set(p,8560,(byte)idx); set(p,8561,(byte)cls); set(p,8562,in); set(p,8565,(byte)evt); set(p,8566,type); return p; }
  static void setLayout(Tree.Obj w,int x,int y,int ww,int hh){ Tree.Obj r=obj(w,6226); set(r,6215,(byte)x); set(r,6216,(byte)y); set(r,6217,(short)ww); set(r,6218,(byte)hh); }
  static Tree.Obj widget(Tree.Obj tpl,int x,int y,int w,int h,Tree.Obj atom,String tip){ Tree.Obj k=cloneObj(tpl); setLayout(k,x,y,w,h); Tree.Ref r=new Tree.Ref(); r.target=atom; get(k,6220).val=r; set(k,6867,tip); return k; }
  static Tree.Obj label(Tree.Obj tpl,int x,int y,int w,int h,String text){ Tree.Obj k=cloneObj(tpl); setLayout(k,x,y,w,h); set(k,6219,text); return k; }


  /** Read a device file contents tree; object references are resolved to Obj targets. */
  public static Tree.Obj load(String path, Map<String,Object> metaOut) throws Exception {
    com.bitwig.flt.file.format.FloatDeviceContentsFileFormat f = com.bitwig.flt.file.format.FloatDeviceContentsFileFormat.iMb();
    Tree.r = f.beI(new FileInputStream(path), (Map)(metaOut==null?new HashMap<String,Object>():metaOut));
    Tree.Obj root=Tree.readObj(); resolve(root); return root;
  }
  /** Number references, verify there are no forward references, and write the device file with Bitwig own writer. */
  public static void write(Tree.Obj root, Map<String,Object> meta, String out) throws Exception {
    renumber(root);
    List<Tree.Obj> pl=new ArrayList<>(); pre(root,pl);
    IdentityHashMap<Tree.Obj,Integer> pos=new IdentityHashMap<>(); for(int i=0;i<pl.size();i++) pos.put(pl.get(i),i);
    int[] fwd={0}; visitOwner(root,null,pos,fwd);
    if(fwd[0]>0) throw new RuntimeException(fwd[0]+" forward reference(s); reorder properties so owners are written first");
    System.out.println("objects="+pl.size()+" forward refs=0");
    // The revision id is the SHA-1 of the written device, so every content change gets a new id (Bitwig can then
    // tell a rebuilt device from the one it already indexed) and identical input gives an identical id.
    byte[] first=writeBytes(root,meta);
    String rev=new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-1").digest(first)).toString(16);
    while(rev.length()<40) rev="0"+rev;
    meta.put("revision_id", rev);
    byte[] data=writeBytes(root,meta);
    Files.write(Paths.get(out), data);
    System.out.println("wrote "+out+" ("+data.length+" bytes, revision "+rev.substring(0,8)+")");
  }
  static byte[] writeBytes(Tree.Obj root, Map<String,Object> meta) throws Exception {
    com.bitwig.flt.file.format.FloatDeviceContentsFileFormat f = com.bitwig.flt.file.format.FloatDeviceContentsFileFormat.iMb();
    ByteArrayOutputStream bo=new ByteArrayOutputStream();
    final Tree.Obj froot=root;
    f.beI((com.bitwig.base.serial.odK) w -> Tree.writeObj(w, froot), bo, (Map)meta);
    return bo.toByteArray();
  }
  /** Compile Nitro source with Bitwig own parser into the blob stored in a Nitro module (property 10421): 0x00 + raw deflate. */
  public static byte[] compileNitro(String source, String name) throws Exception {
    com.bitwig.nitro.NitroFile nf=com.bitwig.nitro.NitroFile.beI(new StringReader(source), name);
    byte[] raw=nf.MgQ(); byte[] blob=new byte[raw.length+1]; System.arraycopy(raw,0,blob,1,raw.length); return blob;
  }
  static void visitRefsWithOwner(Object v, IdentityHashMap<Tree.Obj,Integer> pos, int[] cnt){
    visitOwner(v,null,pos,cnt); }
  static void visitOwner(Object v, Tree.Obj owner, IdentityHashMap<Tree.Obj,Integer> pos, int[] cnt){
    if(v instanceof Tree.Obj){ Tree.Obj o=(Tree.Obj)v; for(Tree.Prop p:o.props) visitOwner(p.val,o,pos,cnt); }
    else if(v instanceof Tree.OList){ for(Object i:((Tree.OList)v).items) visitOwner(((Tree.Prop)i).val,owner,pos,cnt); }
    else if(v instanceof Tree.Ref){ if(pos.get(((Tree.Ref)v).target)>pos.get(owner)){ cnt[0]++; System.out.println(" forward ref from C"+owner.cid+" to C"+((Tree.Ref)v).target.cid); } }
  }
}
