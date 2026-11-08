package net.minecraft.client.renderer.feature.submit;

import net.minecraft.client.renderer.feature.FeatureRendererType;

public interface BatchableSubmit extends SubmitNode {
   Object batchKey();

   @Override
   FeatureRendererType<? extends BatchableSubmit> featureType();
}
