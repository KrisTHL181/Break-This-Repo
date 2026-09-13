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
    private final Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

    private static void serializePacketId(final PacketIdentification identifier, final JsonObject output) {
        output.addProperty("protocolId", identifier.protocolId());
        output.addProperty("packetId", identifier.packetId());
    }

    private static void serializeChunkId(final ChunkIdentification identifier, final JsonObject output) {
        output.addProperty("level", identifier.level());
        output.addProperty("dimension", identifier.dimension());
        output.addProperty("x", identifier.x());
        output.addProperty("z", identifier.z());
    }

    public String format(final JfrStatsResult jfrStats) {
        JsonObject root = new JsonObject();
        root.addProperty("startedEpoch", jfrStats.recordingStarted().toEpochMilli());
        root.addProperty("endedEpoch", jfrStats.recordingEnded().toEpochMilli());
        root.addProperty("durationMs", jfrStats.recordingDuration().toMillis());
        Duration worldCreationDuration = jfrStats.worldCreationDuration();
        if (worldCreationDuration != null) {
            root.addProperty("worldGenDurationMs", worldCreationDuration.toMillis());
        }

        root.add("heap", this.heap(jfrStats.heapSummary()));
        root.add("cpuPercent", this.cpu(jfrStats.cpuLoadStats()));
        root.add("network", this.network(jfrStats));
        root.add("fileIO", this.fileIO(jfrStats));
        root.add("fps", this.fps(jfrStats.fps()));
        root.add("serverTick", this.serverTicks(jfrStats.serverTickTimes()));
        root.add("threadAllocation", this.threadAllocations(jfrStats.threadAllocationSummary()));
        root.add("chunkGen", this.chunkGen(jfrStats.chunkGenSummary()));
        root.add("structureGen", this.structureGen(jfrStats.structureGenStats()));
        return this.gson.toJson(root);
    }

    private JsonElement heap(final GcHeapStat.Summary heapSummary) {
        JsonObject json = new JsonObject();
        json.addProperty("allocationRateBytesPerSecond", heapSummary.allocationRateBytesPerSecond());
        json.addProperty("gcCount", heapSummary.totalGCs());
        json.addProperty("gcOverHeadPercent", heapSummary.gcOverHead());
        json.addProperty("gcTotalDurationMs", heapSummary.gcTotalDuration().toMillis());
        return json;
    }

    private JsonElement structureGen(final List<StructureGenStat> structureGenStats) {
        JsonObject root = new JsonObject();
        Optional<TimedStatSummary<StructureGenStat>> optionalSummary = TimedStatSummary.summary(structureGenStats);
        if (optionalSummary.isEmpty()) {
            return root;
        }

        TimedStatSummary<StructureGenStat> summary = optionalSummary.get();
        JsonArray structureJsonArray = new JsonArray();
        root.add("structure", structureJsonArray);
        structureGenStats.stream().collect(Collectors.groupingBy(StructureGenStat::structureName)).forEach((structureName, timedStat) -> {
            Optional<TimedStatSummary<StructureGenStat>> optionalStatSummary = TimedStatSummary.summary((List<StructureGenStat>)timedStat);
            if (!optionalStatSummary.isEmpty()) {
                TimedStatSummary<StructureGenStat> statSummary = optionalStatSummary.get();
                JsonObject structureJson = new JsonObject();
                structureJsonArray.add(structureJson);
                structureJson.addProperty("name", structureName);
                structureJson.addProperty("count", statSummary.count());
                structureJson.addProperty("durationNanosTotal", statSummary.totalDuration().toNanos());
                structureJson.addProperty("durationNanosAvg", statSummary.totalDuration().toNanos() / statSummary.count());
                JsonObject percentiles = Util.make(new JsonObject(), self -> structureJson.add("durationNanosPercentiles", self));
                statSummary.percentilesNanos().forEach((percentile, v) -> percentiles.addProperty("p" + percentile, v));
                Function<StructureGenStat, JsonElement> structureGenStatJsonGenerator = structureGen -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("durationNanos", structureGen.duration().toNanos());
                    result.addProperty("chunkPosX", structureGen.chunkPos().x());
                    result.addProperty("chunkPosZ", structureGen.chunkPos().z());
                    result.addProperty("structureName", structureGen.structureName());
                    result.addProperty("level", structureGen.level());
                    result.addProperty("success", structureGen.success());
                    return result;
                };
                root.add("fastest", structureGenStatJsonGenerator.apply(summary.fastest()));
                root.add("slowest", structureGenStatJsonGenerator.apply(summary.slowest()));
                root.add("secondSlowest", summary.secondSlowest() != null ? structureGenStatJsonGenerator.apply(summary.secondSlowest()) : JsonNull.INSTANCE);
            }
        });
        return root;
    }

    private JsonElement chunkGen(final List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> chunkGenSummary) {
        JsonObject json = new JsonObject();
        if (chunkGenSummary.isEmpty()) {
            return json;
        }

        json.addProperty("durationNanosTotal", chunkGenSummary.stream().mapToDouble(it -> it.getSecond().totalDuration().toNanos()).sum());
        JsonArray chunkJsonArray = Util.make(new JsonArray(), self -> json.add("status", self));

        for (Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>> summaryByStatus : chunkGenSummary) {
            TimedStatSummary<ChunkGenStat> chunkStat = summaryByStatus.getSecond();
            JsonObject chunkStatusJson = Util.make(new JsonObject(), chunkJsonArray::add);
            chunkStatusJson.addProperty("state", summaryByStatus.getFirst().toString());
            chunkStatusJson.addProperty("count", chunkStat.count());
            chunkStatusJson.addProperty("durationNanosTotal", chunkStat.totalDuration().toNanos());
            chunkStatusJson.addProperty("durationNanosAvg", chunkStat.totalDuration().toNanos() / chunkStat.count());
            JsonObject percentiles = Util.make(new JsonObject(), self -> chunkStatusJson.add("durationNanosPercentiles", self));
            chunkStat.percentilesNanos().forEach((percentile, value) -> percentiles.addProperty("p" + percentile, value));
            Function<ChunkGenStat, JsonElement> chunkGenStatJsonGenerator = chunk -> {
                JsonObject chunkGenStatJson = new JsonObject();
                chunkGenStatJson.addProperty("durationNanos", chunk.duration().toNanos());
                chunkGenStatJson.addProperty("level", chunk.level());
                chunkGenStatJson.addProperty("chunkPosX", chunk.chunkPos().x());
                chunkGenStatJson.addProperty("chunkPosZ", chunk.chunkPos().z());
                chunkGenStatJson.addProperty("worldPosX", chunk.worldPos().x());
                chunkGenStatJson.addProperty("worldPosZ", chunk.worldPos().z());
                return chunkGenStatJson;
            };
            chunkStatusJson.add("fastest", chunkGenStatJsonGenerator.apply(chunkStat.fastest()));
            chunkStatusJson.add("slowest", chunkGenStatJsonGenerator.apply(chunkStat.slowest()));
            chunkStatusJson.add(
                "secondSlowest", chunkStat.secondSlowest() != null ? chunkGenStatJsonGenerator.apply(chunkStat.secondSlowest()) : JsonNull.INSTANCE
            );
        }

        return json;
    }

    private JsonElement threadAllocations(final ThreadAllocationStat.Summary threadAllocationSummary) {
        JsonArray threads = new JsonArray();
        threadAllocationSummary.allocationsPerSecondByThread().forEach((threadName, bytesPerSecond) -> threads.add(Util.make(new JsonObject(), json -> {
            json.addProperty("thread", threadName);
            json.addProperty("bytesPerSecond", bytesPerSecond);
        })));
        return threads;
    }

    private JsonElement serverTicks(final List<TickTimeStat> tickTimeStats) {
        if (tickTimeStats.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();
        double[] tickTimesMs = tickTimeStats.stream().mapToDouble(tickTimeStat -> tickTimeStat.currentAverage().toNanos() / 1000000.0).toArray();
        DoubleSummaryStatistics summary = DoubleStream.of(tickTimesMs).summaryStatistics();
        json.addProperty("minMs", summary.getMin());
        json.addProperty("averageMs", summary.getAverage());
        json.addProperty("maxMs", summary.getMax());
        Map<Integer, Double> percentiles = Percentiles.evaluate(tickTimesMs);
        percentiles.forEach((percentile, value) -> json.addProperty("p" + percentile, value));
        return json;
    }

    private JsonElement fps(final List<FpsStat> fpsStats) {
        if (fpsStats.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();
        int[] fps = fpsStats.stream().mapToInt(FpsStat::fps).toArray();
        IntSummaryStatistics summary = IntStream.of(fps).summaryStatistics();
        json.addProperty("minFPS", summary.getMin());
        json.addProperty("averageFPS", summary.getAverage());
        json.addProperty("maxFPS", summary.getMax());
        Map<Integer, Double> percentiles = Percentiles.evaluate(fps);
        percentiles.forEach((percentile, value) -> json.addProperty("p" + percentile, value));
        return json;
    }

    private JsonElement fileIO(final JfrStatsResult jfrStats) {
        JsonObject json = new JsonObject();
        json.add("write", this.fileIoSummary(jfrStats.fileWrites()));
        json.add("read", this.fileIoSummary(jfrStats.fileReads()));
        json.add("chunksRead", this.ioSummary(jfrStats.readChunks(), JfrResultJsonSerializer::serializeChunkId));
        json.add("chunksWritten", this.ioSummary(jfrStats.writtenChunks(), JfrResultJsonSerializer::serializeChunkId));
        return json;
    }

    private JsonElement fileIoSummary(final FileIOStat.Summary io) {
        JsonObject json = new JsonObject();
        json.addProperty("totalBytes", io.totalBytes());
        json.addProperty("count", io.counts());
        json.addProperty("bytesPerSecond", io.bytesPerSecond());
        json.addProperty("countPerSecond", io.countsPerSecond());
        JsonArray topContributors = new JsonArray();
        json.add("topContributors", topContributors);
        io.topTenContributorsByTotalBytes().forEach(contributor -> {
            JsonObject contributorJson = new JsonObject();
            topContributors.add(contributorJson);
            contributorJson.addProperty("path", contributor.getFirst());
            contributorJson.addProperty("totalBytes", contributor.getSecond());
        });
        return json;
    }

    private JsonElement network(final JfrStatsResult jfrStats) {
        JsonObject json = new JsonObject();
        json.add("sent", this.ioSummary(jfrStats.sentPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
        json.add("received", this.ioSummary(jfrStats.receivedPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
        return json;
    }

    private <T> JsonElement ioSummary(final IoSummary<T> summary, final BiConsumer<T, JsonObject> elementWriter) {
        JsonObject json = new JsonObject();
        json.addProperty("totalBytes", summary.getTotalSize());
        json.addProperty("count", summary.getTotalCount());
        json.addProperty("bytesPerSecond", summary.getSizePerSecond());
        json.addProperty("countPerSecond", summary.getCountsPerSecond());
        JsonArray topContributors = new JsonArray();
        json.add("topContributors", topContributors);
        summary.largestSizeContributors().forEach(contributor -> {
            JsonObject contributorJson = new JsonObject();
            topContributors.add(contributorJson);
            T identifier = contributor.getFirst();
            IoSummary.CountAndSize countAndSize = contributor.getSecond();
            elementWriter.accept(identifier, contributorJson);
            contributorJson.addProperty("totalBytes", countAndSize.totalSize());
            contributorJson.addProperty("count", countAndSize.totalCount());
            contributorJson.addProperty("averageSize", countAndSize.averageSize());
        });
        return json;
    }

    private JsonElement cpu(final List<CpuLoadStat> cpuStats) {
        JsonObject json = new JsonObject();
        BiFunction<List<CpuLoadStat>, ToDoubleFunction<CpuLoadStat>, JsonObject> transformer = (cpuLoadStats, extractor) -> {
            JsonObject jsonGroup = new JsonObject();
            DoubleSummaryStatistics stats = cpuLoadStats.stream().mapToDouble(extractor).summaryStatistics();
            jsonGroup.addProperty("min", stats.getMin());
            jsonGroup.addProperty("average", stats.getAverage());
            jsonGroup.addProperty("max", stats.getMax());
            return jsonGroup;
        };
        json.add("jvm", transformer.apply(cpuStats, CpuLoadStat::jvm));
        json.add("userJvm", transformer.apply(cpuStats, CpuLoadStat::userJvm));
        json.add("system", transformer.apply(cpuStats, CpuLoadStat::system));
        return json;
    }
}