package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;

public class GroupSlotSource extends CompositeSlotSource {
   public static final MapCodec<GroupSlotSource> MAP_CODEC = createCodec(GroupSlotSource::new);
   public static final Codec<GroupSlotSource> INLINE_CODEC = createInlineCodec(GroupSlotSource::new);

   private GroupSlotSource(final List<SlotSource> terms) {
      super(terms);
   }

   @Override
   public MapCodec<GroupSlotSource> codec() {
      return MAP_CODEC;
   }
}
