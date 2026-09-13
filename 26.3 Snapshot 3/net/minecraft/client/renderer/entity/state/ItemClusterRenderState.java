package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemClusterRenderState extends EntityRenderState {
   public final ItemStackRenderState item = new ItemStackRenderState();
   public int count;
   public int seed;

   public void extractItemGroupRenderState(final Entity entity, final ItemStack stack, final ItemModelResolver itemModelResolver) {
      itemModelResolver.updateForNonLiving(this.item, stack, ItemDisplayContext.GROUND, entity);
      this.count = getRenderedAmount(stack.getCount());
      this.seed = getSeedForItemStack(stack);
   }

   public static int getSeedForItemStack(final ItemStack itemStack) {
      return itemStack.isEmpty() ? 187 : Item.getId(itemStack.getItem()) + itemStack.getDamageValue();
   }

   public static int getRenderedAmount(final int stackCount) {
      if (stackCount <= 1) {
         return 1;
      } else if (stackCount <= 16) {
         return 2;
      } else if (stackCount <= 32) {
         return 3;
      } else {
         return stackCount <= 48 ? 4 : 5;
      }
   }
}
