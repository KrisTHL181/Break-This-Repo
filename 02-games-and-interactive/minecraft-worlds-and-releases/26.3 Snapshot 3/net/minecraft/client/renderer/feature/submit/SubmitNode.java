package net.minecraft.client.renderer.feature.submit;

import net.minecraft.client.renderer.feature.FeatureRendererType;

public interface SubmitNode {
   FeatureRendererType<? extends SubmitNode> featureType();
}
