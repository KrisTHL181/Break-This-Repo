package net.minecraft.server.packs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FileUtil;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Util;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DownloadQueue implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_KEPT_PACKS = 20;
   private final Path cacheDir;
   private final JsonEventLog<DownloadQueue.LogEntry> eventLog;
   private final ConsecutiveExecutor tasks = new ConsecutiveExecutor(Util.nonCriticalIoPool(), "download-queue");

   public DownloadQueue(Path p_311573_) throws IOException {
      this.cacheDir = p_311573_;
      FileUtil.createDirectoriesSafe(p_311573_);
      this.eventLog = JsonEventLog.open(DownloadQueue.LogEntry.CODEC, p_311573_.resolve("log.json"));
      DownloadCacheCleaner.vacuumCacheDir(p_311573_, 20);
   }

   private DownloadQueue.BatchResult runDownload(DownloadQueue.BatchConfig p_312964_, Map<UUID, DownloadQueue.DownloadRequest> p_311709_) {
      DownloadQueue.BatchResult downloadqueue$batchresult = new DownloadQueue.BatchResult();
      p_311709_.forEach(
         (p_311290_, p_311466_) -> {
            Path path = this.cacheDir.resolve(p_311290_.toString());
            Path path1 = null;

            try {
               path1 = HttpUtil.downloadFile(
                  path, p_311466_.url, p_312964_.headers, p_312964_.hashFunction, p_311466_.hash, p_312964_.maxSize, p_312964_.proxy, p_312964_.listener
               );
               downloadqueue$batchresult.downloaded.put(p_311290_, path1);
            } catch (Exception exception1) {
               LOGGER.error("Failed to download {}", p_311466_.url, exception1);
               downloadqueue$batchresult.failed.add(p_311290_);
            }

            try {
               this.eventLog
                  .write(
                     new DownloadQueue.LogEntry(
                        p_311290_,
                        p_311466_.url.toString(),
                        Instant.now(),
                        Optional.ofNullable(p_311466_.hash).map(HashCode::toString),
                        path1 != null ? this.getFileInfo(path1) : Either.left("download_failed")
                     )
                  );
            } catch (Exception exception) {
               LOGGER.error("Failed to log download of {}", p_311466_.url, exception);
            }
         }
      );
      return downloadqueue$batchresult;
   }

   private Either<String, DownloadQueue.FileInfoEntry> getFileInfo(Path p_310185_) {
      try {
         long i = Files.size(p_310185_);
         Path path = this.cacheDir.relativize(p_310185_);
         return Either.right(new DownloadQueue.FileInfoEntry(path.toString(), i));
      } catch (IOException ioexception) {
         LOGGER.error("Failed to get file size of {}", p_310185_, ioexception);
         return Either.left("no_access");
      }
   }

   public CompletableFuture<DownloadQueue.BatchResult> downloadBatch(DownloadQueue.BatchConfig p_312532_, Map<UUID, DownloadQueue.DownloadRequest> p_312658_) {
      return CompletableFuture.supplyAsync(() -> this.runDownload(p_312532_, p_312658_), this.tasks::schedule);
   }

   @Override
   public void close() throws IOException {
      this.tasks.close();
      this.eventLog.close();
   }

   public record BatchConfig(HashFunction hashFunction, int maxSize, Map<String, String> headers, Proxy proxy, HttpUtil.DownloadProgressListener listener) {
   }

   public record BatchResult(Map<UUID, Path> downloaded, Set<UUID> failed) {
      public BatchResult() {
         this(new HashMap<>(), new HashSet<>());
      }
   }

   public record DownloadRequest(URL url, @Nullable HashCode hash) {
   }

   record FileInfoEntry(String name, long size) {
      public static final Codec<DownloadQueue.FileInfoEntry> CODEC = RecordCodecBuilder.create(
         p_311514_ -> p_311514_.group(
               Codec.STRING.fieldOf("name").forGetter(DownloadQueue.FileInfoEntry::name),
               Codec.LONG.fieldOf("size").forGetter(DownloadQueue.FileInfoEntry::size)
            )
            .apply(p_311514_, DownloadQueue.FileInfoEntry::new)
      );
   }

   record LogEntry(UUID id, String url, Instant time, Optional<String> hash, Either<String, DownloadQueue.FileInfoEntry> errorOrFileInfo) {
      public static final Codec<DownloadQueue.LogEntry> CODEC = RecordCodecBuilder.create(
         p_310865_ -> p_310865_.group(
               UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DownloadQueue.LogEntry::id),
               Codec.STRING.fieldOf("url").forGetter(DownloadQueue.LogEntry::url),
               ExtraCodecs.INSTANT_ISO8601.fieldOf("time").forGetter(DownloadQueue.LogEntry::time),
               Codec.STRING.optionalFieldOf("hash").forGetter(DownloadQueue.LogEntry::hash),
               Codec.mapEither(Codec.STRING.fieldOf("error"), DownloadQueue.FileInfoEntry.CODEC.fieldOf("file"))
                  .forGetter(DownloadQueue.LogEntry::errorOrFileInfo)
            )
            .apply(p_310865_, DownloadQueue.LogEntry::new)
      );
   }
}
