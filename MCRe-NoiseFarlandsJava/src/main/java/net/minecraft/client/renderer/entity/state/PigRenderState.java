package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PigRenderState extends LivingEntityRenderState {
    public ItemStack saddle = ItemStack.EMPTY;
    public @Nullable PigVariant variant;
}