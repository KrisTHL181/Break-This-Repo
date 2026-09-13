package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.CrashReportCategory;
import org.slf4j.Logger;

public class NativeModuleLister {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int LANG_MASK = 65535;
   private static final int DEFAULT_LANG = 1033;
   private static final int CODEPAGE_MASK = -65536;
   private static final int DEFAULT_CODEPAGE = 78643200;

   public static List<NativeModuleLister.NativeModuleInfo> listModules() {
      if (!Platform.isWindows()) {
         return ImmutableList.of();
      }

      int selfHandle = Kernel32.INSTANCE.GetCurrentProcessId();
      Builder<NativeModuleLister.NativeModuleInfo> result = ImmutableList.builder();

      for (MODULEENTRY32W module : Kernel32Util.getModules(selfHandle)) {
         String name = module.szModule();
         Optional<NativeModuleLister.NativeModuleVersion> versionInfo = tryGetModuleVersion(module.szExePath());
         result.add(new NativeModuleLister.NativeModuleInfo(name, versionInfo));
      }

      return result.build();
   }

   public static Optional<NativeModuleLister.NativeModuleVersion> tryGetModuleVersion(final String path) {
      if (!Platform.isWindows()) {
         return Optional.empty();
      }

      try {
         IntByReference dwDummy = new IntByReference();
         int versionLength = Version.INSTANCE.GetFileVersionInfoSize(path, dwDummy);
         if (versionLength == 0) {
            int lastError = Native.getLastError();
            if (lastError != 1813 && lastError != 1812) {
               throw new Win32Exception(lastError);
            } else {
               return Optional.empty();
            }
         } else {
            Pointer lpData = new Memory(versionLength);
            if (!Version.INSTANCE.GetFileVersionInfo(path, 0, versionLength, lpData)) {
               throw new Win32Exception(Native.getLastError());
            }

            IntByReference size = new IntByReference();
            Pointer translationsBuffer = queryVersionValue(lpData, "\\VarFileInfo\\Translation", size);
            int[] langsAndCodepages = translationsBuffer.getIntArray(0L, size.getValue() / 4);
            OptionalInt maybeLangAndCodepage = findLangAndCodepage(langsAndCodepages);
            if (maybeLangAndCodepage.isEmpty()) {
               return Optional.empty();
            }

            int langAndCodepage = maybeLangAndCodepage.getAsInt();
            int lang = langAndCodepage & 65535;
            int codepage = (langAndCodepage & -65536) >> 16;
            String description = queryVersionString(lpData, langTableKey("FileDescription", lang, codepage), size);
            String companyName = queryVersionString(lpData, langTableKey("CompanyName", lang, codepage), size);
            String fileVersion = queryVersionString(lpData, langTableKey("FileVersion", lang, codepage), size);
            return Optional.of(new NativeModuleLister.NativeModuleVersion(description, fileVersion, companyName));
         }
      } catch (Exception e) {
         LOGGER.info("Failed to find module info for {}", path, e);
         return Optional.empty();
      }
   }

   private static String langTableKey(final String key, final int lang, final int codepage) {
      return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", lang, codepage, key);
   }

   private static OptionalInt findLangAndCodepage(final int[] langsAndCodepages) {
      OptionalInt bestSoFar = OptionalInt.empty();

      for (int langAndCodepage : langsAndCodepages) {
         if ((langAndCodepage & -65536) == 78643200 && (langAndCodepage & 65535) == 1033) {
            return OptionalInt.of(langAndCodepage);
         }

         bestSoFar = OptionalInt.of(langAndCodepage);
      }

      return bestSoFar;
   }

   private static Pointer queryVersionValue(final Pointer lpData, final String key, final IntByReference outSize) {
      PointerByReference lplpBuffer = new PointerByReference();
      if (!Version.INSTANCE.VerQueryValue(lpData, key, lplpBuffer, outSize)) {
         throw new UnsupportedOperationException("Can't get version value " + key);
      } else {
         return lplpBuffer.getValue();
      }
   }

   private static String queryVersionString(final Pointer lpData, final String key, final IntByReference outSize) {
      try {
         Pointer ptr = queryVersionValue(lpData, key, outSize);
         byte[] result = ptr.getByteArray(0L, (outSize.getValue() - 1) * 2);
         return sanitize(new String(result, StandardCharsets.UTF_16LE));
      } catch (Exception e) {
         return "";
      }
   }

   private static String sanitize(final String input) {
      return Util.CONTROL_CHARACTER_ESCAPER.escape(input);
   }

   public static void addCrashSection(final CrashReportCategory category) {
      category.setDetail(
         "Modules", () -> listModules().stream().sorted(Comparator.comparing(module -> module.name)).map(e -> "\n\t\t" + e).collect(Collectors.joining())
      );
   }

   public record NativeModuleInfo(String name, Optional<NativeModuleLister.NativeModuleVersion> version) {
      @Override
      public String toString() {
         return this.version.<String>map(v -> this.name + ":" + v).orElse(this.name);
      }
   }

   public record NativeModuleVersion(String description, String version, String company) {
      @Override
      public String toString() {
         return this.description + ":" + this.version + ":" + this.company;
      }
   }
}
