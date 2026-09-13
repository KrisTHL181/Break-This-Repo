package net.minecraft.client.model.animal.llama;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.util.Mth;

public class LlamaModel extends EntityModel<LlamaRenderState> {
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightChest;
   private final ModelPart leftChest;

   public LlamaModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.rightChest = root.getChild("right_chest");
      this.leftChest = root.getChild("left_chest");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer(final CubeDeformation g) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, -14.0F, -10.0F, 4.0F, 4.0F, 9.0F, g)
            .texOffs(0, 14)
            .addBox("neck", -4.0F, -16.0F, -6.0F, 8.0F, 18.0F, 6.0F, g)
            .texOffs(17, 0)
            .addBox("ear", -4.0F, -19.0F, -4.0F, 3.0F, 3.0F, 2.0F, g)
            .texOffs(17, 0)
            .addBox("ear", 1.0F, -19.0F, -4.0F, 3.0F, 3.0F, 2.0F, g),
         PartPose.offset(0.0F, 7.0F, -6.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(29, 0).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F, g),
         PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_chest",
         CubeListBuilder.create().texOffs(45, 28).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, g),
         PartPose.offsetAndRotation(-8.5F, 3.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      root.addOrReplaceChild(
         "left_chest",
         CubeListBuilder.create().texOffs(45, 41).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, g),
         PartPose.offsetAndRotation(5.5F, 3.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      int legWidth = 4;
      int legHeight = 14;
      CubeListBuilder leg = CubeListBuilder.create().texOffs(29, 29).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F, g);
      root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.5F, 10.0F, 6.0F));
      root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.5F, 10.0F, 6.0F));
      root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.5F, 10.0F, -5.0F));
      root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.5F, 10.0F, -5.0F));
      return LayerDefinition.create(mesh, 128, 64);
   }

   public void setupAnim(final LlamaRenderState state) {
      super.setupAnim(state);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      float animationSpeed = state.walkAnimationSpeed;
      float animationPos = state.walkAnimationPos;
      this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
      this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
      this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * 1.4F * animationSpeed;
      this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * 1.4F * animationSpeed;
      this.rightChest.visible = state.hasChest;
      this.leftChest.visible = state.hasChest;
   }
}
