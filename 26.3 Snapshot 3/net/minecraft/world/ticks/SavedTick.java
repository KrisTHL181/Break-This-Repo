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
      public int hashCode(final SavedTick<?> o) {
         return 31 * o.pos().hashCode() + o.type().hashCode();
      }

      public boolean equals(final @Nullable SavedTick<?> a, final @Nullable SavedTick<?> b) {
         if (a == b) {
            return true;
         } else {
            return a != null && b != null ? a.type() == b.type() && a.pos().equals(b.pos()) : false;
         }
      }
   };

   public static <T> Codec<SavedTick<T>> codec(final Codec<T> typeCodec) {
      MapCodec<BlockPos> posCodec = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.INT.fieldOf("x").forGetter(Vec3i::getX), Codec.INT.fieldOf("y").forGetter(Vec3i::getY), Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
            )
            .apply(i, BlockPos::new)
      );
      return RecordCodecBuilder.create(
         i -> i.group(
               typeCodec.fieldOf("i").forGetter(SavedTick::type),
               posCodec.forGetter(SavedTick::pos),
               Codec.INT.fieldOf("t").forGetter(SavedTick::delay),
               TickPriority.CODEC.fieldOf("p").forGetter(SavedTick::priority)
            )
            .apply(i, SavedTick::new)
      );
   }

   public static <T> List<SavedTick<T>> filterTickListForChunk(final List<SavedTick<T>> savedTicks, final ChunkPos chunkPos) {
      long posKey = chunkPos.pack();
      return savedTicks.stream().filter(tick -> ChunkPos.pack(tick.pos()) == posKey).toList();
   }

   public ScheduledTick<T> unpack(final long currentTick, final long currentSubTick) {
      return new ScheduledTick<>(this.type, this.pos, currentTick + this.delay, this.priority, currentSubTick);
   }

   public static <T> SavedTick<T> probe(final T type, final BlockPos pos) {
      return new SavedTick<>(type, pos, 0, TickPriority.NORMAL);
   }
}
