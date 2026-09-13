package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.jspecify.annotations.Nullable;

public class VaultRenderState extends BlockEntityRenderState {
   public @Nullable ItemClusterRenderState displayItem;
   public float spin;
}
