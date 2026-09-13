package net.minecraft.core.cauldron;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface CauldronInteraction {
   CauldronInteraction DEFAULT = (var0, var1, var2, var3, var4, var5) -> InteractionResult.TRY_WITH_EMPTY_HAND;

   InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand);

   class Dispatcher {
      private final Map<TagKey<Item>, CauldronInteraction> tags = new HashMap<>();
      private final Map<Item, CauldronInteraction> items = new HashMap<>();

      void put(final Item item, final CauldronInteraction interaction) {
         this.items.put(item, interaction);
      }

      void put(final TagKey<Item> tag, final CauldronInteraction interaction) {
         this.tags.put(tag, interaction);
      }

      public CauldronInteraction get(final ItemStack itemStack) {
         for (Entry<TagKey<Item>, CauldronInteraction> e : this.tags.entrySet()) {
            if (itemStack.is(e.getKey())) {
               return e.getValue();
            }
         }

         return this.items.getOrDefault(itemStack.getItem(), CauldronInteraction.DEFAULT);
      }
   }
}
