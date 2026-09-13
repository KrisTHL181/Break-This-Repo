package net.minecraft.world.level.gameevent;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record BlockPositionSource(BlockPos pos) implements PositionSource {
   public static final MapCodec<BlockPositionSource> CODEC = RecordCodecBuilder.mapCodec(
      p_157710_ -> p_157710_.group(BlockPos.CODEC.fieldOf("pos").forGetter(BlockPositionSource::pos)).apply(p_157710_, BlockPositionSource::new)
   );
   public static final StreamCodec<ByteBuf, BlockPositionSource> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, BlockPositionSource::pos, BlockPositionSource::new
   );

   @Override
   public Optional<Vec3> getPosition(Level p_157708_) {
      return Optional.of(Vec3.atCenterOf(this.pos));
   }

   @Override
   public PositionSourceType<BlockPositionSource> getType() {
      return PositionSourceType.BLOCK;
   }

   public static class Type implements PositionSourceType<BlockPositionSource> {
      @Override
      public MapCodec<BlockPositionSource> codec() {
         return BlockPositionSource.CODEC;
      }

      @Override
      public StreamCodec<ByteBuf, BlockPositionSource> streamCodec() {
         return BlockPositionSource.STREAM_CODEC;
      }
   }
}
