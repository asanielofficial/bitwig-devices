import java.io.*; import java.util.*; import java.nio.file.*;

/**
 * Builds devices/ASD Vibrato.bwdevice from asd_vibrato.nit.
 *
 * Usage: AsdVibratoBuild --reference <dir> --icons <dir> --nit <file> --out <file>
 *   --reference  folder with Orbit.bwdevice, Rivet.bwdevice, FILAR.bwdevice, Normalizer.bwdevice (templates only)
 *   --icons      Bitwig icon folder, e.g. C:\Program Files\Bitwig Studio\resources\icons
 *
 * Panel: 122 x 53 units (53 is the fixed panel height), 3-unit margins. Left block: fixed vibrato curve (shader) and the
 * seven shape buttons; right block: two 19-wide columns (Rate / Depth, Time base / R, Stereo / Mix).
 */
public class AsdVibratoBuild extends DocKit {
  /** Live vibrato curve: one cycle of the shape scaled by depth (cents), the right-channel curve, and a cursor at the LFO phase, starting at the far left. Design space 420 x 204. */
  static final String CURVE_SHADER = String.join("\n",
    "// ASD Vibrato live curve. Design space: 420 x 204. One LFO cycle spans the plot; the cursor starts at the far left.",
    "// audioLow: shape index, depth (0..1), LFO phase (cycles), stereo offset (cycles).",
    "// audioMedium: held random values (two segments ago, previous, current).",
    "uniform float4 audioLow;",
    "uniform float4 audioMedium;",
    "",
    "float coverage(float distance, float pixel) {",
    "    return 1. - smoothstep(-pixel * .65, pixel * .65, distance);",
    "}",
    "",
    "float shapeValue(float shape, float u) {",
    "    float seg = floor(u);",
    "    float t = u - seg;",
    "    if (shape > 4.5) {",
    "        float sv = t * t * (3. - 2. * t);",
    "        if (shape < 5.5) {",
    "            if (seg < -.5) return audioMedium.y;",
    "            return audioMedium.z;",
    "        }",
    "        if (seg < -.5) return mix(audioMedium.x, audioMedium.y, sv);",
    "        return mix(audioMedium.y, audioMedium.z, sv);",
    "    }",
    "    if (shape < .5) return sin(6.2831853 * t);",
    "    if (shape < 1.5) {",
    "        if (t < .25) return 4. * t;",
    "        if (t < .75) return 2. - 4. * t;",
    "        return 4. * t - 4.;",
    "    }",
    "    if (shape < 2.5) {",
    "        if (t < .5) return 1.;",
    "        return -1.;",
    "    }",
    "    if (shape < 3.5) return 2. * t - 1.;",
    "    return 1. - 2. * t;",
    "}",
    "",
    "float curveY(float shape, float depth, float u) {",
    "    return 102. - 70. * depth * shapeValue(shape, u);",
    "}",
    "",
    "float curveDist(float shape, float depth, float lag, float2 p) {",
    "    float u1 = (p.x - 1. - 20.) / 380. - lag;",
    "    float u2 = (p.x + 1. - 20.) / 380. - lag;",
    "    float y1 = curveY(shape, depth, u1);",
    "    float y2 = curveY(shape, depth, u2);",
    "    float ym = (y1 + y2) * .5;",
    "    float span = abs(y2 - y1) * .5;",
    "    float s = min(span, 3.);",
    "    return max(abs(p.y - ym) - span, 0.) / length(float2(1., s));",
    "}",
    "",
    "half4 main(float2 fragCoord) {",
    "    float2 p = fragCoord / iResolution.xy * float2(420., 204.);",
    "    float pixel = max(420. / iResolution.x, 204. / iResolution.y);",
    "    float shape = audioLow.x;",
    "    float depth = clamp(audioLow.y, 0., 1.);",
    "    float phase = audioLow.z;",
    "    float lag = audioLow.w;",
    "    float3 well = float3(0., 0., 0.);",
    "    float3 grid = float3(.12, .12, .12);",
    "    float3 axis = float3(.27, .27, .27);",
    "    float3 orange = float3(1., .478, .102);",
    "    float3 col = well;",
    "    float inside = step(20., p.x) * step(p.x, 400.);",
    "    float vx = abs(mod(p.x - 20. + 47.5, 95.) - 47.5);",
    "    float hy = abs(mod(p.y - 16. + 21.5, 43.) - 21.5);",
    "    col = mix(col, grid, coverage(vx - .5, pixel) * inside * step(16., p.y) * step(p.y, 188.));",
    "    col = mix(col, grid, coverage(hy - .5, pixel) * inside);",
    "    col = mix(col, axis, coverage(abs(p.y - 102.) - .7, pixel) * inside);",
    "    float yl = curveY(shape, depth, (p.x - 20.) / 380.);",
    "    float lo = min(yl, 102.);",
    "    float hi = max(yl, 102.);",
    "    col = mix(col, orange, step(lo, p.y) * step(p.y, hi) * inside * .12);",
    "    float rightOn = step(.005, lag);",
    "    col = mix(col, float3(.85, .85, .85), coverage(curveDist(shape, depth, lag, p) - 1.1, pixel) * inside * rightOn * .55);",
    "    col = mix(col, orange, coverage(curveDist(shape, depth, 0., p) - 1.7, pixel) * inside);",
    "    float cx = 20. + 380. * phase;",
    "    col = mix(col, float3(1., 1., 1.), coverage(abs(p.x - cx) - .6, pixel) * step(16., p.y) * step(p.y, 188.) * .3);",
    "    float cy = curveY(shape, depth, phase);",
    "    col = mix(col, float3(1., 1., 1.), coverage(length(p - float2(cx, cy)) - 4.5, pixel));",
    "    return half4(col, 1.);",
    "}",
    "");

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
    if (ref == null || icons == null || nit == null || out == null) { System.err.println("usage: AsdVibratoBuild --reference <dir> --icons <dir> --nit <file> --out <file>"); System.exit(2); }

    Tree.Obj rivet = load(ref + "/Rivet.bwdevice", null);
    Tree.Obj ddT = findWidget(rivet, 1714, "Octave");                       // dropdown (popup chooser)
    Tree.Obj filar = load(ref + "/FILAR.bwdevice", null);
    Tree.Obj imgT = findClass(filar, 1719);                                  // image widget
    Tree.Obj norm = load(ref + "/Normalizer.bwdevice", null);
    Tree.Obj boolT = findClass(norm, 87), toggleT = findClass(norm, 1702);   // boolean atom + toggle button
    if (ddT == null || imgT == null || boolT == null || toggleT == null) throw new RuntimeException("missing template widgets in --reference files");

    Map<String,Object> meta = new HashMap<>();
    Tree.Obj root = load(ref + "/Orbit.bwdevice", meta);                     // skeleton: audio in/out, delay compensation, panel page, shader
    Tree.OList kids = list(root, 173);
    Tree.Obj c2000 = findFirst(kids, 2000, -1, null), c1371 = findFirst(kids, 1371, -1, null);
    Tree.Obj decTpl = findFirst(kids, 289, 701, "STRUCTURE"), enumTpl = findFirst(kids, 180, 701, "MODEL");
    Tree.Obj drawing = findFirst(kids, 5512, -1, null);
    Tree.Obj nitroTpl = null; for (Object i : kids.items) { Tree.Obj o = (Tree.Obj) ((Tree.Prop) i).val; if (o.cid == 1721) nitroTpl = o; }

    // live curve: reuse Orbit's drawing component with the vibrato shader. Input 0 (audioLow) is fed by the Nitro "display" output and
    // input 1 (audioMedium) by "display_hold"; the seven other inputs stay disconnected (see docs/PANELS.md).
    set(drawing, 701, "ASD_VIBRATO_DRAWING"); set(drawing, 16316, CURVE_SHADER);

    // transport atom (tempo/beat feed): class 1839; outputs: 0 playing, 1 beat_time, 2 beats_per_frame
    Tree.Obj transport = new Tree.Obj(); transport.cid = 1839;
    Tree.Prop bp = new Tree.Prop(); bp.pid = 6194; bp.type = "OBJECT"; Tree.Obj base = cloneObj(obj(c1371, 6194)); list(base, 614).items.clear(); bp.val = base; transport.props.add(bp);
    Tree.Prop tp = new Tree.Prop(); tp.pid = 10070; tp.type = "BOOL"; tp.val = true; transport.props.add(tp);

    // parameters
    Tree.Obj rate = decAtom(decTpl, "RATE", "Rate", 0.5), depth = decAtom(decTpl, "DEPTH", "Depth", 0.25);
    Tree.Obj stereo = decAtom(decTpl, "STEREO", "Stereo", 0.0), mix = decAtom(decTpl, "MIX", "Mix", 1.0);
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
    set(nitm, 10421, compileNitro(new String(Files.readAllBytes(Paths.get(nit)), "UTF-8"), "asd_vibrato"));
    Tree.OList ins = list(nitm, 15756), outs = list(nitm, 15757);
    Tree.Obj ptpl = (Tree.Obj) ((Tree.Prop) ins.items.get(0)).val;
    ins.items.clear(); outs.items.clear();
    // {name, width, class(1 audio, 2 event, 3 value), isInput, type string, event kind, source, source port}
    Object[][] I = {
      {"in", 2, 1, true, "", 0, c1371, 0}, {"rate", 1, 1, true, "", 0, rate, 0}, {"depth", 1, 1, true, "", 0, depth, 0},
      {"stereo", 1, 1, true, "", 0, stereo, 0}, {"mix", 1, 1, true, "", 0, mix, 0},
      {"beats_per_frame", 1, 1, true, "", 0, transport, 2},
      {"shape", 1, 3, true, "", 0, shape, 0}, {"timebase", 1, 3, true, "", 0, tbase, 0}, {"retrigger", 1, 3, true, "", 0, retrig, 0},
      {"note_in", 0, 2, true, "{i32, i32, i32, float, i32, i32, i32, [ 16 x i8 ]}", 44, noteProxy, 0} };
    Tree.OList conns = list(obj(nitm, 6194), 614); conns.items.clear();
    for (int i = 0; i < I.length; i++) { Object[] e = I[i];
      ins.items.add(itemProp(port(ptpl, (String) e[0], (Integer) e[1], i, (Integer) e[2], (Boolean) e[3], (String) e[4], (Integer) e[5])));
      conns.items.add(itemProp(conn((Tree.Obj) e[6], (Integer) e[7]))); }
    outs.items.add(itemProp(port(ptpl, "out", 2, 0, 1, false, "", 0)));
    outs.items.add(itemProp(port(ptpl, "display", 4, 1, 1, false, "", 0)));
    outs.items.add(itemProp(port(ptpl, "display_hold", 4, 2, 1, false, "", 0)));
    Tree.OList dconns = list(obj(drawing, 6194), 614);
    for (int ci = 0; ci < dconns.items.size(); ci++) { Tree.Obj c = (Tree.Obj) ((Tree.Prop) dconns.items.get(ci)).val; Tree.Prop src = get(c, 248);
      if (ci < 2) { Tree.Ref r0 = new Tree.Ref(); r0.target = nitm; src.type = "OBJECT_REFERENCE"; src.val = r0; set(c, 249, (byte) (ci + 1)); }
      else { src.type = "NULL_OBJECT"; src.val = null; set(c, 249, (byte) 0); } }

    kids.items.clear();
    for (Tree.Obj o : new Tree.Obj[]{c2000, c1371, transport, rate, depth, stereo, mix, shape, tbase, retrig, nitm, drawing}) kids.items.add(itemProp(o));
    Tree.Obj c50 = (Tree.Obj) ((Tree.Prop) list(root, 178).items.get(0)).val;           // device audio output proxy
    Tree.Obj c50c = (Tree.Obj) ((Tree.Prop) list(obj(c50, 6194), 614).items.get(0)).val; ((Tree.Ref) val(c50c, 248)).target = nitm; set(c50c, 249, (byte) 0);

    // panel
    Tree.Obj page = (Tree.Obj) ((Tree.Prop) list(root, 6213).items.get(0)).val;
    Tree.Obj content = obj(page, 6212); Tree.OList ws = list(content, 6221);
    Tree.Obj knobT = null, segT = null, labT = null, shaderT = null;
    for (Object i : ws.items) { Tree.Obj w = (Tree.Obj) ((Tree.Prop) i).val; if (w.cid == 1687 && knobT == null) knobT = w; if (w.cid == 1712 && segT == null) segT = w; if (w.cid == 1683 && labT == null) labT = w; if (w.cid == 5517 && shaderT == null) shaderT = w; }
    ws.items.clear();
    // Grid: page 122 x 53, margins 3. Left block x=3..73; gap 4; right columns x=77..96 and x=100..119 (w19, gap 4). All span y=3..50.
    // Left: curve y=3..42 (h39), gap 3, shape buttons y=45..50 (h5, the fixed row height of a horizontal button group; 7 buttons of 10).
    // Right rows: knob 15 + label 5 | 1 | control row 5 | 1 | knob 15 + label 5
    List<Tree.Obj> W = new ArrayList<>();
    Tree.Obj curveW = cloneObj(shaderT); setLayout(curveW, 3, 3, 70, 39); Tree.Ref dr = new Tree.Ref(); dr.target = drawing; get(curveW, 6220).val = dr; W.add(curveW);
    Tree.Obj shapeW = widget(segT, 3, 45, 70, 5, shape, "LFO shape"); set(shapeW, 6255, true); set(shapeW, 6257, (byte) 1); set(shapeW, 10143, (byte) 1);   // horizontal, icon style, orange highlight
    W.add(shapeW);
    String[] shapeKeys = {"sine","triangle","square","ramp_up","ramp_down","sample_and_hold","sample_and_slide_cubic"};
    for (int q = 0; q < 7; q++) W.add(image(imgT, 3 + 10 * q, 45, 10, 5, icon(icons, shapeKeys[q])));                           // icon overlays on the buttons
    W.add(widget(knobT, 77, 3, 19, 15, rate, "LFO rate (multiplies the selected time base). On Hold it sets the LFO position instead"));
    W.add(label(labT, 77, 18, 19, 5, "RATE"));
    W.add(widget(knobT, 100, 3, 19, 15, depth, "Vibrato depth: up to 100 cents"));
    W.add(label(labT, 100, 18, 19, 5, "DEPTH"));
    Tree.Obj tbW = widget(ddT, 77, 24, 19, 5, tbase, "Time base: Hz, kHz, bar, note values, pitch of current note, or hold"); set(tbW, 6262, (byte) 1); W.add(tbW);   // icon-only preview
    Tree.Obj tg = cloneObj(toggleT); setLayout(tg, 100, 24, 19, 5); Tree.Ref tr = new Tree.Ref(); tr.target = retrig; get(tg, 6220).val = tr;
    set(tg, 6867, "Retrigger the LFO on every note"); set(tg, 6241, "R"); W.add(tg);
    W.add(widget(knobT, 77, 30, 19, 15, stereo, "Left/right LFO phase offset"));
    W.add(label(labT, 77, 45, 19, 5, "STEREO"));
    W.add(widget(knobT, 100, 30, 19, 15, mix, "Dry/wet mix"));
    W.add(label(labT, 100, 45, 19, 5, "MIX"));
    for (Tree.Obj w : W) ws.items.add(itemProp(w));
    set(page, 6232, (short) 122); set(page, 6233, (byte) 53);

    // metadata
    String uuid = UUID.nameUUIDFromBytes("asd-vibrato-custom-device".getBytes()).toString();
    String desc = "Pitch vibrato with seven LFO shapes, free or tempo-synced rate, depth, stereo phase and mix";
    String tags = "vibrato pitch modulation lfo delay tempo sync retrigger";
    set(root, 385, UUID.fromString(uuid)); set(root, 386, "ASD Vibrato"); set(root, 2057, desc); set(root, 389, tags); get(root, 387).val = "Asaniel"; set(root, 390, "Modulation");
    meta.put("device_name", "ASD Vibrato"); meta.put("preset_name", "ASD Vibrato"); meta.put("device_id", uuid); meta.put("device_uuid", uuid);
    meta.put("device_description", desc); meta.put("comment", ""); meta.put("creator", "Asaniel"); meta.put("device_creator", "Asaniel"); meta.put("tags", tags);
    meta.put("device_category", "Modulation"); meta.put("application_version_name", "6.1.7"); meta.put("has_note_input", Boolean.TRUE);
    write(root, meta, out);
  }
}
