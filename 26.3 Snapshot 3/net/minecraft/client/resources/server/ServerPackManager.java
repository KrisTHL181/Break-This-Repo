package net.minecraft.client.resources.server;

import com.google.common.hash.HashCode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.packs.DownloadQueue;
import org.jspecify.annotations.Nullable;

public class ServerPackManager {
   private final PackDownloader downloader;
   private final PackLoadFeedback packLoadFeedback;
   private final PackReloadConfig reloadConfig;
   private final Runnable updateRequest;
   private ServerPackManager.PackPromptStatus packPromptStatus;
   private final List<ServerPackManager.ServerPackData> packs = new ArrayList<>();

   public ServerPackManager(
      final PackDownloader downloader,
      final PackLoadFeedback packLoadFeedback,
      final PackReloadConfig reloadConfig,
      final Runnable updateRequest,
      final ServerPackManager.PackPromptStatus packPromptStatus
   ) {
      this.downloader = downloader;
      this.packLoadFeedback = packLoadFeedback;
      this.reloadConfig = reloadConfig;
      this.updateRequest = updateRequest;
      this.packPromptStatus = packPromptStatus;
   }

   private void registerForUpdate() {
      this.updateRequest.run();
   }

   private void markExistingPacksAsRemoved(final UUID id) {
      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (pack.id.equals(id)) {
            pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REPLACED);
         }
      }
   }

   public void pushPack(final UUID id, final URL url, final @Nullable HashCode hash) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         this.pushNewPack(id, new ServerPackManager.ServerPackData(id, url, hash));
      }
   }

   public void pushLocalPack(final UUID id, final Path path) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         URL url;
         try {
            url = path.toUri().toURL();
         } catch (MalformedURLException e) {
            throw new IllegalStateException("Can't convert path to URL " + path, e);
         }

         ServerPackManager.ServerPackData pack = new ServerPackManager.ServerPackData(id, url, null);
         pack.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
         pack.path = path;
         this.pushNewPack(id, pack);
      }
   }

   private void pushNewPack(final UUID id, final ServerPackManager.ServerPackData pack) {
      this.markExistingPacksAsRemoved(id);
      this.packs.add(pack);
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.ALLOWED) {
         this.acceptPack(pack);
      }

      this.registerForUpdate();
   }

   private void acceptPack(final ServerPackManager.ServerPackData pack) {
      this.packLoadFeedback.reportUpdate(pack.id, PackLoadFeedback.Update.ACCEPTED);
      pack.promptAccepted = true;
   }

   private ServerPackManager.@Nullable ServerPackData findPackInfo(final UUID id) {
      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (!pack.isRemoved() && pack.id.equals(id)) {
            return pack;
         }
      }

      return null;
   }

   public void popPack(final UUID id) {
      ServerPackManager.ServerPackData packInfo = this.findPackInfo(id);
      if (packInfo != null) {
         packInfo.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
         this.registerForUpdate();
      }
   }

   public void popAll() {
      for (ServerPackManager.ServerPackData pack : this.packs) {
         pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
      }

      this.registerForUpdate();
   }

   public void allowServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (!pack.promptAccepted && !pack.isRemoved()) {
            this.acceptPack(pack);
         }
      }

      this.registerForUpdate();
   }

   public void rejectServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.DECLINED;

      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (!pack.promptAccepted) {
            pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DECLINED);
         }
      }

      this.registerForUpdate();
   }

   public void resetPromptStatus() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.PENDING;
   }

   public void tick() {
      boolean downloadsPending = this.updateDownloads();
      if (!downloadsPending) {
         this.triggerReloadIfNeeded();
      }

      this.cleanupRemovedPacks();
   }

   private void cleanupRemovedPacks() {
      this.packs.removeIf(data -> {
         if (data.activationStatus != ServerPackManager.ActivationStatus.INACTIVE) {
            return false;
         }

         if (data.removalReason != null) {
            PackLoadFeedback.FinalResult response = data.removalReason.serverResponse;
            if (response != null) {
               this.packLoadFeedback.reportFinalResult(data.id, response);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   private void onDownload(final Collection<ServerPackManager.ServerPackData> data, final DownloadQueue.BatchResult result) {
      if (!result.failed().isEmpty()) {
         for (ServerPackManager.ServerPackData pack : this.packs) {
            if (pack.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               if (result.failed().contains(pack.id)) {
                  pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DOWNLOAD_FAILED);
               } else {
                  pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
               }
            }
         }
      }

      for (ServerPackManager.ServerPackData pack : data) {
         Path packFile = result.downloaded().get(pack.id);
         if (packFile != null) {
            pack.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
            pack.path = packFile;
            if (!pack.isRemoved()) {
               this.packLoadFeedback.reportUpdate(pack.id, PackLoadFeedback.Update.DOWNLOADED);
            }
         }
      }

      this.registerForUpdate();
   }

   private boolean updateDownloads() {
      List<ServerPackManager.ServerPackData> downloadPacks = new ArrayList<>();
      boolean downloadsInProgress = false;

      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (!pack.isRemoved() && pack.promptAccepted) {
            if (pack.downloadStatus != ServerPackManager.PackDownloadStatus.DONE) {
               downloadsInProgress = true;
            }

            if (pack.downloadStatus == ServerPackManager.PackDownloadStatus.REQUESTED) {
               pack.downloadStatus = ServerPackManager.PackDownloadStatus.PENDING;
               downloadPacks.add(pack);
            }
         }
      }

      if (!downloadPacks.isEmpty()) {
         Map<UUID, DownloadQueue.DownloadRequest> downloadRequests = new HashMap<>();

         for (ServerPackManager.ServerPackData pack : downloadPacks) {
            downloadRequests.put(pack.id, new DownloadQueue.DownloadRequest(pack.url, pack.hash));
         }

         this.downloader.download(downloadRequests, result -> this.onDownload(downloadPacks, result));
      }

      return downloadsInProgress;
   }

   private void triggerReloadIfNeeded() {
      boolean needsReload = false;
      final List<ServerPackManager.ServerPackData> packsToLoad = new ArrayList<>();
      final List<ServerPackManager.ServerPackData> packsToUnload = new ArrayList<>();

      for (ServerPackManager.ServerPackData pack : this.packs) {
         if (pack.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
            return;
         }

         boolean shouldBeActive = pack.promptAccepted && pack.downloadStatus == ServerPackManager.PackDownloadStatus.DONE && !pack.isRemoved();
         if (shouldBeActive && pack.activationStatus == ServerPackManager.ActivationStatus.INACTIVE) {
            packsToLoad.add(pack);
            needsReload = true;
         }

         if (pack.activationStatus == ServerPackManager.ActivationStatus.ACTIVE) {
            if (!shouldBeActive) {
               needsReload = true;
               packsToUnload.add(pack);
            } else {
               packsToLoad.add(pack);
            }
         }
      }

      if (needsReload) {
         for (ServerPackManager.ServerPackData pack : packsToLoad) {
            if (pack.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               pack.activationStatus = ServerPackManager.ActivationStatus.PENDING;
            }
         }

         for (ServerPackManager.ServerPackData pack : packsToUnload) {
            pack.activationStatus = ServerPackManager.ActivationStatus.PENDING;
         }

         this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks() {
            @Override
            public void onSuccess() {
               for (ServerPackManager.ServerPackData pack : packsToLoad) {
                  pack.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                  if (pack.removalReason == null) {
                     ServerPackManager.this.packLoadFeedback.reportFinalResult(pack.id, PackLoadFeedback.FinalResult.APPLIED);
                  }
               }

               for (ServerPackManager.ServerPackData pack : packsToUnload) {
                  pack.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
               }

               ServerPackManager.this.registerForUpdate();
            }

            @Override
            public void onFailure(final boolean isRecovery) {
               if (!isRecovery) {
                  packsToLoad.clear();

                  for (ServerPackManager.ServerPackData pack : ServerPackManager.this.packs) {
                     switch (pack.activationStatus) {
                        case INACTIVE:
                           pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
                           break;
                        case PENDING:
                           pack.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                           pack.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.ACTIVATION_FAILED);
                           break;
                        case ACTIVE:
                           packsToLoad.add(pack);
                     }
                  }

                  ServerPackManager.this.registerForUpdate();
               } else {
                  for (ServerPackManager.ServerPackData pack : ServerPackManager.this.packs) {
                     if (pack.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
                        pack.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                     }
                  }
               }
            }

            @Override
            public List<PackReloadConfig.IdAndPath> packsToLoad() {
               return packsToLoad.stream().map(pack -> new PackReloadConfig.IdAndPath(pack.id, pack.path)).toList();
            }
         });
      }
   }

   private enum ActivationStatus {
      INACTIVE,
      PENDING,
      ACTIVE;
   }

   private enum PackDownloadStatus {
      REQUESTED,
      PENDING,
      DONE;
   }

   public enum PackPromptStatus {
      PENDING,
      ALLOWED,
      DECLINED;
   }

   private enum RemovalReason {
      DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
      ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
      DECLINED(PackLoadFeedback.FinalResult.DECLINED),
      DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
      SERVER_REMOVED(null),
      SERVER_REPLACED(null);

      private final PackLoadFeedback.@Nullable FinalResult serverResponse;

      RemovalReason(final PackLoadFeedback.@Nullable FinalResult serverResponse) {
         this.serverResponse = serverResponse;
      }
   }

   private static class ServerPackData {
      private final UUID id;
      private final URL url;
      private final @Nullable HashCode hash;
      private @Nullable Path path;
      private ServerPackManager.@Nullable RemovalReason removalReason;
      private ServerPackManager.PackDownloadStatus downloadStatus = ServerPackManager.PackDownloadStatus.REQUESTED;
      private ServerPackManager.ActivationStatus activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
      private boolean promptAccepted;

      private ServerPackData(final UUID id, final URL url, final @Nullable HashCode hash) {
         this.id = id;
         this.url = url;
         this.hash = hash;
      }

      public void setRemovalReasonIfNotSet(final ServerPackManager.RemovalReason removalReason) {
         if (this.removalReason == null) {
            this.removalReason = removalReason;
         }
      }

      public boolean isRemoved() {
         return this.removalReason != null;
      }
   }
}
