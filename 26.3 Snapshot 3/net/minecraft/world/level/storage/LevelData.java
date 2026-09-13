package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

public interface LevelData {
   LevelData.RespawnData getRespawnData();

   long getGameTime();

   boolean isHardcore();

   Difficulty getDifficulty();

   boolean isDifficultyLocked();

   default void fillCrashReportCategory(final CrashReportCategory category, final LevelHeightAccessor levelHeightAccessor) {
      category.setDetail("Level spawn location", () -> CrashReportCategory.formatLocation(levelHeightAccessor, this.getRespawnData().pos()));
   }

   record RespawnData(GlobalPos globalPos, float yaw, float pitch) {
      public static final LevelData.RespawnData DEFAULT = new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0.0F, 0.0F);
      public static final MapCodec<LevelData.RespawnData> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               GlobalPos.MAP_CODEC.forGetter(LevelData.RespawnData::globalPos),
               Codec.floatRange(-180.0F, 180.0F).fieldOf("yaw").forGetter(LevelData.RespawnData::yaw),
               Codec.floatRange(-90.0F, 90.0F).fieldOf("pitch").forGetter(LevelData.RespawnData::pitch)
            )
            .apply(i, LevelData.RespawnData::new)
      );
      public static final Codec<LevelData.RespawnData> CODEC = MAP_CODEC.codec();
      public static final StreamCodec<ByteBuf, LevelData.RespawnData> STREAM_CODEC = StreamCodec.composite(
         GlobalPos.STREAM_CODEC,
         LevelData.RespawnData::globalPos,
         ByteBufCodecs.FLOAT,
         LevelData.RespawnData::yaw,
         ByteBufCodecs.FLOAT,
         LevelData.RespawnData::pitch,
         LevelData.RespawnData::new
      );

      public static LevelData.RespawnData of(final ResourceKey<Level> dimension, final BlockPos pos, final float yaw, final float pitch) {
         return new LevelData.RespawnData(GlobalPos.of(dimension, pos.immutable()), Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F));
      }

      public ResourceKey<Level> dimension() {
         return this.globalPos.dimension();
      }

      public BlockPos pos() {
         return this.globalPos.pos();
      }
   }
}
