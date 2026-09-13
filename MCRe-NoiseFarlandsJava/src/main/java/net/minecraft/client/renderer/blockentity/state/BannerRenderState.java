package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerRenderState extends BlockEntityRenderState {
    public DyeColor baseColor;
    public BannerPatternLayers patterns = BannerPatternLayers.EMPTY;
    public float phase;
    public Transformation transformation = Transformation.IDENTITY;
    public BannerBlock.AttachmentType attachmentType = BannerBlock.AttachmentType.GROUND;
}