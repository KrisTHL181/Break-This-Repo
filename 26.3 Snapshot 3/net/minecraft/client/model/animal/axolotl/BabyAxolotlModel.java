package net.minecraft.client.model.animal.axolotl;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.BabyAxolotlAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;

public class BabyAxolotlModel extends EntityModel<AxolotlRenderState> {
   private static final float MAX_WALK_ANIMATION_SPEED = 15.0F;
   private static final float WALK_ANIMATION_SCALE_FACTOR = 30.0F;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation walkUnderwaterAnimation;
   private final KeyframeAnimation swimAnimation;
   private final KeyframeAnimation idleOnGroundAnimation;
   private final KeyframeAnimation idleOnGroundUnderWaterAnimation;
   private final KeyframeAnimation idleUnderWaterAnimation;
   private final KeyframeAnimation playDeadAnimation;

   public BabyAxolotlModel(final ModelPart root) {
      super(root);
      this.swimAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_SWIM.bake(root);
      this.walkAnimation = BabyAxolotlAnimation.AXOLOTL_WALK_FLOOR.bake(root);
      this.walkUnderwaterAnimation = BabyAxolotlAnimation.WALK_FLOOR_UNDERWATER.bake(root);
      this.idleUnderWaterAnimation = BabyAxolotlAnimation.IDLE_UNDERWATER.bake(root);
      this.idleOnGroundUnderWaterAnimation = BabyAxolotlAnimation.IDLE_FLOOR_UNDERWATER.bake(root);
      this.idleOnGroundAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_IDLE_FLOOR.bake(root);
      this.playDeadAnimation = BabyAxolotlAnimation.BABY_AXOLOTL_PLAY_DEAD.bake(root);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, -0.75F, -2.75F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 12)
            .addBox(0.0F, -1.75F, -2.75F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -1.25F, 1.75F)
      );
      body.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(20, 16).addBox(-3.0F, 0.0F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 0.25F, -1.25F)
      );
      PartDefinition right_leg = body.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, 0.25F, 1.75F, 0.0F, 1.5708F, 1.5708F)
      );
      right_leg.addOrReplaceChild(
         "right_leg_r1",
         CubeListBuilder.create().texOffs(20, 14).addBox(0.0F, 0.0F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 1.5708F)
      );
      body.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(20, 13).addBox(0.0F, 0.0F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 0.25F, -1.25F)
      );
      body.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(20, 14).addBox(0.0F, 0.0F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 0.25F, 1.75F)
      );
      body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(10, 9).addBox(0.0F, -1.5F, -1.0F, 0.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -0.25F, 3.25F)
      );
      PartDefinition head = body.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 0.25F, -2.75F)
      );
      head.addOrReplaceChild(
         "left_gills",
         CubeListBuilder.create().texOffs(20, 8).addBox(0.0F, -3.5F, 0.0F, 3.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -0.5F, -2.0F)
      );
      head.addOrReplaceChild(
         "right_gills",
         CubeListBuilder.create().texOffs(20, 3).addBox(-3.0F, -3.5F, 0.0F, 3.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-3.0F, -0.5F, -2.0F)
      );
      head.addOrReplaceChild(
         "top_gills",
         CubeListBuilder.create().texOffs(20, 0).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -2.0F, -2.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(final AxolotlRenderState state) {
      super.setupAnim(state);
      if (state.walkAnimationState.isStarted()) {
         this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 15.0F, 30.0F);
      }

      this.swimAnimation.apply(state.swimAnimation, state.ageInTicks);
      this.walkUnderwaterAnimation.apply(state.walkAnimationState, state.ageInTicks);
      this.idleOnGroundAnimation.apply(state.idleOnGroundAnimationState, state.ageInTicks);
      this.idleUnderWaterAnimation.apply(state.idleUnderWaterAnimationState, state.ageInTicks);
      this.idleOnGroundUnderWaterAnimation.apply(state.idleUnderWaterOnGroundAnimationState, state.ageInTicks);
      this.playDeadAnimation.apply(state.playDeadAnimationState, state.ageInTicks);
   }
}
