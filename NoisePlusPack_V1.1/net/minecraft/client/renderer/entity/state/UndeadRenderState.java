package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UndeadRenderState extends HumanoidRenderState {
   @Override
   public ItemStack getUseItemStackForArm(HumanoidArm p_453711_) {
      return this.getMainHandItemStack();
   }
}
