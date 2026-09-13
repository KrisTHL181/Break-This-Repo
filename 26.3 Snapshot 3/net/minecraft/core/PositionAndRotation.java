package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public interface PositionAndRotation {
   StreamCodec<ByteBuf, PositionAndRotation> STREAM_CODEC = StreamCodec.composite(
      Vec3.STREAM_CODEC,
      PositionAndRotation::position,
      ByteBufCodecs.FLOAT,
      PositionAndRotation::yRot,
      ByteBufCodecs.FLOAT,
      PositionAndRotation::xRot,
      PositionAndRotation::of
   );

   Vec3 position();

   float yRot();

   float xRot();

   PositionAndRotation immutable();

   default boolean is(final Vec3 position, final float yRot, final float xRot) {
      return this.yRot() == yRot && this.xRot() == xRot && Objects.equals(this.position(), position);
   }

   static PositionAndRotation of(final Vec3 position, final float yRot, final float xRot) {
      return new PositionAndRotation.Immutable(position, yRot, xRot);
   }

   record Immutable(Vec3 position, float yRot, float xRot) implements PositionAndRotation {
      @Override
      public PositionAndRotation immutable() {
         return this;
      }
   }

   class Mutable implements PositionAndRotation {
      private Vec3 position = Vec3.ZERO;
      private float yRot;
      private float xRot;

      @Override
      public Vec3 position() {
         return this.position;
      }

      @Override
      public float yRot() {
         return this.yRot;
      }

      @Override
      public float xRot() {
         return this.xRot;
      }

      @Override
      public PositionAndRotation immutable() {
         return PositionAndRotation.of(this.position, this.yRot, this.xRot);
      }

      public void set(final Vec3 position, final float yRot, final float xRot) {
         this.position = position;
         this.yRot = yRot;
         this.xRot = xRot;
      }

      public void addDelta(final Vec3 delta) {
         this.position = this.position.add(delta);
      }

      public void addRotation(final float yRot, final float xRot) {
         this.yRot += yRot;
         this.xRot += xRot;
      }
   }
}
