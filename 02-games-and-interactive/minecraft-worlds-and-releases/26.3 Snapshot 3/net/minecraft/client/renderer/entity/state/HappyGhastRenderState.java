package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.item.ItemStack;

public class HappyGhastRenderState extends LivingEntityRenderState {
   public ItemStack bodyItem = ItemStack.EMPTY;
   public boolean isRidden;
   public boolean isLeashHolder;
}
