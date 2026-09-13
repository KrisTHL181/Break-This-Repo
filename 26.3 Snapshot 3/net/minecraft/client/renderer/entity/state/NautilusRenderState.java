package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class NautilusRenderState extends LivingEntityRenderState {
   public ItemStack saddle = ItemStack.EMPTY;
   public ItemStack bodyArmorItem = ItemStack.EMPTY;
   public @Nullable ZombieNautilusVariant variant;
}
