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

public class EvokerFangsModel extends EntityModel<EvokerFangsRenderState> {
   private static final String BASE = "base";
   private static final String UPPER_JAW = "upper_jaw";
   private static final String LOWER_JAW = "lower_jaw";
   private final ModelPart base;
   private final ModelPart upperJaw;
   private final ModelPart lowerJaw;

   public EvokerFangsModel(final ModelPart root) {
      super(root);
      this.base = root.getChild("base");
      this.upperJaw = this.base.getChild("upper_jaw");
      this.lowerJaw = this.base.getChild("lower_jaw");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition base = root.addOrReplaceChild(
         "base", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 12.0F, 10.0F), PartPose.offset(-5.0F, 24.0F, -5.0F)
      );
      CubeListBuilder jaw = CubeListBuilder.create().texOffs(40, 0).addBox(0.0F, 0.0F, 0.0F, 4.0F, 14.0F, 8.0F);
      base.addOrReplaceChild("upper_jaw", jaw, PartPose.offsetAndRotation(6.5F, 0.0F, 1.0F, 0.0F, 0.0F, 2.042035F));
      base.addOrReplaceChild("lower_jaw", jaw, PartPose.offsetAndRotation(3.5F, 0.0F, 9.0F, 0.0F, (float) Math.PI, 4.2411504F));
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final EvokerFangsRenderState state) {
      super.setupAnim(state);
      float biteProgress = state.biteProgress;
      float biteAmount = Math.min(biteProgress * 2.0F, 1.0F);
      biteAmount = 1.0F - biteAmount * biteAmount * biteAmount;
      this.upperJaw.zRot = (float) Math.PI - biteAmount * 0.35F * (float) Math.PI;
      this.lowerJaw.zRot = (float) Math.PI + biteAmount * 0.35F * (float) Math.PI;
      this.base.y = this.base.y - (biteProgress + Mth.sin(biteProgress * 2.7F)) * 7.2F;
      float preScale = 1.0F;
      if (biteProgress > 0.9F) {
         preScale *= (1.0F - biteProgress) / 0.1F;
      }

      this.root.y = 24.0F - 20.0F * preScale;
      this.root.xScale = preScale;
      this.root.yScale = preScale;
      this.root.zScale = preScale;
   }
}
