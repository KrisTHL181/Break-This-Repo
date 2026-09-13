package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemClusterRenderState extends EntityRenderState {
   public final ItemStackRenderState item = new ItemStackRenderState();
   public int count;
   public int seed;

   public void extractItemGroupRenderState(Entity p_377676_, ItemStack p_378378_, ItemModelResolver p_377148_) {
      p_377148_.updateForNonLiving(this.item, p_378378_, ItemDisplayContext.GROUND, p_377676_);
      this.count = getRenderedAmount(p_378378_.getCount());
      this.seed = getSeedForItemStack(p_378378_);
   }

   public static int getSeedForItemStack(ItemStack p_377994_) {
      return p_377994_.isEmpty() ? 187 : Item.getId(p_377994_.getItem()) + p_377994_.getDamageValue();
   }

   public static int getRenderedAmount(int p_376134_) {
      if (p_376134_ <= 1) {
         return 1;
      } else if (p_376134_ <= 16) {
         return 2;
      } else if (p_376134_ <= 32) {
         return 3;
      } else {
         return p_376134_ <= 48 ? 4 : 5;
      }
   }
}
