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
      .xmap(groupedStats -> {
         Map<Stat<?>, Integer> stats = new HashMap<>();
         groupedStats.forEach((type, values) -> stats.putAll((Map<? extends Stat<?>, ? extends Integer>)values));
         return stats;
      }, map -> map.entrySet().stream().collect(Collectors.groupingBy(entry -> ((Stat)entry.getKey()).getType(), Util.toMap())));
   private final Path file;
   private final Set<Stat<?>> dirty = Sets.newHashSet();

   private static <T> Codec<Map<Stat<?>, Integer>> createTypedStatsCodec(final StatType<T> type) {
      Codec<T> valueCodec = type.getRegistry().byNameCodec();
      Codec<Stat<?>> statCodec = valueCodec.flatComapMap(
         type::get,
         stat -> stat.getType() == type ? DataResult.success(stat.getValue()) : DataResult.error(() -> "Expected type " + type + ", but got " + stat.getType())
      );
      return Codec.unboundedMap(statCodec, Codec.INT);
   }

   public ServerStatsCounter(final MinecraftServer server, final Path file) {
      this.file = file;
      if (Files.isRegularFile(file)) {
         try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = StrictJsonParser.parse(reader);
            this.parse(server.getFixerUpper(), element);
         } catch (IOException e) {
            LOGGER.error("Couldn't read statistics file {}", file, e);
         } catch (JsonParseException e) {
            LOGGER.error("Couldn't parse statistics file {}", file, e);
         }
      }
   }

   public void save() {
      try {
         FileUtil.createDirectoriesSafe(this.file.getParent());

         try (Writer writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
            GSON.toJson(this.toJson(), GSON.newJsonWriter(writer));
         }
      } catch (IOException | JsonIOException e) {
         LOGGER.error("Couldn't save stats to {}", this.file, e);
      }
   }

   @Override
   public void setValue(final Player player, final Stat<?> stat, final int count) {
      super.setValue(player, stat, count);
      this.dirty.add(stat);
   }

   private Set<Stat<?>> getDirty() {
      Set<Stat<?>> result = Sets.newHashSet(this.dirty);
      this.dirty.clear();
      return result;
   }

   public void parse(final DataFixer fixerUpper, final JsonElement element) {
      Dynamic<JsonElement> data = new Dynamic(JsonOps.INSTANCE, element);
      data = DataFixTypes.STATS.updateToCurrentVersion(fixerUpper, data, NbtUtils.getDataVersion(data, 1343));
      this.stats
         .putAll(
            STATS_CODEC.parse(data.get("stats").orElseEmptyMap())
               .resultOrPartial(error -> LOGGER.error("Failed to parse statistics for {}: {}", this.file, error))
               .orElse(Map.of())
         );
   }

   protected JsonElement toJson() {
      JsonObject result = new JsonObject();
      result.add("stats", (JsonElement)STATS_CODEC.encodeStart(JsonOps.INSTANCE, this.stats).getOrThrow());
      result.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
      return result;
   }

   public void markAllDirty() {
      this.dirty.addAll(this.stats.keySet());
   }

   public void sendStats(final ServerPlayer player) {
      Object2IntMap<Stat<?>> statsToSend = new Object2IntOpenHashMap();

      for (Stat<?> stat : this.getDirty()) {
         statsToSend.put(stat, this.getValue(stat));
      }

      player.connection.send(new ClientboundAwardStatsPacket(statsToSend));
   }
}
