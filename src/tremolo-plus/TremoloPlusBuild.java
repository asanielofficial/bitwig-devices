import java.io.*; import java.util.*; import java.nio.file.*;

/**
 * Builds devices/Tremolo+.bwdevice from tremolo_plus.nit.
 *
 * Usage: TremoloPlusBuild --reference <dir> --icons <dir> --nit <file> --out <file>
 *   --reference  folder with Orbit.bwdevice, Rivet.bwdevice, FILAR.bwdevice, Normalizer.bwdevice
 *                (used only as source of widget and atom templates; never copied into this repository)
 *   --icons      Bitwig icon folder, e.g. C:\Program Files\Bitwig Studio\resources\icons
 *
 * The panel is 36 x 53 units (53 is the fixed panel height, see docs/PANELS.md) with 3-unit margins.
 */
public class TremoloPlusBuild extends DocKit {
  static String icon(String iconsDir, String name) throws Exception {
    // Shape icons are stroke paths in black; embed them as a standalone SVG recoloured light grey.
    javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    f.setValidating(false); f.setNamespaceAware(false);
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    org.w3c.dom.Document d = f.newDocumentBuilder().parse(new File(iconsDir, "device/" + name + ".svg"));
    org.w3c.dom.Element path = (org.w3c.dom.Element) d.getElementsByTagName("path").item(0);
    String style = path.getAttribute("style").replace("#000000", "#e8e8e8");
    org.w3c.dom.Element root = d.getDocumentElement();
    String w = root.getAttribute("width"), h = root.getAttribute("height"), vb = root.getAttribute("viewBox");
    if (vb.isEmpty()) vb = "0 0 " + w + " " + h;
    return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + w + "\" height=\"" + h + "\" viewBox=\"" + vb + "\"><path d=\""
        + path.getAttribute("d") + "\" style=\"" + style + "\"/></svg>";
  }

  public static void main(String[] a) throws Exception {
    Map<String,String> arg = new HashMap<>();
    for (int i = 0; i + 1 < a.length; i += 2) arg.put(a[i], a[i + 1]);
    String ref = arg.get("--reference"), icons = arg.get("--icons"), nit = arg.get("--nit"), out = arg.get("--out");
    if (ref == null || icons == null || nit == null || out == null) { System.err.println("usage: TremoloPlusBuild --reference <dir> --icons <dir> --nit <file> --out <file>"); System.exit(2); }

    Tree.Obj rivet = load(ref + "/Rivet.bwdevice", null);
    Tree.Obj ddT = findWidget(rivet, 1714, "Octave");                       // dropdown (popup chooser)
    Tree.Obj filar = load(ref + "/FILAR.bwdevice", null);
    Tree.Obj imgT = findClass(filar, 1719);                                  // image widget
    Tree.Obj norm = load(ref + "/Normalizer.bwdevice", null);
    Tree.Obj boolT = findClass(norm, 87), toggleT = findClass(norm, 1702);   // boolean atom + toggle button
    if (ddT == null || imgT == null || boolT == null || toggleT == null) throw new RuntimeException("missing template widgets in --reference files");

    Map<String,Object> meta = new HashMap<>();
    Tree.Obj root = load(ref + "/Orbit.bwdevice", meta);                     // skeleton: audio in/out, delay compensation, panel page
    Tree.OList kids = list(root, 173);
    Tree.Obj c2000 = findFirst(kids, 2000, -1, null), c1371 = findFirst(kids, 1371, -1, null);
    Tree.Obj decTpl = findFirst(kids, 289, 701, "STRUCTURE"), enumTpl = findFirst(kids, 180, 701, "MODEL");
    Tree.Obj nitroTpl = null; for (Object i : kids.items) { Tree.Obj o = (Tree.Obj) ((Tree.Prop) i).val; if (o.cid == 1721) nitroTpl = o; }

    // transport atom (tempo/beat feed): class 1839; outputs: 0 playing, 1 beat_time, 2 beats_per_frame
    Tree.Obj transport = new Tree.Obj(); transport.cid = 1839;
    Tree.Prop bp = new Tree.Prop(); bp.pid = 6194; bp.type = "OBJECT"; Tree.Obj base = cloneObj(obj(c1371, 6194)); list(base, 614).items.clear(); bp.val = base; transport.props.add(bp);
    Tree.Prop tp = new Tree.Prop(); tp.pid = 10070; tp.type = "BOOL"; tp.val = true; transport.props.add(tp);

    // parameters
    Tree.Obj rate = decAtom(decTpl, "RATE", "Rate", 0.5), depth = decAtom(decTpl, "DEPTH", "Depth", 0.5);
    Tree.Obj shape = enumAtom(enumTpl, "SHAPE", "Shape", new String[]{"Sine","Triangle","Square","Ramp Up","Ramp Down","Sample & Hold","Sample & Slide"});
    String[] tbNames = {"Hertz","Kilohertz","Bar","Half note","Quarter note","8th note","16th note","32nd note",
        "Dotted half note","Dotted quarter note","Dotted 8th note","Dotted 16th note","Dotted 32nd note",
        "Triplet half note","Triplet quarter note","Triplet 8th note","Triplet 16th note","Triplet 32nd note",
        "Pitch (of current note)","Hold (rate of zero)"};
    String[] tbKeys = {"unit-hz","unit-khz","unit-bar","note-2","note-4","note-8","note-16","note-32","note-2d","note-4d","note-8d","note-16d","note-32d",
        "note-2t","note-4t","note-8t","note-16t","note-32t","tune","note-hold"};
    String[] tbIcons = new String[tbKeys.length];
    for (int q = 0; q < tbKeys.length; q++) tbIcons[q] = tbKeys[q] + ".svg";   // enum entry icons are "<name>.svg" (see docs/PANELS.md)
    Tree.Obj tbase = enumAtom(enumTpl, "TIMEBASE", "Time Base", tbNames, tbIcons);
    Tree.Obj retrig = cloneObj(boolT); set(retrig, 701, "RETRIGGER"); set(retrig, 374, "Retrigger"); set(retrig, 821, "R"); set(obj(retrig, 702), 6957, false);

    // proxies: write P177 (input proxies) before P173 so every reference points backwards
    Tree.OList p177 = list(root, 177);
    Tree.Obj noteProxy = (Tree.Obj) ((Tree.Prop) p177.items.get(1)).val;
    Tree.Prop p177a = (Tree.Prop) p177.items.get(0); Tree.Obj audioProxy = ((Tree.Ref) p177a.val).target;
    Tree.Obj c2000c = (Tree.Obj) ((Tree.Prop) list(obj(c2000, 6194), 614).items.get(0)).val;
    Tree.Prop srcProp = get(c2000c, 248); if (srcProp.val != audioProxy) throw new RuntimeException("unexpected skeleton: audio proxy not embedded in C2000 connection");
    Tree.Ref ar = new Tree.Ref(); ar.target = audioProxy; srcProp.type = "OBJECT_REFERENCE"; srcProp.val = ar;
    p177a.type = "OBJECT"; p177a.val = audioProxy;
    Tree.Prop p177p = get(root, 177); root.props.remove(p177p); root.props.add(0, p177p);

    // Nitro module: ports in declaration order, one connection per input
    Tree.Obj nitm = cloneObj(nitroTpl);
    set(nitm, 10421, compileNitro(new String(Files.readAllBytes(Paths.get(nit)), "UTF-8"), "tremolo_plus"));
    Tree.OList ins = list(nitm, 15756), outs = list(nitm, 15757);
    Tree.Obj ptpl = (Tree.Obj) ((Tree.Prop) ins.items.get(0)).val;
    ins.items.clear(); outs.items.clear();
    // {name, width, class(1 audio, 2 event, 3 value), isInput, type string, event kind, source, source port}
    Object[][] I = {
      {"in", 2, 1, true, "", 0, c1371, 0}, {"rate", 1, 1, true, "", 0, rate, 0}, {"depth", 1, 1, true, "", 0, depth, 0},
      {"beats_per_frame", 1, 1, true, "", 0, transport, 2},
      {"shape", 1, 3, true, "", 0, shape, 0}, {"timebase", 1, 3, true, "", 0, tbase, 0}, {"retrigger", 1, 3, true, "", 0, retrig, 0},
      {"note_in", 0, 2, true, "{i32, i32, i32, float, i32, i32, i32, [ 16 x i8 ]}", 44, noteProxy, 0} };
    Tree.OList conns = list(obj(nitm, 6194), 614); conns.items.clear();
    for (int i = 0; i < I.length; i++) { Object[] e = I[i];
      ins.items.add(itemProp(port(ptpl, (String) e[0], (Integer) e[1], i, (Integer) e[2], (Boolean) e[3], (String) e[4], (Integer) e[5])));
      conns.items.add(itemProp(conn((Tree.Obj) e[6], (Integer) e[7]))); }
    outs.items.add(itemProp(port(ptpl, "out", 2, 0, 1, false, "", 0)));

    kids.items.clear();
    for (Tree.Obj o : new Tree.Obj[]{c2000, c1371, transport, rate, depth, shape, tbase, retrig, nitm}) kids.items.add(itemProp(o));
    Tree.Obj c50 = (Tree.Obj) ((Tree.Prop) list(root, 178).items.get(0)).val;           // device audio output proxy
    Tree.Obj c50c = (Tree.Obj) ((Tree.Prop) list(obj(c50, 6194), 614).items.get(0)).val; ((Tree.Ref) val(c50c, 248)).target = nitm; set(c50c, 249, (byte) 0);

    // panel
    Tree.Obj page = (Tree.Obj) ((Tree.Prop) list(root, 6213).items.get(0)).val;
    Tree.Obj content = obj(page, 6212); Tree.OList ws = list(content, 6221);
    Tree.Obj knobT = null, segT = null, labT = null;
    for (Object i : ws.items) { Tree.Obj w = (Tree.Obj) ((Tree.Prop) i).val; if (w.cid == 1687 && knobT == null) knobT = w; if (w.cid == 1712 && segT == null) segT = w; if (w.cid == 1683 && labT == null) labT = w; }
    ws.items.clear();
    // Grid: page 36 x 53, margins 3. Left column x=3..17, gap 2, right column x=19..33; both span y=3..50.
    // Right column rhythm: 5 | 1 | 14 | 1 | 5 | 1 | 14 | 1 | 5
    List<Tree.Obj> W = new ArrayList<>();
    Tree.Obj shapeW = widget(segT, 3, 3, 14, 47, shape, "LFO shape"); set(shapeW, 6255, false); set(shapeW, 6257, (byte) 1);   // vertical, icon style
    W.add(shapeW);
    String[] shapeKeys = {"sine","triangle","square","ramp_up","ramp_down","sample_and_hold","sample_and_slide_cubic"};
    for (int q = 0; q < 7; q++) { int y0 = 3 + (int) Math.round(47.0 * q / 7.0), y1 = 3 + (int) Math.round(47.0 * (q + 1) / 7.0);
      W.add(image(imgT, 3, y0, 14, y1 - y0, icon(icons, shapeKeys[q]))); }                                                       // icon overlays on the buttons
    Tree.Obj tg = cloneObj(toggleT); setLayout(tg, 22, 3, 8, 5); Tree.Ref tr = new Tree.Ref(); tr.target = retrig; get(tg, 6220).val = tr;
    set(tg, 6867, "Retrigger the LFO on every note"); set(tg, 6241, "R"); W.add(tg);
    W.add(widget(knobT, 19, 9, 14, 14, rate, "LFO rate (multiplies the selected time base)"));
    Tree.Obj tbW = widget(ddT, 19, 24, 14, 5, tbase, "Time base: Hz, kHz, bar, note values, pitch of current note, or hold"); set(tbW, 6262, (byte) 1); W.add(tbW);   // icon-only preview
    W.add(widget(knobT, 19, 30, 14, 14, depth, "Tremolo depth"));
    W.add(label(labT, 19, 45, 14, 5, "DEPTH"));
    for (Tree.Obj w : W) ws.items.add(itemProp(w));
    set(page, 6232, (short) 36); set(page, 6233, (byte) 53);

    // metadata
    String uuid = UUID.nameUUIDFromBytes("tremolo-plus-custom-device".getBytes()).toString();
    String desc = "Tremolo with seven LFO shapes, free or tempo-synced rate, depth and note retrigger";
    String tags = "tremolo lfo amplitude modulation tempo sync retrigger";
    set(root, 385, UUID.fromString(uuid)); set(root, 386, "Tremolo+"); set(root, 2057, desc); set(root, 389, tags); get(root, 387).val = "Custom";
    meta.put("device_name", "Tremolo+"); meta.put("preset_name", "Tremolo+"); meta.put("device_id", uuid); meta.put("device_uuid", uuid);
    meta.put("device_description", desc); meta.put("comment", ""); meta.put("creator", "Custom"); meta.put("device_creator", "Custom"); meta.put("tags", tags);
    meta.put("application_version_name", "6.1.7"); meta.put("has_note_input", Boolean.TRUE);
    meta.put("revision_id", UUID.nameUUIDFromBytes(("rev" + uuid).getBytes()).toString().replace("-", "") + "00000000");
    write(root, meta, out);
  }
}
