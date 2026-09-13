package net.minecraft.client.model.object.equipment;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElytraModel extends EntityModel<HumanoidRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = MeshTransformer.scaling(0.5F);
   private final ModelPart rightWing;
   private final ModelPart leftWing;

   public ElytraModel(ModelPart p_458871_) {
      super(p_458871_);
      this.leftWing = p_458871_.getChild("left_wing");
      this.rightWing = p_458871_.getChild("right_wing");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      CubeDeformation cubedeformation = new CubeDeformation(1.0F);
      partdefinition.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().texOffs(22, 0).addBox(-10.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubedeformation),
         PartPose.offsetAndRotation(5.0F, 0.0F, 0.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12))
      );
      partdefinition.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 10.0F, 20.0F, 2.0F, cubedeformation),
         PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12))
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(HumanoidRenderState p_456557_) {
      super.setupAnim(p_456557_);
      this.leftWing.y = p_456557_.isCrouching ? 3.0F : 0.0F;
      this.leftWing.xRot = p_456557_.elytraRotX;
      this.leftWing.zRot = p_456557_.elytraRotZ;
      this.leftWing.yRot = p_456557_.elytraRotY;
      this.rightWing.yRot = -this.leftWing.yRot;
      this.rightWing.y = this.leftWing.y;
      this.rightWing.xRot = this.leftWing.xRot;
      this.rightWing.zRot = -this.leftWing.zRot;
   }
}
