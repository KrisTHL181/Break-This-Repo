package net.minecraft.client.model.monster.ravager;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.util.Mth;

public class RavagerModel extends EntityModel<RavagerRenderState> {
   private final ModelPart head;
   private final ModelPart mouth;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart neck;

   public RavagerModel(final ModelPart root) {
      super(root);
      this.neck = root.getChild("neck");
      this.head = this.neck.getChild("head");
      this.mouth = this.head.getChild("mouth");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      int legSize = 16;
      PartDefinition neck = root.addOrReplaceChild(
         "neck", CubeListBuilder.create().texOffs(68, 73).addBox(-5.0F, -1.0F, -18.0F, 10.0F, 10.0F, 18.0F), PartPose.offset(0.0F, -7.0F, 5.5F)
      );
      PartDefinition head = neck.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -20.0F, -14.0F, 16.0F, 20.0F, 16.0F).texOffs(0, 0).addBox(-2.0F, -6.0F, -18.0F, 4.0F, 8.0F, 4.0F),
         PartPose.offset(0.0F, 16.0F, -17.0F)
      );
      head.addOrReplaceChild(
         "right_horn",
         CubeListBuilder.create().texOffs(74, 55).addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
         PartPose.offsetAndRotation(-10.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "left_horn",
         CubeListBuilder.create().texOffs(74, 55).mirror().addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
         PartPose.offsetAndRotation(8.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
      );
      head.addOrReplaceChild(
         "mouth", CubeListBuilder.create().texOffs(0, 36).addBox(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F), PartPose.offset(0.0F, -2.0F, 2.0F)
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 55)
            .addBox(-7.0F, -10.0F, -7.0F, 14.0F, 16.0F, 20.0F)
            .texOffs(0, 91)
            .addBox(-6.0F, 6.0F, -7.0F, 12.0F, 13.0F, 18.0F),
         PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, 18.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(96, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, 18.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, -5.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, -5.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public void setupAnim(final RavagerRenderState state) {
      super.setupAnim(state);
      float stunnedTick = state.stunnedTicksRemaining;
      float attackTick = state.attackTicksRemaining;
      int attackTime = 10;
      if (attackTick > 0.0F) {
         float headAnim = Mth.triangleWave(attackTick, 10.0F);
         float scaled = (1.0F + headAnim) * 0.5F;
         float headPos = scaled * scaled * scaled * 12.0F;
         float yOffset = headPos * Mth.sin(this.neck.xRot);
         this.neck.z = -6.5F + headPos;
         this.neck.y = -7.0F - yOffset;
         if (attackTick > 5.0F) {
            this.mouth.xRot = Mth.sin((-4.0F + attackTick) / 4.0F) * (float) Math.PI * 0.4F;
         } else {
            this.mouth.xRot = (float) (Math.PI / 20) * Mth.sin((float) Math.PI * attackTick / 10.0F);
         }
      } else {
         float headPos = -1.0F;
         float yOffset = -1.0F * Mth.sin(this.neck.xRot);
         this.neck.x = 0.0F;
         this.neck.y = -7.0F - yOffset;
         this.neck.z = 5.5F;
         boolean isStunned = stunnedTick > 0.0F;
         this.neck.xRot = isStunned ? 0.21991149F : 0.0F;
         this.mouth.xRot = (float) Math.PI * (isStunned ? 0.05F : 0.01F);
         if (isStunned) {
            double speed = stunnedTick / 40.0;
            this.neck.x = (float)Math.sin(speed * 10.0) * 3.0F;
         } else if (state.roarAnimation > 0.0) {
            float mouthAnim = Mth.sin(state.roarAnimation * (float) Math.PI * 0.25F);
            this.mouth.xRot = (float) (Math.PI / 2) * mouthAnim;
         }
      }

      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      float animationPos = state.walkAnimationPos;
      float legRot = 0.4F * state.walkAnimationSpeed;
      this.rightHindLeg.xRot = Mth.cos(animationPos * 0.6662F) * legRot;
      this.leftHindLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * legRot;
      this.rightFrontLeg.xRot = Mth.cos(animationPos * 0.6662F + (float) Math.PI) * legRot;
      this.leftFrontLeg.xRot = Mth.cos(animationPos * 0.6662F) * legRot;
   }
}
