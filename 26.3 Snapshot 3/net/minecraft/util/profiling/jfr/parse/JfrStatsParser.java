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

   private JfrStatsParser(final Stream<RecordedEvent> events) {
      this.capture(events);
   }

   public static JfrStatsResult parse(final Path path) {
      try (final RecordingFile recordingFile = new RecordingFile(path)) {
         Iterator<RecordedEvent> iterator = new Iterator<RecordedEvent>() {
            @Override
            public boolean hasNext() {
               return recordingFile.hasMoreEvents();
            }

            public RecordedEvent next() {
               if (!this.hasNext()) {
                  throw new NoSuchElementException();
               }

               try {
                  return recordingFile.readEvent();
               } catch (IOException e) {
                  throw new UncheckedIOException(e);
               }
            }
         };
         Stream<RecordedEvent> events = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1297), false);
         return new JfrStatsParser(events).results();
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   private JfrStatsResult results() {
      Duration recordingDuration = Duration.between(this.recordingStarted, this.recordingEnded);
      return new JfrStatsResult(
         this.recordingStarted,
         this.recordingEnded,
         recordingDuration,
         this.worldCreationDuration,
         this.fps,
         this.serverTickTimes,
         this.cpuLoadStat,
         GcHeapStat.summary(recordingDuration, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections),
         ThreadAllocationStat.summary(this.threadAllocationStats),
         collectIoStats(recordingDuration, this.receivedPackets),
         collectIoStats(recordingDuration, this.sentPackets),
         collectIoStats(recordingDuration, this.writtenChunks),
         collectIoStats(recordingDuration, this.readChunks),
         FileIOStat.summary(recordingDuration, this.fileWrites),
         FileIOStat.summary(recordingDuration, this.fileReads),
         this.chunkGenStats,
         this.structureGenStats
      );
   }

   private void capture(final Stream<RecordedEvent> events) {
      events.forEach(event -> {
         if (event.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
            this.recordingEnded = event.getEndTime();
         }

         if (event.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
            this.recordingStarted = event.getStartTime();
         }

         switch (event.getEventType().getName()) {
            case "minecraft.ChunkGeneration":
               this.chunkGenStats.add(ChunkGenStat.from(event));
               break;
            case "minecraft.StructureGeneration":
               this.structureGenStats.add(StructureGenStat.from(event));
               break;
            case "minecraft.LoadWorld":
               this.worldCreationDuration = event.getDuration();
               break;
            case "minecraft.ClientFps":
               this.fps.add(FpsStat.from(event, "fps"));
               break;
            case "minecraft.ServerTickTime":
               this.serverTickTimes.add(TickTimeStat.from(event));
               break;
            case "minecraft.PacketReceived":
               this.incrementPacket(event, event.getInt("bytes"), this.receivedPackets);
               break;
            case "minecraft.PacketSent":
               this.incrementPacket(event, event.getInt("bytes"), this.sentPackets);
               break;
            case "minecraft.ChunkRegionRead":
               this.incrementChunk(event, event.getInt("bytes"), this.readChunks);
               break;
            case "minecraft.ChunkRegionWrite":
               this.incrementChunk(event, event.getInt("bytes"), this.writtenChunks);
               break;
            case "jdk.ThreadAllocationStatistics":
               this.threadAllocationStats.add(ThreadAllocationStat.from(event));
               break;
            case "jdk.GCHeapSummary":
               this.gcHeapStats.add(GcHeapStat.from(event));
               break;
            case "jdk.CPULoad":
               this.cpuLoadStat.add(CpuLoadStat.from(event));
               break;
            case "jdk.FileWrite":
               this.appendFileIO(event, this.fileWrites, "bytesWritten");
               break;
            case "jdk.FileRead":
               this.appendFileIO(event, this.fileReads, "bytesRead");
               break;
            case "jdk.GarbageCollection":
               this.garbageCollections++;
               this.gcTotalDuration = this.gcTotalDuration.plus(event.getDuration());
         }
      });
   }

   private void incrementPacket(final RecordedEvent event, final int packetSize, final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> packets) {
      packets.computeIfAbsent(PacketIdentification.from(event), ignored -> new JfrStatsParser.MutableCountAndSize()).increment(packetSize);
   }

   private void incrementChunk(final RecordedEvent event, final int chunkSize, final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> packets) {
      packets.computeIfAbsent(ChunkIdentification.from(event), ignored -> new JfrStatsParser.MutableCountAndSize()).increment(chunkSize);
   }

   private void appendFileIO(final RecordedEvent event, final List<FileIOStat> stats, final String sizeField) {
      stats.add(new FileIOStat(event.getDuration(), event.getString("path"), event.getLong(sizeField)));
   }

   private static <T> IoSummary<T> collectIoStats(final Duration recordingDuration, final Map<T, JfrStatsParser.MutableCountAndSize> packetStats) {
      List<Pair<T, IoSummary.CountAndSize>> summaryStats = packetStats.entrySet()
         .stream()
         .map(e -> Pair.of(e.getKey(), e.getValue().toCountAndSize()))
         .toList();
      return new IoSummary<>(recordingDuration, summaryStats);
   }

   public static final class MutableCountAndSize {
      private long count;
      private long totalSize;

      public void increment(final int bytes) {
         this.totalSize += bytes;
         this.count++;
      }

      public IoSummary.CountAndSize toCountAndSize() {
         return new IoSummary.CountAndSize(this.count, this.totalSize);
      }
   }
}
