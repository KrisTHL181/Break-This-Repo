package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithBoundingBoxRenderState extends BlockEntityRenderState {
    public boolean isVisible;
    public BoundingBoxRenderable.Mode mode;
    public BoundingBoxRenderable.RenderableBox box;
    public BlockEntityWithBoundingBoxRenderState.@Nullable InvisibleBlockType @Nullable [] invisibleBlocks;
    public boolean @Nullable [] structureVoids;

    @OnlyIn(Dist.CLIENT)
    public enum InvisibleBlockType {
        AIR,
        BARRIER,
        LIGHT,
        STRUCTURE_VOID;
    }
}