package net.minecraft.world.ticks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public record SavedTick<T>(T type, BlockPos pos, int delay, TickPriority priority) {
   public static final Strategy<SavedTick<?>> UNIQUE_TICK_HASH = new Strategy<SavedTick<?>>() {
      public int hashCode(SavedTick<?> p_193364_) {
         return 31 * p_193364_.pos().hashCode() + p_193364_.type().hashCode();
      }

      public boolean equals(@Nullable SavedTick<?> p_193366_, @Nullable SavedTick<?> p_193367_) {
         if (p_193366_ == p_193367_) {
            return true;
         } else {
            return p_193366_ != null && p_193367_ != null ? p_193366_.type() == p_193367_.type() && p_193366_.pos().equals(p_193367_.pos()) : false;
         }
      }
   };

   public static <T> Codec<SavedTick<T>> codec(Codec<T> p_397196_) {
      MapCodec<BlockPos> mapcodec = RecordCodecBuilder.mapCodec(
         p_391157_ -> p_391157_.group(
               Codec.INT.fieldOf("x").forGetter(Vec3i::getX), Codec.INT.fieldOf("y").forGetter(Vec3i::getY), Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
            )
            .apply(p_391157_, BlockPos::new)
      );
      return RecordCodecBuilder.create(
         p_391162_ -> p_391162_.group(
               p_397196_.fieldOf("i").forGetter(SavedTick::type),
               mapcodec.forGetter(SavedTick::pos),
               Codec.INT.fieldOf("t").forGetter(SavedTick::delay),
               TickPriority.CODEC.fieldOf("p").forGetter(SavedTick::priority)
            )
            .apply(p_391162_, SavedTick::new)
      );
   }

   public static <T> List<SavedTick<T>> filterTickListForChunk(List<SavedTick<T>> p_394267_, ChunkPos p_397045_) {
      long i = p_397045_.toLong();
      return p_394267_.stream().filter(p_391159_ -> ChunkPos.asLong(p_391159_.pos()) == i).toList();
   }

   public ScheduledTick<T> unpack(long p_193329_, long p_193330_) {
      return new ScheduledTick<>(this.type, this.pos, p_193329_ + this.delay, this.priority, p_193330_);
   }

   public static <T> SavedTick<T> probe(T p_193336_, BlockPos p_193337_) {
      return new SavedTick<>(p_193336_, p_193337_, 0, TickPriority.NORMAL);
   }
}
