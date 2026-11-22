import java.io.*; import java.nio.file.*;

/**
 * Checks Nitro source with Bitwig own parser and prints what the stored form decodes back to.
 * Usage: NitCompile <file.nit>
 * Only syntax is checked here; type checking happens when Bitwig loads the device.
 */
public class NitCompile {
  public static void main(String[] a) throws Exception {
    String src = new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8");
    byte[] blob = DocKit.compileNitro(src, new File(a[0]).getName());
    k3K codec = (k3K) com.bitwig.nitro.NitroFile.class.getField("beI").get(null);
    com.bitwig.nitro.NitroFile back = com.bitwig.nitro.NitroFile.beI(codec, blob);
    StringWriter sw = new StringWriter(); com.bitwig.base.io.vQq w = new com.bitwig.base.io.vQq(sw); back.beI(w); w.Gef();
    System.out.println("OK: parsed " + src.length() + " chars into " + blob.length + " bytes");
    System.out.println(sw);
  }
}
