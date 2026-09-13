package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VaultRenderState extends BlockEntityRenderState {
    public @Nullable ItemClusterRenderState displayItem;
    public float spin;
}