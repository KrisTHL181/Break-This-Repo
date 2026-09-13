package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.phys.Vec3;

public sealed interface PositionPath permits PositionPath.Linear, PositionPath.Stepped {
   StreamCodec<ByteBuf, PositionPath> STREAM_CODEC = PositionPath.Type.STREAM_CODEC.dispatch(PositionPath::type, PositionPath.Type::streamCodec);

   Vec3 endPosition();

   PositionPath.Type type();

   static PositionPath of(final Vec3 position) {
      return new PositionPath.Linear(position);
   }

   static PositionPath stepped(final List<PositionStep> steps) {
      return new PositionPath.Stepped(steps);
   }

   record Linear(Vec3 endPosition) implements PositionPath {
      public static final StreamCodec<ByteBuf, PositionPath.Linear> STREAM_CODEC = Vec3.STREAM_CODEC
         .map(PositionPath.Linear::new, PositionPath.Linear::endPosition);

      @Override
      public PositionPath.Type type() {
         return PositionPath.Type.LINEAR;
      }
   }

   record Stepped(Vec3 endPosition, List<PositionStep> steps) implements PositionPath {
      public static final StreamCodec<ByteBuf, PositionPath.Stepped> STREAM_CODEC = PositionStep.STREAM_CODEC
         .apply(ByteBufCodecs.list())
         .map(PositionPath.Stepped::new, PositionPath.Stepped::steps);

      public Stepped(final List<PositionStep> steps) {
         this(steps.getLast().position(), steps);
      }

      @Override
      public PositionPath.Type type() {
         return PositionPath.Type.STEPPED;
      }
   }

   enum Type {
      LINEAR(PositionPath.Linear.STREAM_CODEC),
      STEPPED(PositionPath.Stepped.STREAM_CODEC);

      public static final IntFunction<PositionPath.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
      public static final StreamCodec<ByteBuf, PositionPath.Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
      private final StreamCodec<ByteBuf, ? extends PositionPath> streamCodec;

      Type(final StreamCodec<ByteBuf, ? extends PositionPath> streamCodec) {
         this.streamCodec = streamCodec;
      }

      public StreamCodec<ByteBuf, ? extends PositionPath> streamCodec() {
         return this.streamCodec;
      }
   }
}
