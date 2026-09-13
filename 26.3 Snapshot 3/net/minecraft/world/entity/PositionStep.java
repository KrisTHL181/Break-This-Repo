package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record PositionStep(Vec3 position, int tickOffset) {
   public static final StreamCodec<ByteBuf, PositionStep> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC, PositionStep::position, ByteBufCodecs.VAR_INT, PositionStep::tickOffset, PositionStep::new
   );

   public PositionStep addDelta(final Vec3 delta) {
      return new PositionStep(this.position.add(delta), this.tickOffset);
   }
}
