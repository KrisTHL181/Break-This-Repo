package net.minecraft.world;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.EquipmentSlot;

public enum InteractionHand {
   MAIN_HAND(0),
   OFF_HAND(1);

   private static final IntFunction<InteractionHand> BY_ID = ByIdMap.continuous(h -> h.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, InteractionHand> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, h -> h.id);
   private final int id;

   InteractionHand(final int id) {
      this.id = id;
   }

   public EquipmentSlot asEquipmentSlot() {
      return this == MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
   }
}
