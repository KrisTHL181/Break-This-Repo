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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RavagerModel extends EntityModel<RavagerRenderState> {
   private final ModelPart head;
   private final ModelPart mouth;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart neck;

   public RavagerModel(ModelPart p_459263_) {
      super(p_459263_);
      this.neck = p_459263_.getChild("neck");
      this.head = this.neck.getChild("head");
      this.mouth = this.head.getChild("mouth");
      this.rightHindLeg = p_459263_.getChild("right_hind_leg");
      this.leftHindLeg = p_459263_.getChild("left_hind_leg");
      this.rightFrontLeg = p_459263_.getChild("right_front_leg");
      this.leftFrontLeg = p_459263_.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 16;
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "neck", CubeListBuilder.create().texOffs(68, 73).addBox(-5.0F, -1.0F, -18.0F, 10.0F, 10.0F, 18.0F), PartPose.offset(0.0F, -7.0F, 5.5F)
      );
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild(
         "head",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -20.0F, -14.0F, 16.0F, 20.0F, 16.0F).texOffs(0, 0).addBox(-2.0F, -6.0F, -18.0F, 4.0F, 8.0F, 4.0F),
         PartPose.offset(0.0F, 16.0F, -17.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_horn",
         CubeListBuilder.create().texOffs(74, 55).addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
         PartPose.offsetAndRotation(-10.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_horn",
         CubeListBuilder.create().texOffs(74, 55).mirror().addBox(0.0F, -14.0F, -2.0F, 2.0F, 14.0F, 4.0F),
         PartPose.offsetAndRotation(8.0F, -14.0F, -8.0F, 1.0995574F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "mouth", CubeListBuilder.create().texOffs(0, 36).addBox(-8.0F, 0.0F, -16.0F, 16.0F, 3.0F, 16.0F), PartPose.offset(0.0F, -2.0F, 2.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 55)
            .addBox(-7.0F, -10.0F, -7.0F, 14.0F, 16.0F, 20.0F)
            .texOffs(0, 91)
            .addBox(-6.0F, 6.0F, -7.0F, 12.0F, 13.0F, 18.0F),
         PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_hind_leg", CubeListBuilder.create().texOffs(96, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, 18.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_hind_leg", CubeListBuilder.create().texOffs(96, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, 18.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_front_leg", CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(-8.0F, -13.0F, -5.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_front_leg", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(-4.0F, 0.0F, -4.0F, 8.0F, 37.0F, 8.0F), PartPose.offset(8.0F, -13.0F, -5.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   public void setupAnim(RavagerRenderState p_455465_) {
      super.setupAnim(p_455465_);
      float f = p_455465_.stunnedTicksRemaining;
      float f1 = p_455465_.attackTicksRemaining;
      int i = 10;
      if (f1 > 0.0F) {
         float f2 = Mth.triangleWave(f1, 10.0F);
         float f3 = (1.0F + f2) * 0.5F;
         float f4 = f3 * f3 * f3 * 12.0F;
         float f5 = f4 * Mth.sin(this.neck.xRot);
         this.neck.z = -6.5F + f4;
         this.neck.y = -7.0F - f5;
         if (f1 > 5.0F) {
            this.mouth.xRot = Mth.sin((-4.0F + f1) / 4.0F) * (float) Math.PI * 0.4F;
         } else {
            this.mouth.xRot = (float) (Math.PI / 20) * Mth.sin((float) Math.PI * f1 / 10.0F);
         }
      } else {
         float f6 = -1.0F;
         float f8 = -1.0F * Mth.sin(this.neck.xRot);
         this.neck.x = 0.0F;
         this.neck.y = -7.0F - f8;
         this.neck.z = 5.5F;
         boolean flag = f > 0.0F;
         this.neck.xRot = flag ? 0.21991149F : 0.0F;
         this.mouth.xRot = (float) Math.PI * (flag ? 0.05F : 0.01F);
         if (flag) {
            double d0 = f / 40.0;
            this.neck.x = (float)Math.sin(d0 * 10.0) * 3.0F;
         } else if (p_455465_.roarAnimation > 0.0) {
            float f10 = Mth.sin(p_455465_.roarAnimation * (float) Math.PI * 0.25F);
            this.mouth.xRot = (float) (Math.PI / 2) * f10;
         }
      }

      this.head.xRot = p_455465_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_455465_.yRot * (float) (Math.PI / 180.0);
      float f7 = p_455465_.walkAnimationPos;
      float f9 = 0.4F * p_455465_.walkAnimationSpeed;
      this.rightHindLeg.xRot = Mth.cos(f7 * 0.6662F) * f9;
      this.leftHindLeg.xRot = Mth.cos(f7 * 0.6662F + (float) Math.PI) * f9;
      this.rightFrontLeg.xRot = Mth.cos(f7 * 0.6662F + (float) Math.PI) * f9;
      this.leftFrontLeg.xRot = Mth.cos(f7 * 0.6662F) * f9;
   }
}
