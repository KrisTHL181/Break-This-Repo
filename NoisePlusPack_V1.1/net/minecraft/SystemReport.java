package net.minecraft;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class SystemReport {
   public static final long BYTES_PER_MEBIBYTE = 1048576L;
   private static final long ONE_GIGA = 1000000000L;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String OPERATING_SYSTEM = System.getProperty("os.name")
      + " ("
      + System.getProperty("os.arch")
      + ") version "
      + System.getProperty("os.version");
   private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
   private static final String JAVA_VM_VERSION = System.getProperty("java.vm.name")
      + " ("
      + System.getProperty("java.vm.info")
      + "), "
      + System.getProperty("java.vm.vendor");
   private final Map<String, String> entries = Maps.newLinkedHashMap();

   public SystemReport() {
      this.setDetail("Minecraft Version", SharedConstants.getCurrentVersion().name());
      this.setDetail("Minecraft Version ID", SharedConstants.getCurrentVersion().id());
      this.setDetail("Operating System", OPERATING_SYSTEM);
      this.setDetail("Java Version", JAVA_VERSION);
      this.setDetail("Java VM Version", JAVA_VM_VERSION);
      this.setDetail("Memory", () -> {
         Runtime runtime = Runtime.getRuntime();
         long i = runtime.maxMemory();
         long j = runtime.totalMemory();
         long k = runtime.freeMemory();
         long l = i / 1048576L;
         long i1 = j / 1048576L;
         long j1 = k / 1048576L;
         return k + " bytes (" + j1 + " MiB) / " + j + " bytes (" + i1 + " MiB) up to " + i + " bytes (" + l + " MiB)";
      });
      this.setDetail("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
      this.ignoreErrors("hardware", () -> this.putHardware(new SystemInfo()));
      this.setDetail("JVM Flags", () -> printJvmFlags(p_420617_ -> p_420617_.startsWith("-X")));
      this.setDetail("Debug Flags", () -> printJvmFlags(p_420618_ -> p_420618_.startsWith("-DMC_DEBUG_")));
   }

   private static String printJvmFlags(Predicate<String> p_426325_) {
      List<String> list = ManagementFactory.getRuntimeMXBean().getInputArguments();
      List<String> list1 = list.stream().filter(p_426325_).toList();
      return String.format(Locale.ROOT, "%d total; %s", list1.size(), String.join(" ", list1));
   }

   public void setDetail(String p_143520_, String p_143521_) {
      this.entries.put(p_143520_, p_143521_);
   }

   public void setDetail(String p_143523_, Supplier<String> p_143524_) {
      try {
         this.setDetail(p_143523_, p_143524_.get());
      } catch (Exception exception) {
         LOGGER.warn("Failed to get system info for {}", p_143523_, exception);
         this.setDetail(p_143523_, "ERR");
      }
   }

   private void putHardware(SystemInfo p_143536_) {
      HardwareAbstractionLayer hardwareabstractionlayer = p_143536_.getHardware();
      this.ignoreErrors("processor", () -> this.putProcessor(hardwareabstractionlayer.getProcessor()));
      this.ignoreErrors("graphics", () -> this.putGraphics(hardwareabstractionlayer.getGraphicsCards()));
      this.ignoreErrors("memory", () -> this.putMemory(hardwareabstractionlayer.getMemory()));
      this.ignoreErrors("storage", this::putStorage);
   }

   private void ignoreErrors(String p_143517_, Runnable p_143518_) {
      try {
         p_143518_.run();
      } catch (Throwable throwable) {
         LOGGER.warn("Failed retrieving info for group {}", p_143517_, throwable);
      }
   }

   public static float sizeInMiB(long p_342777_) {
      return (float)p_342777_ / 1048576.0F;
   }

   private void putPhysicalMemory(List<PhysicalMemory> p_143532_) {
      int i = 0;

      for (PhysicalMemory physicalmemory : p_143532_) {
         String s = String.format(Locale.ROOT, "Memory slot #%d ", i++);
         this.setDetail(s + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(physicalmemory.getCapacity())));
         this.setDetail(s + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)physicalmemory.getClockSpeed() / 1.0E9F));
         this.setDetail(s + "type", physicalmemory::getMemoryType);
      }
   }

   private void putVirtualMemory(VirtualMemory p_143550_) {
      this.setDetail("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getVirtualMax())));
      this.setDetail("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getVirtualInUse())));
      this.setDetail("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getSwapTotal())));
      this.setDetail("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(p_143550_.getSwapUsed())));
   }

   private void putMemory(GlobalMemory p_143542_) {
      this.ignoreErrors("physical memory", () -> this.putPhysicalMemory(p_143542_.getPhysicalMemory()));
      this.ignoreErrors("virtual memory", () -> this.putVirtualMemory(p_143542_.getVirtualMemory()));
   }

   private void putGraphics(List<GraphicsCard> p_143553_) {
      int i = 0;

      for (GraphicsCard graphicscard : p_143553_) {
         String s = String.format(Locale.ROOT, "Graphics card #%d ", i++);
         this.setDetail(s + "name", graphicscard::getName);
         this.setDetail(s + "vendor", graphicscard::getVendor);
         this.setDetail(s + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", sizeInMiB(graphicscard.getVRam())));
         this.setDetail(s + "deviceId", graphicscard::getDeviceId);
         this.setDetail(s + "versionInfo", graphicscard::getVersionInfo);
      }
   }

   private void putProcessor(CentralProcessor p_143540_) {
      ProcessorIdentifier processoridentifier = p_143540_.getProcessorIdentifier();
      this.setDetail("Processor Vendor", processoridentifier::getVendor);
      this.setDetail("Processor Name", processoridentifier::getName);
      this.setDetail("Identifier", processoridentifier::getIdentifier);
      this.setDetail("Microarchitecture", processoridentifier::getMicroarchitecture);
      this.setDetail("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)processoridentifier.getVendorFreq() / 1.0E9F));
      this.setDetail("Number of physical packages", () -> String.valueOf(p_143540_.getPhysicalPackageCount()));
      this.setDetail("Number of physical CPUs", () -> String.valueOf(p_143540_.getPhysicalProcessorCount()));
      this.setDetail("Number of logical CPUs", () -> String.valueOf(p_143540_.getLogicalProcessorCount()));
   }

   private void putStorage() {
      this.putSpaceForProperty("jna.tmpdir");
      this.putSpaceForProperty("org.lwjgl.system.SharedLibraryExtractPath");
      this.putSpaceForProperty("io.netty.native.workdir");
      this.putSpaceForProperty("java.io.tmpdir");
      this.putSpaceForPath("workdir", () -> "");
   }

   private void putSpaceForProperty(String p_345490_) {
      this.putSpaceForPath(p_345490_, () -> System.getProperty(p_345490_));
   }

   private void putSpaceForPath(String p_344137_, Supplier<@Nullable String> p_344490_) {
      String s = "Space in storage for " + p_344137_ + " (MiB)";

      try {
         String s1 = p_344490_.get();
         if (s1 == null) {
            this.setDetail(s, "<path not set>");
            return;
         }

         FileStore filestore = Files.getFileStore(Path.of(s1));
         this.setDetail(
            s, String.format(Locale.ROOT, "available: %.2f, total: %.2f", sizeInMiB(filestore.getUsableSpace()), sizeInMiB(filestore.getTotalSpace()))
         );
      } catch (InvalidPathException invalidpathexception) {
         LOGGER.warn("{} is not a path", p_344137_, invalidpathexception);
         this.setDetail(s, "<invalid path>");
      } catch (Exception exception) {
         LOGGER.warn("Failed retrieving storage space for {}", p_344137_, exception);
         this.setDetail(s, "ERR");
      }
   }

   public void appendToCrashReportString(StringBuilder p_143526_) {
      p_143526_.append("-- ").append("System Details").append(" --\n");
      p_143526_.append("Details:");
      this.entries.forEach((p_143529_, p_143530_) -> {
         p_143526_.append("\n\t");
         p_143526_.append(p_143529_);
         p_143526_.append(": ");
         p_143526_.append(p_143530_);
      });
   }

   public String toLineSeparatedString() {
      return this.entries
         .entrySet()
         .stream()
         .map(p_143534_ -> p_143534_.getKey() + ": " + p_143534_.getValue())
         .collect(Collectors.joining(System.lineSeparator()));
   }
}
