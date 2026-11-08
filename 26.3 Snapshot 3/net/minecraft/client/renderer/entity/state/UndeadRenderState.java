package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class UndeadRenderState extends HumanoidRenderState {
   @Override
   public ItemStack getUseItemStackForArm(final HumanoidArm arm) {
      return this.getMainHandItemStack();
   }
}
