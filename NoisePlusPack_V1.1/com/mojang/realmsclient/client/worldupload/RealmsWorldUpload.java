package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.UploadResult;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.SharedConstants;
import net.minecraft.client.User;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldUpload {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int UPLOAD_RETRIES = 20;
   private final RealmsClient client = RealmsClient.getOrCreate();
   private final Path worldFolder;
   private final RealmsSlot realmsSlot;
   private final User user;
   private final long realmId;
   private final RealmsWorldUploadStatusTracker statusCallback;
   private volatile boolean cancelled;
   private volatile @Nullable CompletableFuture<?> uploadTask;

   public RealmsWorldUpload(Path p_365252_, RealmsSlot p_408995_, User p_366652_, long p_368429_, RealmsWorldUploadStatusTracker p_361427_) {
      this.worldFolder = p_365252_;
      this.realmsSlot = p_408995_;
      this.user = p_366652_;
      this.realmId = p_368429_;
      this.statusCallback = p_361427_;
   }

   public CompletableFuture<?> packAndUpload() {
      return CompletableFuture.runAsync(
         () -> {
            File file1 = null;

            try {
               UploadInfo uploadinfo = this.requestUploadInfoWithRetries();
               file1 = RealmsUploadWorldPacker.pack(this.worldFolder, () -> this.cancelled);
               this.statusCallback.setUploading();

               try (FileUpload fileupload = new FileUpload(
                     file1,
                     this.realmId,
                     this.realmsSlot.slotId,
                     uploadinfo,
                     this.user,
                     SharedConstants.getCurrentVersion().name(),
                     this.realmsSlot.options.version,
                     this.statusCallback.getUploadStatus()
                  )) {
                  CompletableFuture<UploadResult> completablefuture = fileupload.startUpload();
                  this.uploadTask = completablefuture;
                  if (this.cancelled) {
                     completablefuture.cancel(true);
                     return;
                  }

                  UploadResult uploadresult;
                  try {
                     uploadresult = completablefuture.join();
                  } catch (CompletionException completionexception) {
                     throw completionexception.getCause();
                  }

                  String s = uploadresult.getSimplifiedErrorMessage();
                  if (s != null) {
                     throw new RealmsUploadFailedException(s);
                  }

                  UploadTokenCache.invalidate(this.realmId);
                  this.client.updateSlot(this.realmId, this.realmsSlot.slotId, this.realmsSlot.options, this.realmsSlot.settings);
               }
            } catch (RealmsServiceException realmsserviceexception) {
               throw new RealmsUploadFailedException(realmsserviceexception.realmsError.errorMessage());
            } catch (CancellationException | InterruptedException interruptedexception) {
               throw new RealmsUploadCanceledException();
            } catch (RealmsUploadException realmsuploadexception) {
               throw realmsuploadexception;
            } catch (Throwable throwable1) {
               if (throwable1 instanceof Error error) {
                  throw error;
               }

               throw new RealmsUploadFailedException(throwable1.getMessage());
            } finally {
               if (file1 != null) {
                  LOGGER.debug("Deleting file {}", file1.getAbsolutePath());
                  file1.delete();
               }
            }
         },
         Util.backgroundExecutor()
      );
   }

   public void cancel() {
      this.cancelled = true;
      CompletableFuture<?> completablefuture = this.uploadTask;
      if (completablefuture != null) {
         completablefuture.cancel(true);
      }
   }

   private UploadInfo requestUploadInfoWithRetries() throws RealmsServiceException, InterruptedException {
      for (int i = 0; i < 20; i++) {
         try {
            UploadInfo uploadinfo = this.client.requestUploadInfo(this.realmId);
            if (this.cancelled) {
               throw new RealmsUploadCanceledException();
            }

            if (uploadinfo != null) {
               if (!uploadinfo.worldClosed()) {
                  throw new RealmsUploadWorldNotClosedException();
               }

               return uploadinfo;
            }
         } catch (RetryCallException retrycallexception) {
            Thread.sleep(retrycallexception.delaySeconds * 1000L);
         }
      }

      throw new RealmsUploadWorldNotClosedException();
   }
}
