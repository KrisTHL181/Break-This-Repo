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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreakingModel extends EntityModel<CreakingRenderState> {
   private final ModelPart head;
   private final KeyframeAnimation walkAnimation;
   private final KeyframeAnimation attackAnimation;
   private final KeyframeAnimation invulnerableAnimation;
   private final KeyframeAnimation deathAnimation;

   public CreakingModel(ModelPart p_452752_) {
      super(p_452752_);
      ModelPart modelpart = p_452752_.getChild("root");
      ModelPart modelpart1 = modelpart.getChild("upper_body");
      this.head = modelpart1.getChild("head");
      this.walkAnimation = CreakingAnimation.CREAKING_WALK.bake(modelpart);
      this.attackAnimation = CreakingAnimation.CREAKING_ATTACK.bake(modelpart);
      this.invulnerableAnimation = CreakingAnimation.CREAKING_INVULNERABLE.bake(modelpart);
      this.deathAnimation = CreakingAnimation.CREAKING_DEATH.bake(modelpart);
   }

   private static MeshDefinition createMesh() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
      partdefinition2.addOrReplaceChild(
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
      partdefinition2.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F).texOffs(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F),
         PartPose.offset(0.0F, -7.0F, 1.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F).texOffs(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F),
         PartPose.offset(-7.0F, -9.5F, 1.5F)
      );
      partdefinition2.addOrReplaceChild(
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
      partdefinition1.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F).texOffs(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F),
         PartPose.offset(1.5F, -16.0F, 0.5F)
      );
      partdefinition1.addOrReplaceChild(
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
      return meshdefinition;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = createMesh();
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public static LayerDefinition createEyesLayer() {
      MeshDefinition meshdefinition = createMesh();
      meshdefinition.getRoot().retainExactParts(Set.of("head"));
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(CreakingRenderState p_454590_) {
      super.setupAnim(p_454590_);
      this.head.xRot = p_454590_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_454590_.yRot * (float) (Math.PI / 180.0);
      if (p_454590_.canMove) {
         this.walkAnimation.applyWalk(p_454590_.walkAnimationPos, p_454590_.walkAnimationSpeed, 1.0F, 1.0F);
      }

      this.attackAnimation.apply(p_454590_.attackAnimationState, p_454590_.ageInTicks);
      this.invulnerableAnimation.apply(p_454590_.invulnerabilityAnimationState, p_454590_.ageInTicks);
      this.deathAnimation.apply(p_454590_.deathAnimationState, p_454590_.ageInTicks);
   }
}
