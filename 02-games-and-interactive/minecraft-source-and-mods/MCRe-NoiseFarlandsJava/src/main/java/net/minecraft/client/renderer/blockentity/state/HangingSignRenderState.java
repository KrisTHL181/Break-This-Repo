package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.world.level.block.HangingSignBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HangingSignRenderState extends SignRenderState {
    public HangingSignBlock.Attachment attachmentType = HangingSignBlock.Attachment.CEILING;
}