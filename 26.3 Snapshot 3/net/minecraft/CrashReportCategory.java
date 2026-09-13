package net.minecraft;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CrashReportCategory {
   private final String title;
   private final List<CrashReportCategory.Entry> entries = Lists.newArrayList();
   private StackTraceElement[] stackTrace = new StackTraceElement[0];

   public CrashReportCategory(final String title) {
      this.title = title;
   }

   public static String formatLocation(final LevelHeightAccessor levelHeightAccessor, final double x, final double y, final double z) {
      return String.format(Locale.ROOT, "%.2f,%.2f,%.2f - %s", x, y, z, formatLocation(levelHeightAccessor, BlockPos.containing(x, y, z)));
   }

   public static String formatLocation(final LevelHeightAccessor levelHeightAccessor, final BlockPos pos) {
      return formatLocation(levelHeightAccessor, pos.getX(), pos.getY(), pos.getZ());
   }

   public static String formatLocation(final LevelHeightAccessor levelHeightAccessor, final int x, final int y, final int z) {
      StringBuilder result = new StringBuilder();

      try {
         result.append(String.format(Locale.ROOT, "World: (%d,%d,%d)", x, y, z));
      } catch (Throwable ignored) {
         result.append("(Error finding world loc)");
      }

      result.append(", ");

      try {
         int sectionX = SectionPos.blockToSectionCoord(x);
         int sectionY = SectionPos.blockToSectionCoord(y);
         int sectionZ = SectionPos.blockToSectionCoord(z);
         int relativeX = x & 15;
         int relativeY = y & 15;
         int relativeZ = z & 15;
         int minBlockX = SectionPos.sectionToBlockCoord(sectionX);
         int minBlockY = levelHeightAccessor.getMinY();
         int minBlockZ = SectionPos.sectionToBlockCoord(sectionZ);
         int maxBlockX = SectionPos.sectionToBlockCoord(sectionX + 1) - 1;
         int maxBlockY = levelHeightAccessor.getMaxY();
         int maxBlockZ = SectionPos.sectionToBlockCoord(sectionZ + 1) - 1;
         result.append(
            String.format(
               Locale.ROOT,
               "Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)",
               relativeX,
               relativeY,
               relativeZ,
               sectionX,
               sectionY,
               sectionZ,
               minBlockX,
               minBlockY,
               minBlockZ,
               maxBlockX,
               maxBlockY,
               maxBlockZ
            )
         );
      } catch (Throwable ignored) {
         result.append("(Error finding chunk loc)");
      }

      result.append(", ");

      try {
         int regionX = x >> 9;
         int regionZ = z >> 9;
         int minChunkX = regionX << 5;
         int minChunkZ = regionZ << 5;
         int maxChunkX = (regionX + 1 << 5) - 1;
         int maxChunkZ = (regionZ + 1 << 5) - 1;
         int minBlockX = regionX << 9;
         int minBlockY = levelHeightAccessor.getMinY();
         int minBlockZ = regionZ << 9;
         int maxBlockX = (regionX + 1 << 9) - 1;
         int maxBlockY = levelHeightAccessor.getMaxY();
         int maxBlockZ = (regionZ + 1 << 9) - 1;
         result.append(
            String.format(
               Locale.ROOT,
               "Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)",
               regionX,
               regionZ,
               minChunkX,
               minChunkZ,
               maxChunkX,
               maxChunkZ,
               minBlockX,
               minBlockY,
               minBlockZ,
               maxBlockX,
               maxBlockY,
               maxBlockZ
            )
         );
      } catch (Throwable ignored) {
         result.append("(Error finding world loc)");
      }

      return result.toString();
   }

   public CrashReportCategory setDetail(final String key, final CrashReportDetail<String> callback) {
      try {
         this.setDetail(key, callback.call());
      } catch (Throwable t) {
         this.setDetailError(key, t);
      }

      return this;
   }

   public CrashReportCategory setDetail(final String key, final @Nullable Object value) {
      this.entries.add(new CrashReportCategory.Entry(key, value));
      return this;
   }

   public void setDetailError(final String key, final Throwable t) {
      this.setDetail(key, t);
   }

   public int fillInStackTrace(final int nestedOffset) {
      StackTraceElement[] full = Thread.currentThread().getStackTrace();
      if (full.length <= 0) {
         return 0;
      }

      this.stackTrace = new StackTraceElement[full.length - 3 - nestedOffset];
      System.arraycopy(full, 3 + nestedOffset, this.stackTrace, 0, this.stackTrace.length);
      return this.stackTrace.length;
   }

   public boolean validateStackTrace(final @Nullable StackTraceElement source, final @Nullable StackTraceElement next) {
      if (this.stackTrace.length != 0 && source != null) {
         StackTraceElement current = this.stackTrace[0];
         if (current.isNativeMethod() == source.isNativeMethod()
            && Objects.equals(current.getClassName(), source.getClassName())
            && Objects.equals(current.getFileName(), source.getFileName())
            && Objects.equals(current.getMethodName(), source.getMethodName())) {
            if (next != null != this.stackTrace.length > 1) {
               return false;
            }

            if (next != null && !this.stackTrace[1].equals(next)) {
               return false;
            }

            this.stackTrace[0] = source;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void getDetails(final StringBuilder builder) {
      builder.append("-- ").append(this.title).append(" --\n");
      builder.append("Details:");

      for (CrashReportCategory.Entry entry : this.entries) {
         builder.append("\n\t");
         builder.append(entry.key());
         builder.append(": ");
         builder.append(entry.value());
      }

      if (this.stackTrace.length > 0) {
         builder.append("\nStacktrace:");

         for (StackTraceElement element : this.stackTrace) {
            builder.append("\n\tat ");
            builder.append(element);
         }
      }
   }

   public StackTraceElement[] getStacktrace() {
      return this.stackTrace;
   }

   public static void populateBlockDetails(
      final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor, final BlockPos pos, final BlockState state
   ) {
      category.setDetail("Block", state::toString);
      populateBlockLocationDetails(category, levelHeightAccessor, pos);
   }

   public static CrashReportCategory populateBlockLocationDetails(
      final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor, final BlockPos pos
   ) {
      return category.setDetail("Block location", () -> formatLocation(levelHeightAccessor, pos));
   }

   public record Entry(String key, String value) {
      public Entry(final String key, final @Nullable Object rawValue) {
         String value;
         if (rawValue == null) {
            value = "~~NULL~~";
         } else if (rawValue instanceof Throwable t) {
            value = "~~ERROR~~ " + t.getClass().getSimpleName() + ": " + t.getMessage();
         } else {
            value = rawValue.toString();
         }

         this(key, value);
      }
   }
}
