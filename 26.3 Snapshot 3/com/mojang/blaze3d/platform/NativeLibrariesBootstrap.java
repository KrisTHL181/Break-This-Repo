package com.mojang.blaze3d.platform;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.RandomSource;
import org.apache.commons.io.output.TeeOutputStream;
import org.jspecify.annotations.Nullable;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.ALC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;
import org.lwjgl.util.freetype.FreeType;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.spvc.Spvc;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK;
import org.slf4j.Logger;

public class NativeLibrariesBootstrap {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
   private static boolean vulkanLoaderAvailable;

   public static void loadLibraries() throws IOException {
      Stopwatch stopwatch = Stopwatch.createStarted();
      configureLWJGLLibraryPath();
      createAndCheckDirectory((String)Configuration.SHARED_LIBRARY_EXTRACT_PATH.get(""));
      createAndCheckDirectory(System.getProperty("jna.tmpdir", ""));
      createAndCheckDirectory(System.getProperty("io.netty.native.workdir", ""));
      Boolean originalDebugLoader = (Boolean)Configuration.DEBUG_LOADER.get();
      Configuration.DEBUG_LOADER.set(true);
      Supplier<String> stopCapturing = setupLWJGLCapture();
      int libraryIndex = -1;
      List<NativeLibrariesBootstrap.LibraryLoadEntry> entries = new ArrayList<>();

      try {
         if (SharedConstants.DEBUG_SIMULATE_LIBRARY_LOAD_FAILURE) {
            throw new UnsatisfiedLinkError("Simulated debug crash");
         }

         loadLibrary(stopCapturing, "LWJGL system", NativeLibrariesBootstrap::loadLWJGLSystem);
         vulkanLoaderAvailable = tryLoadingVulkan();
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("GLFW", NativeLibrariesBootstrap::loadGlfw));
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("OpenGL", NativeLibrariesBootstrap::loadOpenGL));
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("OpenAL", NativeLibrariesBootstrap::loadOpenAL));
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("STB", NativeLibrariesBootstrap::loadSTB));
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("tinyfd", NativeLibrariesBootstrap::loadTinyFD));
         entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("freetype", NativeLibrariesBootstrap::loadFreeType));
         if (vulkanLoaderAvailable) {
            entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("shaderc", NativeLibrariesBootstrap::loadShaderc));
            entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("spvc", NativeLibrariesBootstrap::loadSpvc));
            entries.add(new NativeLibrariesBootstrap.LibraryLoadEntry("vma", NativeLibrariesBootstrap::loadVma));
         }

         Collections.shuffle(entries);

         for (libraryIndex = 0; libraryIndex < entries.size(); libraryIndex++) {
            NativeLibrariesBootstrap.LibraryLoadEntry e = entries.get(libraryIndex);
            loadLibrary(stopCapturing, e.name(), e.loader());
         }
      } catch (Throwable t) {
         CrashReport crashReport = CrashReport.forThrowable(t, "Loading libraries");
         CrashReportCategory librariesLoaded = crashReport.addCategory("Libraries loaded");
         librariesLoaded.setDetail(
            "Loading order", () -> entries.stream().map(NativeLibrariesBootstrap.LibraryLoadEntry::name).collect(Collectors.joining(","))
         );
         librariesLoaded.setDetail("Loading index", Integer.toString(libraryIndex));
         throw new ReportedException(crashReport);
      } finally {
         stopCapturing.get();
         Configuration.DEBUG_LOADER.set(originalDebugLoader);
      }

      long var13 = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
      LOGGER.debug("Library load time: {} ms", var13);
   }

   private static void createAndCheckDirectory(final String libraryDir) throws IOException {
      if (!libraryDir.isEmpty()) {
         Path libraryDirPath = Path.of(libraryDir);
         Files.createDirectories(libraryDirPath);
         RandomSource randomSource = RandomSource.createThreadLocalInstance();
         String trollFileName = System.mapLibraryName("VeryImportant" + randomSource.nextInt(9999));
         Path probeFile = libraryDirPath.resolve(trollFileName);
         byte[] expectedBytes = Ints.toByteArray(randomSource.nextInt());
         Files.write(probeFile, expectedBytes);

         try (
            FileChannel fc = FileChannel.open(probeFile);
            FileLock lock = tryLock(fc);
         ) {
            if (lock == null) {
               throw new IOException("Failed to lock " + probeFile);
            }

            byte[] readBytes = Channels.newInputStream(fc).readAllBytes();
            if (!Arrays.equals(expectedBytes, readBytes)) {
               throw new IOException(
                  "Unexpected probe file contents, expected '" + HEX_FORMAT.formatHex(expectedBytes) + "', but got '" + HEX_FORMAT.formatHex(readBytes) + "'"
               );
            }
         } finally {
            Files.delete(probeFile);
         }
      }
   }

   private static @Nullable FileLock tryLock(final FileChannel fc) throws IOException {
      for (int i = 0; i < 5; i++) {
         FileLock lock = fc.tryLock(0L, Long.MAX_VALUE, true);
         if (lock != null) {
            return lock;
         }

         try {
            Thread.sleep(10L);
         } catch (InterruptedException var4) {
            break;
         }
      }

      return null;
   }

   private static void configureLWJGLLibraryPath() {
      String libraryPathString = (String)Configuration.SHARED_LIBRARY_EXTRACT_PATH.get();
      if (libraryPathString != null) {
         String version = Version.getVersion().replace(' ', '-');
         String arch = Platform.getArchitecture().name().toLowerCase(Locale.ROOT);
         Path newLibraryDir = Path.of(libraryPathString).resolve(version, arch);
         Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(newLibraryDir.toString());
      }
   }

   @SuppressForbidden(reason = "System.out needed before bootstrap")
   private static Supplier<String> setupLWJGLCapture() {
      if (Configuration.DEBUG_STREAM.get() == null && !(Boolean)Configuration.DEBUG.get(false)) {
         NativeLibrariesBootstrap.CapturingPrintStream capturingPrintStream = new NativeLibrariesBootstrap.CapturingPrintStream(System.out);
         Configuration.DEBUG_STREAM.set(capturingPrintStream);
         capturingPrintStream.startCapturing();
         return capturingPrintStream::stopCapturing;
      } else {
         return () -> "<LWJGL debug enabled, not capturing>";
      }
   }

   private static void loadLibrary(final Supplier<String> debugCapture, final String name, final Runnable loader) {
      try {
         LOGGER.debug("Loading {}", name);
         loader.run();
      } catch (Throwable t) {
         CrashReport crashReport = CrashReport.forThrowable(t, "Loading library " + name);
         CrashReportCategory libraryInfoCategory = crashReport.addCategory("Library directory contents");
         String systemPropertyDir = System.getProperty("java.library.path", "");
         String lwjglPropertyDir = (String)Configuration.LIBRARY_PATH.get("");
         if (systemPropertyDir.equals(lwjglPropertyDir)) {
            libraryInfoCategory.setDetail("Contents of shared library directory", () -> listLibrariesDirectory(systemPropertyDir));
         } else {
            libraryInfoCategory.setDetail("Contents of java.library.path ", () -> listLibrariesDirectory(systemPropertyDir));
            libraryInfoCategory.setDetail("Contents of org.lwjgl.librarypath", () -> listLibrariesDirectory(lwjglPropertyDir));
         }

         libraryInfoCategory.setDetail("LWJGL platform", () -> Platform.get().toString());
         libraryInfoCategory.setDetail("LWJGL architecture", () -> Platform.getArchitecture().toString());
         CrashReportCategory lwjglDebugLog = crashReport.addCategory("LWJGL debug log");

         try {
            lwjglDebugLog.setDetail("Log", debugCapture.get());
         } catch (Throwable e) {
            lwjglDebugLog.setDetail("Log", e);
         }

         throw new ReportedException(crashReport);
      }
   }

   private static String listLibrariesDirectory(final @Nullable String libraryDirProperty) throws IOException {
      if (libraryDirProperty == null || libraryDirProperty.isEmpty()) {
         return "<not set>";
      }

      if (libraryDirProperty.contains(";")) {
         return "<multiple directories>";
      }

      Path libraryDirPath = Path.of(libraryDirProperty);
      if (!Files.isDirectory(libraryDirPath)) {
         return "<not a directory>";
      }

      List<Pair<Path, String>> contents = new ArrayList<>();

      try (DirectoryStream<Path> libraryDir = Files.newDirectoryStream(libraryDirPath)) {
         for (Path dirEntry : libraryDir) {
            contents.add(Pair.of(dirEntry, identifyFileContents(dirEntry)));
         }
      }

      return contents.isEmpty()
         ? "<empty>"
         : "\n" + contents.stream().map(s -> "\t\t" + ((Path)s.getFirst()).getFileName() + ": " + (String)s.getSecond()).collect(Collectors.joining("\n"));
   }

   private static String identifyFileContents(final Path path) {
      try {
         if (Files.isRegularFile(path)) {
            if (path.getFileName().toString().endsWith(".dll")) {
               Optional<String> detailedModuleInfo = NativeModuleLister.tryGetModuleVersion(path.toString())
                  .map(NativeModuleLister.NativeModuleVersion::toString);
               if (detailedModuleInfo.isPresent()) {
                  return "module: " + detailedModuleInfo.get();
               }
            }

            return Objects.requireNonNullElse(Files.probeContentType(path), "unknown type");
         } else {
            return "not a file";
         }
      } catch (Throwable e) {
         LOGGER.warn("Failed to get details of file {}", path, e);
         return "error: " + e.getMessage();
      }
   }

   private static void loadLWJGLSystem() {
      Library.initialize();
   }

   private static void loadGlfw() {
      Objects.requireNonNull(GLFW.getLibrary());
   }

   private static void loadOpenGL() {
      Objects.requireNonNull(GL.getFunctionProvider());
   }

   private static void loadOpenAL() {
      Objects.requireNonNull(ALC.getFunctionProvider());
   }

   private static void loadSTB() {
      String lastStbError = STBImage.stbi_failure_reason();
      if (lastStbError != null) {
         throw new IllegalStateException("No error expected, but got " + lastStbError);
      }
   }

   private static boolean tryLoadingVulkan() {
      if (Configuration.VULKAN_EXPLICIT_INIT.get() == null) {
         Configuration.VULKAN_EXPLICIT_INIT.set(true);
      }

      try {
         VK.create();
         return true;
      } catch (Throwable t) {
         LOGGER.warn("Failed to load Vulkan loader", t);
         return false;
      }
   }

   public static boolean isVulkanLoaderAvailable() {
      return vulkanLoaderAvailable;
   }

   private static void loadShaderc() {
      Objects.requireNonNull(Shaderc.getLibrary());
   }

   private static void loadSpvc() {
      Objects.requireNonNull(Spvc.getLibrary());
   }

   private static void loadVma() {
      try {
         Vma.vmaDestroyAllocator(0L);
      } catch (NullPointerException var1) {
      }
   }

   private static void loadTinyFD() {
      Objects.requireNonNull(TinyFileDialogs.tinyfd_getGlobalChar("tinyfd_version"));
   }

   private static void loadFreeType() {
      Objects.requireNonNull(FreeType.getLibrary());
   }

   private static class CapturingPrintStream extends PrintStream {
      private final NativeLibrariesBootstrap.CapturingStream collector;

      public CapturingPrintStream(final OutputStream out) {
         NativeLibrariesBootstrap.CapturingStream logStream = new NativeLibrariesBootstrap.CapturingStream();
         super(new TeeOutputStream(out, logStream), false, StandardCharsets.UTF_8);
         this.collector = logStream;
      }

      public synchronized void startCapturing() {
         this.collector.buffer = new ByteArrayOutputStream();
      }

      public synchronized String stopCapturing() {
         ByteArrayOutputStream buffer = this.collector.buffer;
         this.collector.buffer = null;
         return buffer != null ? buffer.toString(StandardCharsets.UTF_8) : "";
      }
   }

   private static class CapturingStream extends OutputStream {
      private @Nullable ByteArrayOutputStream buffer;

      @Override
      public void write(final byte[] b, final int off, final int len) {
         if (this.buffer != null) {
            this.buffer.write(b, off, len);
         }
      }

      @Override
      public void write(final int b) {
         if (this.buffer != null) {
            this.buffer.write(b);
         }
      }
   }

   private record LibraryLoadEntry(String name, Runnable loader) {
   }
}
