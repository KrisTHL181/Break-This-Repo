package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DebugBeeInfo(Optional<BlockPos> hivePos, Optional<BlockPos> flowerPos, int travelTicks, List<BlockPos> blacklistedHives) {
   public static final StreamCodec<ByteBuf, DebugBeeInfo> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional),
      DebugBeeInfo::hivePos,
      BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional),
      DebugBeeInfo::flowerPos,
      ByteBufCodecs.VAR_INT,
      DebugBeeInfo::travelTicks,
      BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
      DebugBeeInfo::blacklistedHives,
      DebugBeeInfo::new
   );

   public boolean hasHive(BlockPos p_428512_) {
      return this.hivePos.isPresent() && p_428512_.equals(this.hivePos.get());
   }
}
