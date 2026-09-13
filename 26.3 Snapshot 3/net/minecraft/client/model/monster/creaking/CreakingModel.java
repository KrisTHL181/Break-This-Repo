package net.minecraft.client.model.monster.creaking;

import java.util.Set;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.CreakingAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;

public class CreakingModel extends EntityModel<CreakingRenderState> {
   private final ModelPart head;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation attackAnimation;
   private final KeyframeAnimation invulnerableAnimation;
   private final KeyframeAnimation deathAnimation;

   public CreakingModel(final ModelPart roots) {
      super(roots);
      ModelPart root = roots.getChild("root");
      ModelPart upperBody = root.getChild("upper_body");
      this.head = upperBody.getChild("head");
      this.walkAnimation = CreakingAnimation.CREAKING_WALK.bake(root);
      this.attackAnimation = CreakingAnimation.CREAKING_ATTACK.bake(root);
      this.invulnerableAnimation = CreakingAnimation.CREAKING_INVULNERABLE.bake(root);
      this.deathAnimation = CreakingAnimation.CREAKING_DEATH.bake(root);
   }

   private static MeshDefinition createMesh() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      PartDefinition root = partDefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
      upperBody.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
            .texOffs(28, 31)
            .addBox(-3.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F)
            .texOffs(12, 40)
            .addBox(3.0F, -13.0F, 0.0F, 9.0F, 14.0F, 0.0F)
            .texOffs(34, 12)
            .addBox(-12.0F, -14.0F, 0.0F, 9.0F, 14.0F, 0.0F),
         PartPose.offset(-3.0F, -11.0F, 0.0F)
      );
      upperBody.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F).texOffs(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F),
         PartPose.offset(0.0F, -7.0F, 1.0F)
      );
      upperBody.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F).texOffs(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F),
         PartPose.offset(-7.0F, -9.5F, 1.5F)
      );
      upperBody.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(30, 40)
            .addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
            .texOffs(52, 12)
            .addBox(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
            .texOffs(52, 19)
            .addBox(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F),
         PartPose.offset(6.0F, -9.0F, 0.5F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F).texOffs(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F),
         PartPose.offset(1.5F, -16.0F, 0.5F)
      );
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create()
            .texOffs(0, 34)
            .addBox(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
            .texOffs(45, 46)
            .addBox(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
            .texOffs(12, 34)
            .addBox(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F),
         PartPose.offset(-1.0F, -17.5F, 0.5F)
      );
      return meshDefinition;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = createMesh();
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createEyesLayer() {
      MeshDefinition mesh = createMesh();
      mesh.getRoot().retainExactParts(Set.of("head"));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final CreakingRenderState state) {
      super.setupAnim(state);
      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
      if (state.canMove) {
         this.walkAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 1.0F, 1.0F);
      }

      this.attackAnimation.apply(state.attackAnimationState, state.ageInTicks);
      this.invulnerableAnimation.apply(state.invulnerabilityAnimationState, state.ageInTicks);
      this.deathAnimation.apply(state.deathAnimationState, state.ageInTicks);
   }
}
