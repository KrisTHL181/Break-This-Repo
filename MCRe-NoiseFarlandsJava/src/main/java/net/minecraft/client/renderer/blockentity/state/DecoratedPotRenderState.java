package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderState extends BlockEntityRenderState {
    public float yRot;
    public DecoratedPotBlockEntity.@Nullable WobbleStyle wobbleStyle;
    public float wobbleProgress;
    public PotDecorations decorations = PotDecorations.EMPTY;
    public Direction direction = Direction.NORTH;
}