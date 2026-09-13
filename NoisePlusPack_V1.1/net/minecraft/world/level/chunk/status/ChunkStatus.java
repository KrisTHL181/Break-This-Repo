package net.minecraft.world.level.chunk.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class ChunkStatus {
   public static final int MAX_STRUCTURE_DISTANCE = 8;
   private static final EnumSet<Heightmap.Types> WORLDGEN_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
   public static final EnumSet<Heightmap.Types> FINAL_HEIGHTMAPS = EnumSet.of(
      Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
   );
   public static final ChunkStatus EMPTY = register("empty", null, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus STRUCTURE_STARTS = register("structure_starts", EMPTY, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus STRUCTURE_REFERENCES = register("structure_references", STRUCTURE_STARTS, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus BIOMES = register("biomes", STRUCTURE_REFERENCES, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus NOISE = register("noise", BIOMES, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus SURFACE = register("surface", NOISE, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus CARVERS = register("carvers", SURFACE, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus FEATURES = register("features", CARVERS, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus INITIALIZE_LIGHT = register("initialize_light", FEATURES, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus LIGHT = register("light", INITIALIZE_LIGHT, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus SPAWN = register("spawn", LIGHT, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
   public static final ChunkStatus FULL = register("full", SPAWN, FINAL_HEIGHTMAPS, ChunkType.LEVELCHUNK);
   public static final Codec<ChunkStatus> CODEC = BuiltInRegistries.CHUNK_STATUS.byNameCodec();
   private final int index;
   private final ChunkStatus parent;
   private final ChunkType chunkType;
   private final EnumSet<Heightmap.Types> heightmapsAfter;

   private static ChunkStatus register(String p_334704_, @Nullable ChunkStatus p_335238_, EnumSet<Heightmap.Types> p_335194_, ChunkType p_333808_) {
      return Registry.register(BuiltInRegistries.CHUNK_STATUS, p_334704_, new ChunkStatus(p_335238_, p_335194_, p_333808_));
   }

   public static List<ChunkStatus> getStatusList() {
      List<ChunkStatus> list = Lists.newArrayList();

      ChunkStatus chunkstatus;
      for (chunkstatus = FULL; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
         list.add(chunkstatus);
      }

      list.add(chunkstatus);
      Collections.reverse(list);
      return list;
   }

   @VisibleForTesting
   protected ChunkStatus(@Nullable ChunkStatus p_334696_, EnumSet<Heightmap.Types> p_329876_, ChunkType p_336141_) {
      this.parent = p_334696_ == null ? this : p_334696_;
      this.chunkType = p_336141_;
      this.heightmapsAfter = p_329876_;
      this.index = p_334696_ == null ? 0 : p_334696_.getIndex() + 1;
   }

   public int getIndex() {
      return this.index;
   }

   public ChunkStatus getParent() {
      return this.parent;
   }

   public ChunkType getChunkType() {
      return this.chunkType;
   }

   public static ChunkStatus byName(String p_329723_) {
      return BuiltInRegistries.CHUNK_STATUS.getValue(Identifier.tryParse(p_329723_));
   }

   public EnumSet<Heightmap.Types> heightmapsAfter() {
      return this.heightmapsAfter;
   }

   public boolean isOrAfter(ChunkStatus p_334516_) {
      return this.getIndex() >= p_334516_.getIndex();
   }

   public boolean isAfter(ChunkStatus p_342258_) {
      return this.getIndex() > p_342258_.getIndex();
   }

   public boolean isOrBefore(ChunkStatus p_343765_) {
      return this.getIndex() <= p_343765_.getIndex();
   }

   public boolean isBefore(ChunkStatus p_344604_) {
      return this.getIndex() < p_344604_.getIndex();
   }

   public static ChunkStatus max(ChunkStatus p_345032_, ChunkStatus p_342131_) {
      return p_345032_.isAfter(p_342131_) ? p_345032_ : p_342131_;
   }

   @Override
   public String toString() {
      return this.getName();
   }

   public String getName() {
      return BuiltInRegistries.CHUNK_STATUS.getKey(this).toString();
   }
}
