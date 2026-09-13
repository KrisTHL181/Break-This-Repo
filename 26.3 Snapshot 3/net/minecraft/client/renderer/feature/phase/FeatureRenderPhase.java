package net.minecraft.client.renderer.feature.phase;

import java.util.Collection;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

public interface FeatureRenderPhase<Submit extends SubmitNode> {
   void submit(Submit submit);

   void sortInto(FeatureRenderPhase.Output output);

   boolean isEmpty();

   @FunctionalInterface
   interface Output {
      void accept(SubmitNode submit, boolean strictlyOrdered);

      default <Submit extends SubmitNode> void acceptFeatureGroup(
         final FeatureRendererType<Submit> featureType, final Collection<Submit> submits, final boolean strictlyOrdered
      ) {
         for (Submit submit : submits) {
            if (submit.featureType() != featureType) {
               throw new IllegalArgumentException(submit + " was not of feature type " + featureType);
            }

            this.accept(submit, strictlyOrdered);
         }
      }
   }
}
