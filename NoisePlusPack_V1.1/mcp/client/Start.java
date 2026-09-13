package mcp.client;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.main.Main;

public class Start {
   public static void main(String[] args) throws IOException {
      int INDEX = 29;
      String assets = findAssets(INDEX);
      Main.main(
         concat(
            new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", Integer.toString(INDEX), "--userProperties", "{}"},
            args
         )
      );
   }

   public static <T> T[] concat(T[] first, T[] second) {
      T[] result = (T[])Arrays.copyOf(first, first.length + second.length);
      System.arraycopy(second, 0, result, first.length, second.length);
      return result;
   }

   private static String findAssets(int index) throws IOException {
      String ret = System.getenv("assetDirectory");
      if (ret != null) {
         return ret;
      }

      File dir = new File(getMCDir(), "assets");
      if (hasIndex(dir, index)) {
         return dir.getCanonicalPath();
      }

      dir = new File(".").getCanonicalFile();

      while (dir != null && !"versions".equals(dir.getName())) {
         dir = dir.getParentFile();
      }

      if (dir != null) {
         dir = new File(dir, "../build/assets");
         if (hasIndex(dir, index)) {
            return dir.getCanonicalPath();
         }
      }

      return "assets";
   }

   private static boolean hasIndex(File root, int idx) {
      return new File(root, "indexes/" + idx + ".json").exists();
   }

   private static File getMCDir() {
      switch (Start.OS.getCurrent()) {
         case WINDOWS:
            return new File(System.getenv("APPDATA") + "\\.minecraft");
         case LINUX:
         default:
            return new File(System.getProperty("user.home") + "/.minecraft");
         case OSX:
            return new File(System.getProperty("user.home") + "/Library/Application Support/minecraft");
      }
   }

   private enum OS {
      WINDOWS("win"),
      LINUX("linux", "unix"),
      OSX("osx", "mac"),
      UNKNOWN();

      private final String[] keys;

      OS(String... keys) {
         this.keys = keys;
      }

      static Start.OS getCurrent() {
         String prop = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

         for (Start.OS os : values()) {
            for (String key : os.keys) {
               if (prop.contains(key)) {
                  return os;
               }
            }
         }

         return UNKNOWN;
      }
   }
}
