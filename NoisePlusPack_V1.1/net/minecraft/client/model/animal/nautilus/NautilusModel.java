package net.minecraft.client.model.animal.nautilus;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.NautilusAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.NautilusRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NautilusModel extends EntityModel<NautilusRenderState> {
   private static final float SWIM_ANIMATION_SPEED_MAX = 2.0F;
   private static final float SWIM_ANIMATION_SCALE_FACTOR = 3.0F;
   private static final float IDLE_SWIM_ANIMATION_SPEED = 0.2F;
   private static final float IDLE_SWIM_ANIMATION_SCALE = 5.0F;
   protected final ModelPart body;
   protected final ModelPart nautilus;
   private final KeyframeAnimation swimAnimation;

   public NautilusModel(ModelPart p_454944_) {
      super(p_454944_);
      this.nautilus = p_454944_.getChild("root");
      this.body = this.nautilus.getChild("body");
      this.swimAnimation = NautilusAnimation.SWIMMING.bake(p_454944_);
   }

   public static LayerDefinition createBodyLayer() {
      return LayerDefinition.create(createBodyMesh(), 128, 128);
   }

   public static MeshDefinition createBodyMesh() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 29.0F, -6.0F));
      partdefinition1.addOrReplaceChild(
         "shell",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F, new CubeDeformation(0.0F))
            .texOffs(0, 26)
            .addBox(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F, new CubeDeformation(0.0F))
            .texOffs(48, 26)
            .addBox(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -13.0F, 5.0F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 54)
            .addBox(-5.0F, -4.51F, -3.0F, 10.0F, 8.0F, 14.0F, new CubeDeformation(0.0F))
            .texOffs(0, 76)
            .addBox(-5.0F, -4.51F, 7.0F, 10.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -8.5F, 12.3F)
      );
      partdefinition2.addOrReplaceChild(
         "upper_mouth",
         CubeListBuilder.create().texOffs(54, 54).addBox(-5.0F, -2.0F, 0.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)),
         PartPose.offset(0.0F, -2.51F, 7.0F)
      );
      partdefinition2.addOrReplaceChild(
         "inner_mouth",
         CubeListBuilder.create().texOffs(54, 70).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -0.51F, 7.5F)
      );
      partdefinition2.addOrReplaceChild(
         "lower_mouth",
         CubeListBuilder.create().texOffs(54, 62).addBox(-5.0F, -1.98F, 0.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(-0.001F)),
         PartPose.offset(0.0F, 1.49F, 7.0F)
      );
      return meshdefinition;
   }

   public static LayerDefinition createBabyBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(-0.5F, 28.0F, -0.5F));
      partdefinition1.addOrReplaceChild(
         "shell",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-6.0F, -4.0F, -1.0F, 7.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(0, 11)
            .addBox(-6.0F, 0.0F, -1.0F, 7.0F, 4.0F, 9.0F, new CubeDeformation(0.0F))
            .texOffs(23, 11)
            .addBox(-6.0F, 0.0F, 5.0F, 7.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(3.0F, -8.0F, -2.0F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 24)
            .addBox(-2.5F, -3.01F, -1.0F, 5.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(0, 35)
            .addBox(-2.5F, -3.01F, 4.1F, 5.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.5F, -5.0F, 3.0F)
      );
      partdefinition2.addOrReplaceChild(
         "upper_mouth",
         CubeListBuilder.create().texOffs(24, 24).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(-0.001F)),
         PartPose.offset(0.0F, -2.01F, 3.9F)
      );
      partdefinition2.addOrReplaceChild(
         "inner_mouth",
         CubeListBuilder.create().texOffs(24, 32).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, -1.01F, 4.9F)
      );
      partdefinition2.addOrReplaceChild(
         "lower_mouth",
         CubeListBuilder.create().texOffs(24, 28).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(-0.001F)),
         PartPose.offset(0.0F, -0.01F, 3.9F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(NautilusRenderState p_455566_) {
      super.setupAnim(p_455566_);
      this.applyBodyRotation(p_455566_.yRot, p_455566_.xRot);
      this.swimAnimation.applyWalk(p_455566_.walkAnimationPos + p_455566_.ageInTicks / 5.0F, p_455566_.walkAnimationSpeed + 0.2F, 2.0F, 3.0F);
   }

   private void applyBodyRotation(float p_450962_, float p_453483_) {
      p_450962_ = Mth.clamp(p_450962_, -10.0F, 10.0F);
      p_453483_ = Mth.clamp(p_453483_, -10.0F, 10.0F);
      this.body.yRot = p_450962_ * (float) (Math.PI / 180.0);
      this.body.xRot = p_453483_ * (float) (Math.PI / 180.0);
   }
}
