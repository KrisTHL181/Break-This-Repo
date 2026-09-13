package net.minecraft.util.profiling.jfr.parse;

import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.ChunkIdentification;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.FpsStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.IoSummary;
import net.minecraft.util.profiling.jfr.stats.PacketIdentification;
import net.minecraft.util.profiling.jfr.stats.StructureGenStat;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;
import org.jspecify.annotations.Nullable;

public class JfrStatsParser {
   private Instant recordingStarted = Instant.EPOCH;
   private Instant recordingEnded = Instant.EPOCH;
   private final List<ChunkGenStat> chunkGenStats = new ArrayList<>();
   private final List<StructureGenStat> structureGenStats = new ArrayList<>();
   private final List<CpuLoadStat> cpuLoadStat = new ArrayList<>();
   private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> receivedPackets = new HashMap<>();
   private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> sentPackets = new HashMap<>();
   private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> readChunks = new HashMap<>();
   private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> writtenChunks = new HashMap<>();
   private final List<FileIOStat> fileWrites = new ArrayList<>();
   private final List<FileIOStat> fileReads = new ArrayList<>();
   private int garbageCollections;
   private Duration gcTotalDuration = Duration.ZERO;
   private final List<GcHeapStat> gcHeapStats = new ArrayList<>();
   private final List<ThreadAllocationStat> threadAllocationStats = new ArrayList<>();
   private final List<FpsStat> fps = new ArrayList<>();
   private final List<TickTimeStat> serverTickTimes = new ArrayList<>();
   private @Nullable Duration worldCreationDuration = null;

   private JfrStatsParser(Stream<RecordedEvent> p_185443_) {
      this.capture(p_185443_);
   }

   public static JfrStatsResult parse(Path p_185448_) {
      try (final RecordingFile recordingfile = new RecordingFile(p_185448_)) {
         Iterator<RecordedEvent> iterator = new Iterator<RecordedEvent>() {
            @Override
            public boolean hasNext() {
               return recordingfile.hasMoreEvents();
            }

            public RecordedEvent next() {
               if (!this.hasNext()) {
                  throw new NoSuchElementException();
               }

               try {
                  return recordingfile.readEvent();
               } catch (IOException ioexception1) {
                  throw new UncheckedIOException(ioexception1);
               }
            }
         };
         Stream<RecordedEvent> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1297), false);
         return new JfrStatsParser(stream).results();
      } catch (IOException ioexception) {
         throw new UncheckedIOException(ioexception);
      }
   }

   private JfrStatsResult results() {
      Duration duration = Duration.between(this.recordingStarted, this.recordingEnded);
      return new JfrStatsResult(
         this.recordingStarted,
         this.recordingEnded,
         duration,
         this.worldCreationDuration,
         this.fps,
         this.serverTickTimes,
         this.cpuLoadStat,
         GcHeapStat.summary(duration, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections),
         ThreadAllocationStat.summary(this.threadAllocationStats),
         collectIoStats(duration, this.receivedPackets),
         collectIoStats(duration, this.sentPackets),
         collectIoStats(duration, this.writtenChunks),
         collectIoStats(duration, this.readChunks),
         FileIOStat.summary(duration, this.fileWrites),
         FileIOStat.summary(duration, this.fileReads),
         this.chunkGenStats,
         this.structureGenStats
      );
   }

   private void capture(Stream<RecordedEvent> p_185455_) {
      p_185455_.forEach(p_449356_ -> {
         if (p_449356_.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
            this.recordingEnded = p_449356_.getEndTime();
         }

         if (p_449356_.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
            this.recordingStarted = p_449356_.getStartTime();
         }

         String s = p_449356_.getEventType().getName();
         switch (s) {
            case "minecraft.ChunkGeneration":
               this.chunkGenStats.add(ChunkGenStat.from(p_449356_));
               break;
            case "minecraft.StructureGeneration":
               this.structureGenStats.add(StructureGenStat.from(p_449356_));
               break;
            case "minecraft.LoadWorld":
               this.worldCreationDuration = p_449356_.getDuration();
               break;
            case "minecraft.ClientFps":
               this.fps.add(FpsStat.from(p_449356_, "fps"));
               break;
            case "minecraft.ServerTickTime":
               this.serverTickTimes.add(TickTimeStat.from(p_449356_));
               break;
            case "minecraft.PacketReceived":
               this.incrementPacket(p_449356_, p_449356_.getInt("bytes"), this.receivedPackets);
               break;
            case "minecraft.PacketSent":
               this.incrementPacket(p_449356_, p_449356_.getInt("bytes"), this.sentPackets);
               break;
            case "minecraft.ChunkRegionRead":
               this.incrementChunk(p_449356_, p_449356_.getInt("bytes"), this.readChunks);
               break;
            case "minecraft.ChunkRegionWrite":
               this.incrementChunk(p_449356_, p_449356_.getInt("bytes"), this.writtenChunks);
               break;
            case "jdk.ThreadAllocationStatistics":
               this.threadAllocationStats.add(ThreadAllocationStat.from(p_449356_));
               break;
            case "jdk.GCHeapSummary":
               this.gcHeapStats.add(GcHeapStat.from(p_449356_));
               break;
            case "jdk.CPULoad":
               this.cpuLoadStat.add(CpuLoadStat.from(p_449356_));
               break;
            case "jdk.FileWrite":
               this.appendFileIO(p_449356_, this.fileWrites, "bytesWritten");
               break;
            case "jdk.FileRead":
               this.appendFileIO(p_449356_, this.fileReads, "bytesRead");
               break;
            case "jdk.GarbageCollection":
               this.garbageCollections++;
               this.gcTotalDuration = this.gcTotalDuration.plus(p_449356_.getDuration());
         }
      });
   }

   private void incrementPacket(RecordedEvent p_185459_, int p_185460_, Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> p_185461_) {
      p_185461_.computeIfAbsent(PacketIdentification.from(p_185459_), p_326728_ -> new JfrStatsParser.MutableCountAndSize()).increment(p_185460_);
   }

   private void incrementChunk(RecordedEvent p_329550_, int p_328110_, Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> p_329507_) {
      p_329507_.computeIfAbsent(ChunkIdentification.from(p_329550_), p_332913_ -> new JfrStatsParser.MutableCountAndSize()).increment(p_328110_);
   }

   private void appendFileIO(RecordedEvent p_185463_, List<FileIOStat> p_185464_, String p_185465_) {
      p_185464_.add(new FileIOStat(p_185463_.getDuration(), p_185463_.getString("path"), p_185463_.getLong(p_185465_)));
   }

   private static <T> IoSummary<T> collectIoStats(Duration p_333492_, Map<T, JfrStatsParser.MutableCountAndSize> p_336276_) {
      List<Pair<T, IoSummary.CountAndSize>> list = p_336276_.entrySet()
         .stream()
         .map(p_326729_ -> Pair.of(p_326729_.getKey(), p_326729_.getValue().toCountAndSize()))
         .toList();
      return new IoSummary<>(p_333492_, list);
   }

   public static final class MutableCountAndSize {
      private long count;
      private long totalSize;

      public void increment(int p_185477_) {
         this.totalSize += p_185477_;
         this.count++;
      }

      public IoSummary.CountAndSize toCountAndSize() {
         return new IoSummary.CountAndSize(this.count, this.totalSize);
      }
   }
}
