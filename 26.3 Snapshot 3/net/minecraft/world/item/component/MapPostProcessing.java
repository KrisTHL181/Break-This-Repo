package net.minecraft.world.item.component;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public enum MapPostProcessing {
   LOCK(0),
   SCALE(1);

   public static final IntFunction<MapPostProcessing> ID_MAP = ByIdMap.continuous(MapPostProcessing::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, MapPostProcessing> STREAM_CODEC = ByteBufCodecs.idMapper(ID_MAP, MapPostProcessing::id);
   private final int id;

   MapPostProcessing(final int id) {
      this.id = id;
   }

   public int id() {
      return this.id;
   }
}
