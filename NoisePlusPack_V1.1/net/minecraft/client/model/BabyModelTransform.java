package net.minecraft.client.model;

import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BabyModelTransform(
   boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset, float babyHeadScale, float babyBodyScale, float bodyYOffset, Set<String> headParts
) implements MeshTransformer {
   public BabyModelTransform(Set<String> p_361409_) {
      this(false, 5.0F, 2.0F, p_361409_);
   }

   public BabyModelTransform(boolean p_363472_, float p_363171_, float p_366309_, Set<String> p_363509_) {
      this(p_363472_, p_363171_, p_366309_, 2.0F, 2.0F, 24.0F, p_363509_);
   }

   @Override
   public MeshDefinition apply(MeshDefinition p_365887_) {
      float f = this.scaleHead ? 1.5F / this.babyHeadScale : 1.0F;
      float f1 = 1.0F / this.babyBodyScale;
      UnaryOperator<PartPose> unaryoperator = p_364896_ -> p_364896_.translated(0.0F, this.babyYHeadOffset, this.babyZHeadOffset).scaled(f);
      UnaryOperator<PartPose> unaryoperator1 = p_363020_ -> p_363020_.translated(0.0F, this.bodyYOffset, 0.0F).scaled(f1);
      MeshDefinition meshdefinition = new MeshDefinition();

      for (Entry<String, PartDefinition> entry : p_365887_.getRoot().getChildren()) {
         String s = entry.getKey();
         PartDefinition partdefinition = entry.getValue();
         meshdefinition.getRoot().addOrReplaceChild(s, partdefinition.transformed(this.headParts.contains(s) ? unaryoperator : unaryoperator1));
      }

      return meshdefinition;
   }
}
