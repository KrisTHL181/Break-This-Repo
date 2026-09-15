package net.minecraft.client.renderer.feature.submit;

import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BatchableSubmit extends SubmitNode {
    Object batchKey();

    @Override
    FeatureRendererType<? extends BatchableSubmit> featureType();
}