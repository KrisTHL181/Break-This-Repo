package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
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

   long getDayTime();

   boolean isThundering();

   boolean isRaining();

   void setRaining(boolean var1);

   boolean isHardcore();

   Difficulty getDifficulty();

   boolean isDifficultyLocked();

   default void fillCrashReportCategory(CrashReportCategory p_164873_, LevelHeightAccessor p_164874_) {
      p_164873_.setDetail("Level spawn location", () -> CrashReportCategory.formatLocation(p_164874_, this.getRespawnData().pos()));
      p_164873_.setDetail("Level time", () -> String.format(Locale.ROOT, "%d game time, %d day time", this.getGameTime(), this.getDayTime()));
   }

   record RespawnData(GlobalPos globalPos, float yaw, float pitch) {
      public static final LevelData.RespawnData DEFAULT = new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0.0F, 0.0F);
      public static final MapCodec<LevelData.RespawnData> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_430507_ -> p_430507_.group(
               GlobalPos.MAP_CODEC.forGetter(LevelData.RespawnData::globalPos),
               Codec.floatRange(-180.0F, 180.0F).fieldOf("yaw").forGetter(LevelData.RespawnData::yaw),
               Codec.floatRange(-90.0F, 90.0F).fieldOf("pitch").forGetter(LevelData.RespawnData::pitch)
            )
            .apply(p_430507_, LevelData.RespawnData::new)
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

      public static LevelData.RespawnData of(ResourceKey<Level> p_423518_, BlockPos p_427072_, float p_429462_, float p_430832_) {
         return new LevelData.RespawnData(GlobalPos.of(p_423518_, p_427072_.immutable()), Mth.wrapDegrees(p_429462_), Mth.clamp(p_430832_, -90.0F, 90.0F));
      }

      public ResourceKey<Level> dimension() {
         return this.globalPos.dimension();
      }

      public BlockPos pos() {
         return this.globalPos.pos();
      }
   }
}
