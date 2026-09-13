package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaRenderState extends LivingEntityRenderState {
    public Llama.Variant variant = Llama.Variant.DEFAULT;
    public boolean hasChest;
    public ItemStack bodyItem = ItemStack.EMPTY;
    public boolean isTraderLlama;
}