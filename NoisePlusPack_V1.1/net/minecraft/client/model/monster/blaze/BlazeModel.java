package net.minecraft.client.model.monster.blaze;

import java.util.Arrays;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlazeModel extends EntityModel<LivingEntityRenderState> {
   private final ModelPart[] upperBodyParts;
   private final ModelPart head;

   public BlazeModel(ModelPart p_451350_) {
      super(p_451350_);
      this.head = p_451350_.getChild("head");
      this.upperBodyParts = new ModelPart[12];
      Arrays.setAll(this.upperBodyParts, p_453991_ -> p_451350_.getChild(getPartName(p_453991_)));
   }

   private static String getPartName(int p_460472_) {
      return "part" + p_460472_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      float f = 0.0F;
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);

      for (int i = 0; i < 4; i++) {
         float f1 = Mth.cos(f) * 9.0F;
         float f2 = -2.0F + Mth.cos(i * 2 * 0.25F);
         float f3 = Mth.sin(f) * 9.0F;
         partdefinition.addOrReplaceChild(getPartName(i), cubelistbuilder, PartPose.offset(f1, f2, f3));
         f++;
      }

      f = (float) (Math.PI / 4);

      for (int j = 4; j < 8; j++) {
         float f4 = Mth.cos(f) * 7.0F;
         float f6 = 2.0F + Mth.cos(j * 2 * 0.25F);
         float f8 = Mth.sin(f) * 7.0F;
         partdefinition.addOrReplaceChild(getPartName(j), cubelistbuilder, PartPose.offset(f4, f6, f8));
         f++;
      }

      f = 0.47123894F;

      for (int k = 8; k < 12; k++) {
         float f5 = Mth.cos(f) * 5.0F;
         float f7 = 11.0F + Mth.cos(k * 1.5F * 0.5F);
         float f9 = Mth.sin(f) * 5.0F;
         partdefinition.addOrReplaceChild(getPartName(k), cubelistbuilder, PartPose.offset(f5, f7, f9));
         f++;
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(LivingEntityRenderState p_456733_) {
      super.setupAnim(p_456733_);
      float f = p_456733_.ageInTicks * (float) Math.PI * -0.1F;

      for (int i = 0; i < 4; i++) {
         this.upperBodyParts[i].y = -2.0F + Mth.cos((i * 2 + p_456733_.ageInTicks) * 0.25F);
         this.upperBodyParts[i].x = Mth.cos(f) * 9.0F;
         this.upperBodyParts[i].z = Mth.sin(f) * 9.0F;
         f++;
      }

      f = (float) (Math.PI / 4) + p_456733_.ageInTicks * (float) Math.PI * 0.03F;

      for (int j = 4; j < 8; j++) {
         this.upperBodyParts[j].y = 2.0F + Mth.cos((j * 2 + p_456733_.ageInTicks) * 0.25F);
         this.upperBodyParts[j].x = Mth.cos(f) * 7.0F;
         this.upperBodyParts[j].z = Mth.sin(f) * 7.0F;
         f++;
      }

      f = 0.47123894F + p_456733_.ageInTicks * (float) Math.PI * -0.05F;

      for (int k = 8; k < 12; k++) {
         this.upperBodyParts[k].y = 11.0F + Mth.cos((k * 1.5F + p_456733_.ageInTicks) * 0.5F);
         this.upperBodyParts[k].x = Mth.cos(f) * 5.0F;
         this.upperBodyParts[k].z = Mth.sin(f) * 5.0F;
         f++;
      }

      this.head.yRot = p_456733_.yRot * (float) (Math.PI / 180.0);
      this.head.xRot = p_456733_.xRot * (float) (Math.PI / 180.0);
   }
}
