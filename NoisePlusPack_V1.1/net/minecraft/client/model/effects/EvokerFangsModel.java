package net.minecraft.client.model.effects;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EvokerFangsRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerFangsModel extends EntityModel<EvokerFangsRenderState> {
   private static final String BASE = "base";
   private static final String UPPER_JAW = "upper_jaw";
   private static final String LOWER_JAW = "lower_jaw";
   private final ModelPart base;
   private final ModelPart upperJaw;
   private final ModelPart lowerJaw;

   public EvokerFangsModel(ModelPart p_451055_) {
      super(p_451055_);
      this.base = p_451055_.getChild("base");
      this.upperJaw = this.base.getChild("upper_jaw");
      this.lowerJaw = this.base.getChild("lower_jaw");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "base", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 12.0F, 10.0F), PartPose.offset(-5.0F, 24.0F, -5.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, 0.0F, 0.0F, 4.0F, 14.0F, 8.0F);
      partdefinition1.addOrReplaceChild("upper_jaw", cubelistbuilder, PartPose.offsetAndRotation(6.5F, 0.0F, 1.0F, 0.0F, 0.0F, 2.042035F));
      partdefinition1.addOrReplaceChild("lower_jaw", cubelistbuilder, PartPose.offsetAndRotation(3.5F, 0.0F, 9.0F, 0.0F, (float) Math.PI, 4.2411504F));
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(EvokerFangsRenderState p_461036_) {
      super.setupAnim(p_461036_);
      float f = p_461036_.biteProgress;
      float f1 = Math.min(f * 2.0F, 1.0F);
      f1 = 1.0F - f1 * f1 * f1;
      this.upperJaw.zRot = (float) Math.PI - f1 * 0.35F * (float) Math.PI;
      this.lowerJaw.zRot = (float) Math.PI + f1 * 0.35F * (float) Math.PI;
      this.base.y = this.base.y - (f + Mth.sin(f * 2.7F)) * 7.2F;
      float f2 = 1.0F;
      if (f > 0.9F) {
         f2 *= (1.0F - f) / 0.1F;
      }

      this.root.y = 24.0F - 20.0F * f2;
      this.root.xScale = f2;
      this.root.yScale = f2;
      this.root.zScale = f2;
   }
}
