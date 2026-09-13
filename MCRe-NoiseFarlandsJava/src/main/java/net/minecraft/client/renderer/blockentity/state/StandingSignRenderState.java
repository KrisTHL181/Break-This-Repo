package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StandingSignRenderState extends SignRenderState {
    public PlainSignBlock.Attachment attachmentType = PlainSignBlock.Attachment.GROUND;
}