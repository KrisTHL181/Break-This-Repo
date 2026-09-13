package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.server.packs.PackResources;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ResourceLoadStateTracker {
   private static final Logger LOGGER = LogUtils.getLogger();
   private ResourceLoadStateTracker.@Nullable ReloadState reloadState;
   private int reloadCount;

   public void startReload(final ResourceLoadStateTracker.ReloadReason reloadReason, final List<PackResources> packs) {
      this.reloadCount++;
      if (this.reloadState != null && !this.reloadState.finished) {
         LOGGER.warn("Reload already ongoing, replacing");
      }

      this.reloadState = new ResourceLoadStateTracker.ReloadState(
         reloadReason, packs.stream().map(PackResources::packId).collect(ImmutableList.toImmutableList())
      );
   }

   public void startRecovery(final Throwable reason) {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to signal reload recovery, but nothing was started");
         this.reloadState = new ResourceLoadStateTracker.ReloadState(ResourceLoadStateTracker.ReloadReason.UNKNOWN, ImmutableList.of());
      }

      this.reloadState.recoveryReloadInfo = new ResourceLoadStateTracker.RecoveryInfo(reason);
   }

   public void finishReload() {
      if (this.reloadState == null) {
         LOGGER.warn("Trying to finish reload, but nothing was started");
      } else {
         this.reloadState.finished = true;
      }
   }

   public void fillCrashReport(final CrashReport report) {
      CrashReportCategory category = report.addCategory("Last reload");
      category.setDetail("Reload number", this.reloadCount);
      if (this.reloadState != null) {
         this.reloadState.fillCrashInfo(category);
      }
   }

   private static class RecoveryInfo {
      private final Throwable error;

      private RecoveryInfo(final Throwable error) {
         this.error = error;
      }

      public void fillCrashInfo(final CrashReportCategory category) {
         category.setDetail("Recovery", "Yes");
         category.setDetail("Recovery reason", () -> {
            StringWriter writer = new StringWriter();
            this.error.printStackTrace(new PrintWriter(writer));
            return writer.toString();
         });
      }
   }

   public enum ReloadReason {
      INITIAL("initial"),
      MANUAL("manual"),
      UNKNOWN("unknown");

      private final String name;

      ReloadReason(final String name) {
         this.name = name;
      }
   }

   private static class ReloadState {
      private final ResourceLoadStateTracker.ReloadReason reloadReason;
      private final List<String> packs;
      private ResourceLoadStateTracker.@Nullable RecoveryInfo recoveryReloadInfo;
      private boolean finished;

      private ReloadState(final ResourceLoadStateTracker.ReloadReason reloadReason, final List<String> packs) {
         this.reloadReason = reloadReason;
         this.packs = packs;
      }

      public void fillCrashInfo(final CrashReportCategory category) {
         category.setDetail("Reload reason", this.reloadReason.name);
         category.setDetail("Finished", this.finished ? "Yes" : "No");
         category.setDetail("Packs", () -> String.join(", ", this.packs));
         if (this.recoveryReloadInfo != null) {
            this.recoveryReloadInfo.fillCrashInfo(category);
         }
      }
   }
}
