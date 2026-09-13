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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ServerPackManager {
   private final PackDownloader downloader;
   final PackLoadFeedback packLoadFeedback;
   private final PackReloadConfig reloadConfig;
   private final Runnable updateRequest;
   private ServerPackManager.PackPromptStatus packPromptStatus;
   final List<ServerPackManager.ServerPackData> packs = new ArrayList<>();

   public ServerPackManager(
      PackDownloader p_313039_, PackLoadFeedback p_311463_, PackReloadConfig p_312595_, Runnable p_310909_, ServerPackManager.PackPromptStatus p_311512_
   ) {
      this.downloader = p_313039_;
      this.packLoadFeedback = p_311463_;
      this.reloadConfig = p_312595_;
      this.updateRequest = p_310909_;
      this.packPromptStatus = p_311512_;
   }

   void registerForUpdate() {
      this.updateRequest.run();
   }

   private void markExistingPacksAsRemoved(UUID p_309694_) {
      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (serverpackmanager$serverpackdata.id.equals(p_309694_)) {
            serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REPLACED);
         }
      }
   }

   public void pushPack(UUID p_309690_, URL p_312710_, @Nullable HashCode p_312316_) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(p_309690_, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         this.pushNewPack(p_309690_, new ServerPackManager.ServerPackData(p_309690_, p_312710_, p_312316_));
      }
   }

   public void pushLocalPack(UUID p_312688_, Path p_312014_) {
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.DECLINED) {
         this.packLoadFeedback.reportFinalResult(p_312688_, PackLoadFeedback.FinalResult.DECLINED);
      } else {
         URL url;
         try {
            url = p_312014_.toUri().toURL();
         } catch (MalformedURLException malformedurlexception) {
            throw new IllegalStateException("Can't convert path to URL " + p_312014_, malformedurlexception);
         }

         ServerPackManager.ServerPackData serverpackmanager$serverpackdata = new ServerPackManager.ServerPackData(p_312688_, url, null);
         serverpackmanager$serverpackdata.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
         serverpackmanager$serverpackdata.path = p_312014_;
         this.pushNewPack(p_312688_, serverpackmanager$serverpackdata);
      }
   }

   private void pushNewPack(UUID p_312820_, ServerPackManager.ServerPackData p_310310_) {
      this.markExistingPacksAsRemoved(p_312820_);
      this.packs.add(p_310310_);
      if (this.packPromptStatus == ServerPackManager.PackPromptStatus.ALLOWED) {
         this.acceptPack(p_310310_);
      }

      this.registerForUpdate();
   }

   private void acceptPack(ServerPackManager.ServerPackData p_309901_) {
      this.packLoadFeedback.reportUpdate(p_309901_.id, PackLoadFeedback.Update.ACCEPTED);
      p_309901_.promptAccepted = true;
   }

   private ServerPackManager.@Nullable ServerPackData findPackInfo(UUID p_312512_) {
      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (!serverpackmanager$serverpackdata.isRemoved() && serverpackmanager$serverpackdata.id.equals(p_312512_)) {
            return serverpackmanager$serverpackdata;
         }
      }

      return null;
   }

   public void popPack(UUID p_312676_) {
      ServerPackManager.ServerPackData serverpackmanager$serverpackdata = this.findPackInfo(p_312676_);
      if (serverpackmanager$serverpackdata != null) {
         serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
         this.registerForUpdate();
      }
   }

   public void popAll() {
      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.SERVER_REMOVED);
      }

      this.registerForUpdate();
   }

   public void allowServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (!serverpackmanager$serverpackdata.promptAccepted && !serverpackmanager$serverpackdata.isRemoved()) {
            this.acceptPack(serverpackmanager$serverpackdata);
         }
      }

      this.registerForUpdate();
   }

   public void rejectServerPacks() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.DECLINED;

      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (!serverpackmanager$serverpackdata.promptAccepted) {
            serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DECLINED);
         }
      }

      this.registerForUpdate();
   }

   public void resetPromptStatus() {
      this.packPromptStatus = ServerPackManager.PackPromptStatus.PENDING;
   }

   public void tick() {
      boolean flag = this.updateDownloads();
      if (!flag) {
         this.triggerReloadIfNeeded();
      }

      this.cleanupRemovedPacks();
   }

   private void cleanupRemovedPacks() {
      this.packs.removeIf(p_312551_ -> {
         if (p_312551_.activationStatus != ServerPackManager.ActivationStatus.INACTIVE) {
            return false;
         }

         if (p_312551_.removalReason != null) {
            PackLoadFeedback.FinalResult packloadfeedback$finalresult = p_312551_.removalReason.serverResponse;
            if (packloadfeedback$finalresult != null) {
               this.packLoadFeedback.reportFinalResult(p_312551_.id, packloadfeedback$finalresult);
            }

            return true;
         } else {
            return false;
         }
      });
   }

   private void onDownload(Collection<ServerPackManager.ServerPackData> p_311905_, DownloadQueue.BatchResult p_312404_) {
      if (!p_312404_.failed().isEmpty()) {
         for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
            if (serverpackmanager$serverpackdata.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               if (p_312404_.failed().contains(serverpackmanager$serverpackdata.id)) {
                  serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DOWNLOAD_FAILED);
               } else {
                  serverpackmanager$serverpackdata.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
               }
            }
         }
      }

      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata1 : p_311905_) {
         Path path = p_312404_.downloaded().get(serverpackmanager$serverpackdata1.id);
         if (path != null) {
            serverpackmanager$serverpackdata1.downloadStatus = ServerPackManager.PackDownloadStatus.DONE;
            serverpackmanager$serverpackdata1.path = path;
            if (!serverpackmanager$serverpackdata1.isRemoved()) {
               this.packLoadFeedback.reportUpdate(serverpackmanager$serverpackdata1.id, PackLoadFeedback.Update.DOWNLOADED);
            }
         }
      }

      this.registerForUpdate();
   }

   private boolean updateDownloads() {
      List<ServerPackManager.ServerPackData> list = new ArrayList<>();
      boolean flag = false;

      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (!serverpackmanager$serverpackdata.isRemoved() && serverpackmanager$serverpackdata.promptAccepted) {
            if (serverpackmanager$serverpackdata.downloadStatus != ServerPackManager.PackDownloadStatus.DONE) {
               flag = true;
            }

            if (serverpackmanager$serverpackdata.downloadStatus == ServerPackManager.PackDownloadStatus.REQUESTED) {
               serverpackmanager$serverpackdata.downloadStatus = ServerPackManager.PackDownloadStatus.PENDING;
               list.add(serverpackmanager$serverpackdata);
            }
         }
      }

      if (!list.isEmpty()) {
         Map<UUID, DownloadQueue.DownloadRequest> map = new HashMap<>();

         for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata1 : list) {
            map.put(
               serverpackmanager$serverpackdata1.id,
               new DownloadQueue.DownloadRequest(serverpackmanager$serverpackdata1.url, serverpackmanager$serverpackdata1.hash)
            );
         }

         this.downloader.download(map, p_310750_ -> this.onDownload(list, p_310750_));
      }

      return flag;
   }

   private void triggerReloadIfNeeded() {
      boolean flag = false;
      final List<ServerPackManager.ServerPackData> list = new ArrayList<>();
      final List<ServerPackManager.ServerPackData> list1 = new ArrayList<>();

      for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata : this.packs) {
         if (serverpackmanager$serverpackdata.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
            return;
         }

         boolean flag1 = serverpackmanager$serverpackdata.promptAccepted
            && serverpackmanager$serverpackdata.downloadStatus == ServerPackManager.PackDownloadStatus.DONE
            && !serverpackmanager$serverpackdata.isRemoved();
         if (flag1 && serverpackmanager$serverpackdata.activationStatus == ServerPackManager.ActivationStatus.INACTIVE) {
            list.add(serverpackmanager$serverpackdata);
            flag = true;
         }

         if (serverpackmanager$serverpackdata.activationStatus == ServerPackManager.ActivationStatus.ACTIVE) {
            if (!flag1) {
               flag = true;
               list1.add(serverpackmanager$serverpackdata);
            } else {
               list.add(serverpackmanager$serverpackdata);
            }
         }
      }

      if (flag) {
         for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata1 : list) {
            if (serverpackmanager$serverpackdata1.activationStatus != ServerPackManager.ActivationStatus.ACTIVE) {
               serverpackmanager$serverpackdata1.activationStatus = ServerPackManager.ActivationStatus.PENDING;
            }
         }

         for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata2 : list1) {
            serverpackmanager$serverpackdata2.activationStatus = ServerPackManager.ActivationStatus.PENDING;
         }

         this.reloadConfig.scheduleReload(new PackReloadConfig.Callbacks() {
            @Override
            public void onSuccess() {
               for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata3 : list) {
                  serverpackmanager$serverpackdata3.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                  if (serverpackmanager$serverpackdata3.removalReason == null) {
                     ServerPackManager.this.packLoadFeedback.reportFinalResult(serverpackmanager$serverpackdata3.id, PackLoadFeedback.FinalResult.APPLIED);
                  }
               }

               for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata4 : list1) {
                  serverpackmanager$serverpackdata4.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
               }

               ServerPackManager.this.registerForUpdate();
            }

            @Override
            public void onFailure(boolean p_311939_) {
               if (!p_311939_) {
                  list.clear();

                  for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata3 : ServerPackManager.this.packs) {
                     switch (serverpackmanager$serverpackdata3.activationStatus) {
                        case INACTIVE:
                           serverpackmanager$serverpackdata3.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.DISCARDED);
                           break;
                        case PENDING:
                           serverpackmanager$serverpackdata3.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                           serverpackmanager$serverpackdata3.setRemovalReasonIfNotSet(ServerPackManager.RemovalReason.ACTIVATION_FAILED);
                           break;
                        case ACTIVE:
                           list.add(serverpackmanager$serverpackdata3);
                     }
                  }

                  ServerPackManager.this.registerForUpdate();
               } else {
                  for (ServerPackManager.ServerPackData serverpackmanager$serverpackdata4 : ServerPackManager.this.packs) {
                     if (serverpackmanager$serverpackdata4.activationStatus == ServerPackManager.ActivationStatus.PENDING) {
                        serverpackmanager$serverpackdata4.activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
                     }
                  }
               }
            }

            @Override
            public List<PackReloadConfig.IdAndPath> packsToLoad() {
               return list.stream().map(p_312955_ -> new PackReloadConfig.IdAndPath(p_312955_.id, p_312955_.path)).toList();
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   enum ActivationStatus {
      INACTIVE,
      PENDING,
      ACTIVE;
   }

   @OnlyIn(Dist.CLIENT)
   enum PackDownloadStatus {
      REQUESTED,
      PENDING,
      DONE;
   }

   @OnlyIn(Dist.CLIENT)
   public enum PackPromptStatus {
      PENDING,
      ALLOWED,
      DECLINED;
   }

   @OnlyIn(Dist.CLIENT)
   enum RemovalReason {
      DOWNLOAD_FAILED(PackLoadFeedback.FinalResult.DOWNLOAD_FAILED),
      ACTIVATION_FAILED(PackLoadFeedback.FinalResult.ACTIVATION_FAILED),
      DECLINED(PackLoadFeedback.FinalResult.DECLINED),
      DISCARDED(PackLoadFeedback.FinalResult.DISCARDED),
      SERVER_REMOVED(null),
      SERVER_REPLACED(null);

      final PackLoadFeedback.@Nullable FinalResult serverResponse;

      RemovalReason(final PackLoadFeedback.FinalResult p_312250_) {
         this.serverResponse = p_312250_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ServerPackData {
      final UUID id;
      final URL url;
      final @Nullable HashCode hash;
      @Nullable Path path;
      ServerPackManager.@Nullable RemovalReason removalReason;
      ServerPackManager.PackDownloadStatus downloadStatus = ServerPackManager.PackDownloadStatus.REQUESTED;
      ServerPackManager.ActivationStatus activationStatus = ServerPackManager.ActivationStatus.INACTIVE;
      boolean promptAccepted;

      ServerPackData(UUID p_310861_, URL p_310292_, @Nullable HashCode p_311680_) {
         this.id = p_310861_;
         this.url = p_310292_;
         this.hash = p_311680_;
      }

      public void setRemovalReasonIfNotSet(ServerPackManager.RemovalReason p_312334_) {
         if (this.removalReason == null) {
            this.removalReason = p_312334_;
         }
      }

      public boolean isRemoved() {
         return this.removalReason != null;
      }
   }
}
