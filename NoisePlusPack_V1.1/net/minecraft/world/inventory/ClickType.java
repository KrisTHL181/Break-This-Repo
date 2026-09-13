package net.minecraft.world.inventory;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public enum ClickType {
   PICKUP(0),
   QUICK_MOVE(1),
   SWAP(2),
   CLONE(3),
   THROW(4),
   QUICK_CRAFT(5),
   PICKUP_ALL(6);

   private static final IntFunction<ClickType> BY_ID = ByIdMap.continuous(ClickType::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, ClickType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ClickType::id);
   private final int id;

   ClickType(final int p_394043_) {
      this.id = p_394043_;
   }

   public int id() {
      return this.id;
   }
}
