package net.minecraft.client.model.animal.sniffer;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.SnifferAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;

public class SnifferModel extends EntityModel<SnifferRenderState> {
   private static final float WALK_ANIMATION_SPEED_MAX = 9.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 100.0F;
   private final ModelPart head;
   private final KeyframeAnimation sniffSearchAnimation;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation digAnimation;
   private final KeyframeAnimation longSniffAnimation;
   private final KeyframeAnimation standUpAnimation;
   private final KeyframeAnimation happyAnimation;
   private final KeyframeAnimation sniffSniffAnimation;

   public SnifferModel(final ModelPart root) {
      super(root);
      this.head = root.getChild("bone").getChild("body").getChild("head");
      this.sniffSearchAnimation = SnifferAnimation.SNIFFER_SNIFF_SEARCH.bake(root);
      this.walkAnimation = SnifferAnimation.SNIFFER_WALK.bake(root);
      this.digAnimation = SnifferAnimation.SNIFFER_DIG.bake(root);
      this.longSniffAnimation = SnifferAnimation.SNIFFER_LONGSNIFF.bake(root);
      this.standUpAnimation = SnifferAnimation.SNIFFER_STAND_UP.bake(root);
      this.happyAnimation = SnifferAnimation.SNIFFER_HAPPY.bake(root);
      this.sniffSniffAnimation = SnifferAnimation.SNIFFER_SNIFFSNIFF.bake(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 5.0F, 0.0F));
      PartDefinition body = bone.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(62, 68)
            .addBox(-12.5F, -14.0F, -20.0F, 25.0F, 29.0F, 40.0F, new CubeDeformation(0.0F))
            .texOffs(62, 0)
            .addBox(-12.5F, -14.0F, -20.0F, 25.0F, 24.0F, 40.0F, new CubeDeformation(0.5F))
            .texOffs(87, 68)
            .addBox(-12.5F, 12.0F, -20.0F, 25.0F, 0.0F, 40.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(32, 87).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-7.5F, 10.0F, -15.0F)
      );
      bone.addOrReplaceChild(
         "right_mid_leg",
         CubeListBuilder.create().texOffs(32, 105).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-7.5F, 10.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(32, 123).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-7.5F, 10.0F, 15.0F)
      );
      bone.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(0, 87).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(7.5F, 10.0F, -15.0F)
      );
      bone.addOrReplaceChild(
         "left_mid_leg",
         CubeListBuilder.create().texOffs(0, 105).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(7.5F, 10.0F, 0.0F)
      );
      bone.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(0, 123).addBox(-3.5F, -1.0F, -4.0F, 7.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(7.5F, 10.0F, 15.0F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(8, 15)
            .addBox(-6.5F, -7.5F, -11.5F, 13.0F, 18.0F, 11.0F, new CubeDeformation(0.0F))
            .texOffs(8, 4)
            .addBox(-6.5F, 7.5F, -11.5F, 13.0F, 0.0F, 11.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 6.5F, -19.48F)
      );
      head.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(2, 0).addBox(0.0F, 0.0F, -3.0F, 1.0F, 19.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(6.51F, -7.5F, -4.51F)
      );
      head.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 19.0F, 7.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-6.51F, -7.5F, -4.51F)
      );
      head.addOrReplaceChild(
         "nose",
         CubeListBuilder.create().texOffs(10, 45).addBox(-6.5F, -2.0F, -9.0F, 13.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -4.5F, -11.5F)
      );
      head.addOrReplaceChild(
         "lower_beak",
         CubeListBuilder.create().texOffs(10, 57).addBox(-6.5F, -7.0F, -8.0F, 13.0F, 12.0F, 9.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 2.5F, -12.5F)
      );
      return LayerDefinition.create(mesh, 192, 192);
   }

   public void setupAnim(final SnifferRenderState state) {
      super.setupAnim(state);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      if (state.isSearching) {
         this.sniffSearchAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 9.0F, 100.0F);
      } else {
         this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 9.0F, 100.0F);
      }

      this.digAnimation.apply(state.diggingAnimationState, state.ageInTicks);
      this.longSniffAnimation.apply(state.sniffingAnimationState, state.ageInTicks);
      this.standUpAnimation.apply(state.risingAnimationState, state.ageInTicks);
      this.happyAnimation.apply(state.feelingHappyAnimationState, state.ageInTicks);
      this.sniffSniffAnimation.apply(state.scentingAnimationState, state.ageInTicks);
   }
}
