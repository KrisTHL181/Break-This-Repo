package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.CheckReturnValue;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileDownload {
   private static final Logger LOGGER = LogUtils.getLogger();
   private volatile boolean cancelled;
   private volatile boolean finished;
   private volatile boolean error;
   private volatile boolean extracting;
   private volatile @Nullable File tempFile;
   private volatile File resourcePackPath;
   private volatile @Nullable CompletableFuture<?> pendingRequest;
   private @Nullable Thread currentThread;
   private static final String[] INVALID_FILE_NAMES = new String[]{
      "CON",
      "COM",
      "PRN",
      "AUX",
      "CLOCK$",
      "NUL",
      "COM1",
      "COM2",
      "COM3",
      "COM4",
      "COM5",
      "COM6",
      "COM7",
      "COM8",
      "COM9",
      "LPT1",
      "LPT2",
      "LPT3",
      "LPT4",
      "LPT5",
      "LPT6",
      "LPT7",
      "LPT8",
      "LPT9"
   };

   private <T> @Nullable T joinCancellableRequest(final CompletableFuture<T> pendingRequest) throws Throwable {
      this.pendingRequest = pendingRequest;
      if (this.cancelled) {
         pendingRequest.cancel(true);
         return null;
      }

      try {
         try {
            return pendingRequest.join();
         } catch (CompletionException e) {
            throw e.getCause();
         }
      } catch (CancellationException e) {
         return null;
      }
   }

   private static HttpClient createClient() {
      return HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofMinutes(2L)).build();
   }

   private static Builder createRequest(final String downloadLink) {
      return HttpRequest.newBuilder(URI.create(downloadLink)).timeout(Duration.ofMinutes(2L));
   }

   @CheckReturnValue
   public static OptionalLong contentLength(final String downloadLink) {
      try (HttpClient client = createClient()) {
         HttpResponse<Void> response = client.send(createRequest(downloadLink).HEAD().build(), BodyHandlers.discarding());
         return response.headers().firstValueAsLong("Content-Length");
      } catch (Exception e) {
         LOGGER.error("Unable to get content length for download");
         return OptionalLong.empty();
      }
   }

   public void download(
      final WorldDownload worldDownload,
      final String worldName,
      final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus,
      final LevelStorageSource levelStorageSource
   ) {
      if (this.currentThread == null) {
         this.currentThread = new Thread(() -> {
            try (HttpClient client = createClient()) {
               try {
                  this.tempFile = File.createTempFile("backup", ".tar.gz");
                  this.download(downloadStatus, client, worldDownload.downloadLink(), this.tempFile);
                  this.finishWorldDownload(worldName.trim(), this.tempFile, levelStorageSource, downloadStatus);
               } catch (Exception e) {
                  LOGGER.error("Caught exception while downloading world", e);
                  this.error = true;
               } finally {
                  this.pendingRequest = null;
                  if (this.tempFile != null) {
                     this.tempFile.delete();
                  }

                  this.tempFile = null;
               }

               if (this.error) {
                  return;
               }

               String resourcePackLink = worldDownload.resourcePackUrl();
               if (!resourcePackLink.isEmpty() && !worldDownload.resourcePackHash().isEmpty()) {
                  try {
                     this.tempFile = File.createTempFile("resources", ".tar.gz");
                     this.download(downloadStatus, client, resourcePackLink, this.tempFile);
                     this.finishResourcePackDownload(downloadStatus, this.tempFile, worldDownload);
                  } catch (Exception e) {
                     LOGGER.error("Caught exception while downloading resource pack", e);
                     this.error = true;
                  } finally {
                     this.pendingRequest = null;
                     if (this.tempFile != null) {
                        this.tempFile.delete();
                     }

                     this.tempFile = null;
                  }
               }

               this.finished = true;
            }
         }, "Realms world download");
         this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
         this.currentThread.start();
      }
   }

   private void download(final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, final HttpClient client, final String url, final File target) throws IOException {
      HttpRequest request = createRequest(url).GET().build();

      HttpResponse<InputStream> response;
      try {
         response = this.joinCancellableRequest(client.sendAsync(request, BodyHandlers.ofInputStream()));
      } catch (Error e) {
         throw e;
      } catch (Throwable e) {
         LOGGER.error("Failed to download {}", url, e);
         this.error = true;
         return;
      }

      if (response != null && !this.cancelled) {
         if (response.statusCode() != 200) {
            this.error = true;
         } else {
            downloadStatus.totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0L);

            try (
               InputStream is = response.body();
               OutputStream os = new FileOutputStream(target);
            ) {
               is.transferTo(new FileDownload.DownloadCountingOutputStream(os, downloadStatus));
            }
         }
      }
   }

   public void cancel() {
      if (this.tempFile != null) {
         this.tempFile.delete();
         this.tempFile = null;
      }

      this.cancelled = true;
      CompletableFuture<?> pendingRequest = this.pendingRequest;
      if (pendingRequest != null) {
         pendingRequest.cancel(true);
      }
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isError() {
      return this.error;
   }

   public boolean isExtracting() {
      return this.extracting;
   }

   public static String findAvailableFolderName(String folder) {
      folder = folder.replaceAll("[\\./\"]", "_");

      for (String invalidName : INVALID_FILE_NAMES) {
         if (folder.equalsIgnoreCase(invalidName)) {
            folder = "_" + folder + "_";
         }
      }

      return folder;
   }

   private void untarGzipArchive(String name, final @Nullable File file, final LevelStorageSource levelStorageSource) throws IOException {
      Pattern namePattern = Pattern.compile(".*-([0-9]+)$");
      int number = 1;

      for (char replacer : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         name = name.replace(replacer, '_');
      }

      if (StringUtils.isEmpty(name)) {
         name = "Realm";
      }

      name = findAvailableFolderName(name);

      try {
         for (LevelStorageSource.LevelDirectory level : levelStorageSource.findLevelCandidates()) {
            String levelId = level.directoryName();
            if (levelId.toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT))) {
               Matcher matcher = namePattern.matcher(levelId);
               if (matcher.matches()) {
                  int parsedNumber = Integer.parseInt(matcher.group(1));
                  if (parsedNumber > number) {
                     number = parsedNumber;
                  }
               } else {
                  number++;
               }
            }
         }
      } catch (Exception e) {
         LOGGER.error("Error getting level list", e);
         this.error = true;
         return;
      }

      String finalName;
      if (levelStorageSource.isNewLevelIdAcceptable(name) && number <= 1) {
         finalName = name;
      } else {
         finalName = name + (number == 1 ? "" : "-" + number);
         if (!levelStorageSource.isNewLevelIdAcceptable(finalName)) {
            boolean foundName = false;

            while (!foundName) {
               number++;
               finalName = name + (number == 1 ? "" : "-" + number);
               if (levelStorageSource.isNewLevelIdAcceptable(finalName)) {
                  foundName = true;
               }
            }
         }
      }

      TarArchiveInputStream tarIn = null;
      Path worldPath = Minecraft.getInstance().getLevelSource().getLevelPath(finalName).normalize();

      try {
         FileUtil.createDirectoriesSafe(worldPath);
         tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(file))));
         TarArchiveEntry tarEntry = tarIn.getNextTarEntry();

         while (tarEntry != null) {
            Path destPath = worldPath.resolve(Path.of("world").relativize(Path.of(tarEntry.getName()))).normalize();
            if (!destPath.startsWith(worldPath)) {
               LOGGER.warn("Unexpected entry in Realms world download: {}", tarEntry.getName());
               tarEntry = tarIn.getNextTarEntry();
            } else {
               if (tarEntry.isDirectory()) {
                  FileUtil.createDirectoriesSafe(destPath);
               } else {
                  Path parent = destPath.getParent();
                  if (parent != null) {
                     FileUtil.createDirectoriesSafe(parent);
                  }

                  try (FileOutputStream output = new FileOutputStream(destPath.toFile())) {
                     IOUtils.copy(tarIn, output);
                  }
               }

               tarEntry = tarIn.getNextTarEntry();
            }
         }
      } catch (Exception e) {
         LOGGER.error("Error extracting world", e);
         this.error = true;
      } finally {
         if (tarIn != null) {
            tarIn.close();
         }

         if (file != null) {
            file.delete();
         }

         try (LevelStorageSource.LevelStorageAccess access = levelStorageSource.validateAndCreateAccess(finalName)) {
            access.renameAndDropPlayer(finalName);
         } catch (IOException | NbtException | ReportedNbtException e) {
            LOGGER.error("Failed to modify unpacked realms level {}", finalName, e);
         } catch (ContentValidationException e) {
            LOGGER.warn("Failed to download file", e);
         }

         this.resourcePackPath = worldPath.resolve(LevelResource.MAP_RESOURCE_FILE.id()).toFile();
      }
   }

   private void finishWorldDownload(
      final String worldName,
      final File tempFile,
      final LevelStorageSource levelStorageSource,
      final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus
   ) {
      if (downloadStatus.bytesWritten >= downloadStatus.totalBytes && !this.cancelled && !this.error) {
         try {
            this.extracting = true;
            this.untarGzipArchive(worldName, tempFile, levelStorageSource);
         } catch (IOException e) {
            LOGGER.error("Error extracting archive", e);
            this.error = true;
         }
      }
   }

   private void finishResourcePackDownload(
      final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, final File tempFile, final WorldDownload worldDownload
   ) {
      if (downloadStatus.bytesWritten >= downloadStatus.totalBytes && !this.cancelled) {
         try {
            String actualHash = Hashing.sha1().hashBytes(Files.toByteArray(tempFile)).toString();
            if (actualHash.equals(worldDownload.resourcePackHash())) {
               FileUtils.copyFile(tempFile, this.resourcePackPath);
               this.finished = true;
            } else {
               LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", worldDownload.resourcePackHash(), actualHash);
               FileUtils.deleteQuietly(tempFile);
               this.error = true;
            }
         } catch (IOException e) {
            LOGGER.error("Error copying resourcepack file", e);
            this.error = true;
         }
      }
   }

   private static class DownloadCountingOutputStream extends CountingOutputStream {
      private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

      public DownloadCountingOutputStream(final OutputStream out, final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
         super(out);
         this.downloadStatus = downloadStatus;
      }

      protected void afterWrite(final int n) throws IOException {
         super.afterWrite(n);
         this.downloadStatus.bytesWritten = this.getByteCount();
      }
   }
}
