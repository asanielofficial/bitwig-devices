import java.io.*; import java.nio.file.*; import java.util.stream.*;

/**
 * Lowers the class-file version of extracted Bitwig classes to what the running JDK can load, so the
 * tools can be compiled and run on an older JDK than the one Bitwig ships with (the classes are never
 * rewritten otherwise). Usage: java tools/src/PatchClasses.java <extracted-classes-dir>
 */
public class PatchClasses {
  public static void main(String[] a) throws Exception {
    int runtimeMajor = Runtime.version().feature() + 44;   // Java 21 -> 65
    int patched = 0, seen = 0;
    try (Stream<Path> s = Files.walk(Paths.get(a[0]))) {
      for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".class"))::iterator) {
        byte[] b = Files.readAllBytes(p); seen++;
        if (b.length < 8 || (b[0] & 0xff) != 0xCA || (b[1] & 0xff) != 0xFE) continue;
        int major = ((b[6] & 0xff) << 8) | (b[7] & 0xff);
        if (major > runtimeMajor) { b[6] = (byte) (runtimeMajor >> 8); b[7] = (byte) runtimeMajor; Files.write(p, b); patched++; }
      }
    }
    System.out.println("classes: " + seen + ", patched to major " + runtimeMajor + ": " + patched);
  }
}
