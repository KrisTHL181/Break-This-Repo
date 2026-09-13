package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderState extends BlockEntityRenderState {
    public @Nullable EntityRenderState displayEntity;
    public float spin;
    public float scale;
}