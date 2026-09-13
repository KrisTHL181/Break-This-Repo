package net.minecraft.stats;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class ServerStatsCounter extends StatsCounter {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<Map<Stat<?>, Integer>> STATS_CODEC = Codec.dispatchedMap(
         BuiltInRegistries.STAT_TYPE.byNameCodec(), Util.memoize(ServerStatsCounter::createTypedStatsCodec)
      )
      .xmap(p_390196_ -> {
         Map<Stat<?>, Integer> map = new HashMap<>();
         p_390196_.forEach((p_390199_, p_390200_) -> map.putAll((Map<? extends Stat<?>, ? extends Integer>)p_390200_));
         return map;
      }, p_449209_ -> p_449209_.entrySet().stream().collect(Collectors.groupingBy(p_390201_ -> ((Stat)p_390201_.getKey()).getType(), Util.toMap())));
   private final Path file;
   private final Set<Stat<?>> dirty = Sets.newHashSet();

   private static <T> Codec<Map<Stat<?>, Integer>> createTypedStatsCodec(StatType<T> p_395191_) {
      Codec<T> codec = p_395191_.getRegistry().byNameCodec();
      Codec<Stat<?>> codec1 = codec.flatComapMap(
         p_395191_::get,
         p_390205_ -> p_390205_.getType() == p_395191_
            ? DataResult.success(p_390205_.getValue())
            : DataResult.error(() -> "Expected type " + p_395191_ + ", but got " + p_390205_.getType())
      );
      return Codec.unboundedMap(codec1, Codec.INT);
   }

   public ServerStatsCounter(MinecraftServer p_12816_, Path p_451842_) {
      this.file = p_451842_;
      if (Files.isRegularFile(p_451842_)) {
         try (Reader reader = Files.newBufferedReader(p_451842_, StandardCharsets.UTF_8)) {
            JsonElement jsonelement = StrictJsonParser.parse(reader);
            this.parse(p_12816_.getFixerUpper(), jsonelement);
         } catch (IOException ioexception) {
            LOGGER.error("Couldn't read statistics file {}", p_451842_, ioexception);
         } catch (JsonParseException jsonparseexception) {
            LOGGER.error("Couldn't parse statistics file {}", p_451842_, jsonparseexception);
         }
      }
   }

   public void save() {
      try {
         FileUtil.createDirectoriesSafe(this.file.getParent());

         try (Writer writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(this.toJson(), GSON.newJsonWriter(writer));
         }
      } catch (JsonIOException | IOException ioexception) {
         LOGGER.error("Couldn't save stats to {}", this.file, ioexception);
      }
   }

   @Override
   public void setValue(Player p_12827_, Stat<?> p_12828_, int p_12829_) {
      super.setValue(p_12827_, p_12828_, p_12829_);
      this.dirty.add(p_12828_);
   }

   private Set<Stat<?>> getDirty() {
      Set<Stat<?>> set = Sets.newHashSet(this.dirty);
      this.dirty.clear();
      return set;
   }

   public void parse(DataFixer p_460093_, JsonElement p_453130_) {
      Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, p_453130_);
      dynamic = DataFixTypes.STATS.updateToCurrentVersion(p_460093_, dynamic, NbtUtils.getDataVersion(dynamic, 1343));
      this.stats
         .putAll(
            STATS_CODEC.parse(dynamic.get("stats").orElseEmptyMap())
               .resultOrPartial(p_449208_ -> LOGGER.error("Failed to parse statistics for {}: {}", this.file, p_449208_))
               .orElse(Map.of())
         );
   }

   protected JsonElement toJson() {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("stats", (JsonElement)STATS_CODEC.encodeStart(JsonOps.INSTANCE, this.stats).getOrThrow());
      jsonobject.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
      return jsonobject;
   }

   public void markAllDirty() {
      this.dirty.addAll(this.stats.keySet());
   }

   public void sendStats(ServerPlayer p_12820_) {
      Object2IntMap<Stat<?>> object2intmap = new Object2IntOpenHashMap();

      for (Stat<?> stat : this.getDirty()) {
         object2intmap.put(stat, this.getValue(stat));
      }

      p_12820_.connection.send(new ClientboundAwardStatsPacket(object2intmap));
   }
}
