package net.minecraft.util.profiling.jfr.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.LongSerializationPolicy;
import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.Percentiles;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
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
import net.minecraft.util.profiling.jfr.stats.TimedStatSummary;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class JfrResultJsonSerializer {
   private static final String BYTES_PER_SECOND = "bytesPerSecond";
   private static final String COUNT = "count";
   private static final String DURATION_NANOS_TOTAL = "durationNanosTotal";
   private static final String TOTAL_BYTES = "totalBytes";
   private static final String COUNT_PER_SECOND = "countPerSecond";
   final Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

   private static void serializePacketId(PacketIdentification p_335435_, JsonObject p_331788_) {
      p_331788_.addProperty("protocolId", p_335435_.protocolId());
      p_331788_.addProperty("packetId", p_335435_.packetId());
   }

   private static void serializeChunkId(ChunkIdentification p_332094_, JsonObject p_330415_) {
      p_330415_.addProperty("level", p_332094_.level());
      p_330415_.addProperty("dimension", p_332094_.dimension());
      p_330415_.addProperty("x", p_332094_.x());
      p_330415_.addProperty("z", p_332094_.z());
   }

   public String format(JfrStatsResult p_185536_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("startedEpoch", p_185536_.recordingStarted().toEpochMilli());
      jsonobject.addProperty("endedEpoch", p_185536_.recordingEnded().toEpochMilli());
      jsonobject.addProperty("durationMs", p_185536_.recordingDuration().toMillis());
      Duration duration = p_185536_.worldCreationDuration();
      if (duration != null) {
         jsonobject.addProperty("worldGenDurationMs", duration.toMillis());
      }

      jsonobject.add("heap", this.heap(p_185536_.heapSummary()));
      jsonobject.add("cpuPercent", this.cpu(p_185536_.cpuLoadStats()));
      jsonobject.add("network", this.network(p_185536_));
      jsonobject.add("fileIO", this.fileIO(p_185536_));
      jsonobject.add("fps", this.fps(p_185536_.fps()));
      jsonobject.add("serverTick", this.serverTicks(p_185536_.serverTickTimes()));
      jsonobject.add("threadAllocation", this.threadAllocations(p_185536_.threadAllocationSummary()));
      jsonobject.add("chunkGen", this.chunkGen(p_185536_.chunkGenSummary()));
      jsonobject.add("structureGen", this.structureGen(p_185536_.structureGenStats()));
      return this.gson.toJson(jsonobject);
   }

   private JsonElement heap(GcHeapStat.Summary p_185542_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("allocationRateBytesPerSecond", p_185542_.allocationRateBytesPerSecond());
      jsonobject.addProperty("gcCount", p_185542_.totalGCs());
      jsonobject.addProperty("gcOverHeadPercent", p_185542_.gcOverHead());
      jsonobject.addProperty("gcTotalDurationMs", p_185542_.gcTotalDuration().toMillis());
      return jsonobject;
   }

   private JsonElement structureGen(List<StructureGenStat> p_375433_) {
      JsonObject jsonobject = new JsonObject();
      Optional<TimedStatSummary<StructureGenStat>> optional = TimedStatSummary.summary(p_375433_);
      if (optional.isEmpty()) {
         return jsonobject;
      }

      TimedStatSummary<StructureGenStat> timedstatsummary = optional.get();
      JsonArray jsonarray = new JsonArray();
      jsonobject.add("structure", jsonarray);
      p_375433_.stream()
         .collect(Collectors.groupingBy(StructureGenStat::structureName))
         .forEach(
            (p_449369_, p_449370_) -> {
               Optional<TimedStatSummary<StructureGenStat>> optional1 = TimedStatSummary.summary((List<StructureGenStat>)p_449370_);
               if (!optional1.isEmpty()) {
                  TimedStatSummary<StructureGenStat> timedstatsummary1 = optional1.get();
                  JsonObject jsonobject1 = new JsonObject();
                  jsonarray.add(jsonobject1);
                  jsonobject1.addProperty("name", p_449369_);
                  jsonobject1.addProperty("count", timedstatsummary1.count());
                  jsonobject1.addProperty("durationNanosTotal", timedstatsummary1.totalDuration().toNanos());
                  jsonobject1.addProperty("durationNanosAvg", timedstatsummary1.totalDuration().toNanos() / timedstatsummary1.count());
                  JsonObject jsonobject2 = Util.make(new JsonObject(), p_374916_ -> jsonobject1.add("durationNanosPercentiles", p_374916_));
                  timedstatsummary1.percentilesNanos().forEach((p_449364_, p_449365_) -> jsonobject2.addProperty("p" + p_449364_, p_449365_));
                  Function<StructureGenStat, JsonElement> function = p_374920_ -> {
                     JsonObject jsonobject3 = new JsonObject();
                     jsonobject3.addProperty("durationNanos", p_374920_.duration().toNanos());
                     jsonobject3.addProperty("chunkPosX", p_374920_.chunkPos().x);
                     jsonobject3.addProperty("chunkPosZ", p_374920_.chunkPos().z);
                     jsonobject3.addProperty("structureName", p_374920_.structureName());
                     jsonobject3.addProperty("level", p_374920_.level());
                     jsonobject3.addProperty("success", p_374920_.success());
                     return jsonobject3;
                  };
                  jsonobject.add("fastest", function.apply(timedstatsummary.fastest()));
                  jsonobject.add("slowest", function.apply(timedstatsummary.slowest()));
                  jsonobject.add(
                     "secondSlowest",
                     (JsonElement)(timedstatsummary.secondSlowest() != null ? function.apply(timedstatsummary.secondSlowest()) : JsonNull.INSTANCE)
                  );
               }
            }
         );
      return jsonobject;
   }

   private JsonElement chunkGen(List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> p_185573_) {
      JsonObject jsonobject = new JsonObject();
      if (p_185573_.isEmpty()) {
         return jsonobject;
      }

      jsonobject.addProperty(
         "durationNanosTotal", p_185573_.stream().mapToDouble(p_185567_ -> ((TimedStatSummary)p_185567_.getSecond()).totalDuration().toNanos()).sum()
      );
      JsonArray jsonarray = Util.make(new JsonArray(), p_185558_ -> jsonobject.add("status", p_185558_));

      for (Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>> pair : p_185573_) {
         TimedStatSummary<ChunkGenStat> timedstatsummary = (TimedStatSummary<ChunkGenStat>)pair.getSecond();
         JsonObject jsonobject1 = Util.make(new JsonObject(), jsonarray::add);
         jsonobject1.addProperty("state", ((ChunkStatus)pair.getFirst()).toString());
         jsonobject1.addProperty("count", timedstatsummary.count());
         jsonobject1.addProperty("durationNanosTotal", timedstatsummary.totalDuration().toNanos());
         jsonobject1.addProperty("durationNanosAvg", timedstatsummary.totalDuration().toNanos() / timedstatsummary.count());
         JsonObject jsonobject2 = Util.make(new JsonObject(), p_185561_ -> jsonobject1.add("durationNanosPercentiles", p_185561_));
         timedstatsummary.percentilesNanos().forEach((p_185584_, p_185585_) -> jsonobject2.addProperty("p" + p_185584_, p_185585_));
         Function<ChunkGenStat, JsonElement> function = p_185538_ -> {
            JsonObject jsonobject3 = new JsonObject();
            jsonobject3.addProperty("durationNanos", p_185538_.duration().toNanos());
            jsonobject3.addProperty("level", p_185538_.level());
            jsonobject3.addProperty("chunkPosX", p_185538_.chunkPos().x);
            jsonobject3.addProperty("chunkPosZ", p_185538_.chunkPos().z);
            jsonobject3.addProperty("worldPosX", p_185538_.worldPos().x());
            jsonobject3.addProperty("worldPosZ", p_185538_.worldPos().z());
            return jsonobject3;
         };
         jsonobject1.add("fastest", function.apply(timedstatsummary.fastest()));
         jsonobject1.add("slowest", function.apply(timedstatsummary.slowest()));
         jsonobject1.add(
            "secondSlowest", (JsonElement)(timedstatsummary.secondSlowest() != null ? function.apply(timedstatsummary.secondSlowest()) : JsonNull.INSTANCE)
         );
      }

      return jsonobject;
   }

   private JsonElement threadAllocations(ThreadAllocationStat.Summary p_185546_) {
      JsonArray jsonarray = new JsonArray();
      p_185546_.allocationsPerSecondByThread().forEach((p_449361_, p_449362_) -> jsonarray.add(Util.make(new JsonObject(), p_185571_ -> {
         p_185571_.addProperty("thread", p_449361_);
         p_185571_.addProperty("bytesPerSecond", p_449362_);
      })));
      return jsonarray;
   }

   private JsonElement serverTicks(List<TickTimeStat> p_185587_) {
      if (p_185587_.isEmpty()) {
         return JsonNull.INSTANCE;
      }

      JsonObject jsonobject = new JsonObject();
      double[] adouble = p_185587_.stream().mapToDouble(p_185548_ -> p_185548_.currentAverage().toNanos() / 1000000.0).toArray();
      DoubleSummaryStatistics doublesummarystatistics = DoubleStream.of(adouble).summaryStatistics();
      jsonobject.addProperty("minMs", doublesummarystatistics.getMin());
      jsonobject.addProperty("averageMs", doublesummarystatistics.getAverage());
      jsonobject.addProperty("maxMs", doublesummarystatistics.getMax());
      Map<Integer, Double> map = Percentiles.evaluate(adouble);
      map.forEach((p_374918_, p_374919_) -> jsonobject.addProperty("p" + p_374918_, p_374919_));
      return jsonobject;
   }

   private JsonElement fps(List<FpsStat> p_453713_) {
      if (p_453713_.isEmpty()) {
         return JsonNull.INSTANCE;
      }

      JsonObject jsonobject = new JsonObject();
      int[] aint = p_453713_.stream().mapToInt(FpsStat::fps).toArray();
      IntSummaryStatistics intsummarystatistics = IntStream.of(aint).summaryStatistics();
      jsonobject.addProperty("minFPS", intsummarystatistics.getMin());
      jsonobject.addProperty("averageFPS", intsummarystatistics.getAverage());
      jsonobject.addProperty("maxFPS", intsummarystatistics.getMax());
      Map<Integer, Double> map = Percentiles.evaluate(aint);
      map.forEach((p_185564_, p_185565_) -> jsonobject.addProperty("p" + p_185564_, p_185565_));
      return jsonobject;
   }

   private JsonElement fileIO(JfrStatsResult p_185578_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("write", this.fileIoSummary(p_185578_.fileWrites()));
      jsonobject.add("read", this.fileIoSummary(p_185578_.fileReads()));
      jsonobject.add("chunksRead", this.ioSummary(p_185578_.readChunks(), JfrResultJsonSerializer::serializeChunkId));
      jsonobject.add("chunksWritten", this.ioSummary(p_185578_.writtenChunks(), JfrResultJsonSerializer::serializeChunkId));
      return jsonobject;
   }

   private JsonElement fileIoSummary(FileIOStat.Summary p_185540_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("totalBytes", p_185540_.totalBytes());
      jsonobject.addProperty("count", p_185540_.counts());
      jsonobject.addProperty("bytesPerSecond", p_185540_.bytesPerSecond());
      jsonobject.addProperty("countPerSecond", p_185540_.countsPerSecond());
      JsonArray jsonarray = new JsonArray();
      jsonobject.add("topContributors", jsonarray);
      p_185540_.topTenContributorsByTotalBytes().forEach(p_185581_ -> {
         JsonObject jsonobject1 = new JsonObject();
         jsonarray.add(jsonobject1);
         jsonobject1.addProperty("path", (String)p_185581_.getFirst());
         jsonobject1.addProperty("totalBytes", (Number)p_185581_.getSecond());
      });
      return jsonobject;
   }

   private JsonElement network(JfrStatsResult p_185589_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("sent", this.ioSummary(p_185589_.sentPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
      jsonobject.add("received", this.ioSummary(p_185589_.receivedPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
      return jsonobject;
   }

   private <T> JsonElement ioSummary(IoSummary<T> p_333759_, BiConsumer<T, JsonObject> p_328845_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("totalBytes", p_333759_.getTotalSize());
      jsonobject.addProperty("count", p_333759_.getTotalCount());
      jsonobject.addProperty("bytesPerSecond", p_333759_.getSizePerSecond());
      jsonobject.addProperty("countPerSecond", p_333759_.getCountsPerSecond());
      JsonArray jsonarray = new JsonArray();
      jsonobject.add("topContributors", jsonarray);
      p_333759_.largestSizeContributors().forEach(p_326733_ -> {
         JsonObject jsonobject1 = new JsonObject();
         jsonarray.add(jsonobject1);
         T t = (T)p_326733_.getFirst();
         IoSummary.CountAndSize iosummary$countandsize = (IoSummary.CountAndSize)p_326733_.getSecond();
         p_328845_.accept(t, jsonobject1);
         jsonobject1.addProperty("totalBytes", iosummary$countandsize.totalSize());
         jsonobject1.addProperty("count", iosummary$countandsize.totalCount());
         jsonobject1.addProperty("averageSize", iosummary$countandsize.averageSize());
      });
      return jsonobject;
   }

   private JsonElement cpu(List<CpuLoadStat> p_185591_) {
      JsonObject jsonobject = new JsonObject();
      BiFunction<List<CpuLoadStat>, ToDoubleFunction<CpuLoadStat>, JsonObject> bifunction = (p_185575_, p_185576_) -> {
         JsonObject jsonobject1 = new JsonObject();
         DoubleSummaryStatistics doublesummarystatistics = p_185575_.stream().mapToDouble(p_185576_).summaryStatistics();
         jsonobject1.addProperty("min", doublesummarystatistics.getMin());
         jsonobject1.addProperty("average", doublesummarystatistics.getAverage());
         jsonobject1.addProperty("max", doublesummarystatistics.getMax());
         return jsonobject1;
      };
      jsonobject.add("jvm", (JsonElement)bifunction.apply(p_185591_, CpuLoadStat::jvm));
      jsonobject.add("userJvm", (JsonElement)bifunction.apply(p_185591_, CpuLoadStat::userJvm));
      jsonobject.add("system", (JsonElement)bifunction.apply(p_185591_, CpuLoadStat::system));
      return jsonobject;
   }
}
