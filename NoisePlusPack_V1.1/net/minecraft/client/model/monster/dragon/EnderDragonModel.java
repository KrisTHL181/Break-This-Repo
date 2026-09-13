package net.minecraft.client.model.monster.dragon;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnderDragonModel extends EntityModel<EnderDragonRenderState> {
   private static final int NECK_PART_COUNT = 5;
   private static final int TAIL_PART_COUNT = 12;
   private final ModelPart head;
   private final ModelPart[] neckParts = new ModelPart[5];
   private final ModelPart[] tailParts = new ModelPart[12];
   private final ModelPart jaw;
   private final ModelPart body;
   private final ModelPart leftWing;
   private final ModelPart leftWingTip;
   private final ModelPart leftFrontLeg;
   private final ModelPart leftFrontLegTip;
   private final ModelPart leftFrontFoot;
   private final ModelPart leftRearLeg;
   private final ModelPart leftRearLegTip;
   private final ModelPart leftRearFoot;
   private final ModelPart rightWing;
   private final ModelPart rightWingTip;
   private final ModelPart rightFrontLeg;
   private final ModelPart rightFrontLegTip;
   private final ModelPart rightFrontFoot;
   private final ModelPart rightRearLeg;
   private final ModelPart rightRearLegTip;
   private final ModelPart rightRearFoot;

   private static String neckName(int p_452441_) {
      return "neck" + p_452441_;
   }

   private static String tailName(int p_458218_) {
      return "tail" + p_458218_;
   }

   public EnderDragonModel(ModelPart p_459221_) {
      super(p_459221_);
      this.head = p_459221_.getChild("head");
      this.jaw = this.head.getChild("jaw");

      for (int i = 0; i < this.neckParts.length; i++) {
         this.neckParts[i] = p_459221_.getChild(neckName(i));
      }

      for (int j = 0; j < this.tailParts.length; j++) {
         this.tailParts[j] = p_459221_.getChild(tailName(j));
      }

      this.body = p_459221_.getChild("body");
      this.leftWing = this.body.getChild("left_wing");
      this.leftWingTip = this.leftWing.getChild("left_wing_tip");
      this.leftFrontLeg = this.body.getChild("left_front_leg");
      this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
      this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
      this.leftRearLeg = this.body.getChild("left_hind_leg");
      this.leftRearLegTip = this.leftRearLeg.getChild("left_hind_leg_tip");
      this.leftRearFoot = this.leftRearLegTip.getChild("left_hind_foot");
      this.rightWing = this.body.getChild("right_wing");
      this.rightWingTip = this.rightWing.getChild("right_wing_tip");
      this.rightFrontLeg = this.body.getChild("right_front_leg");
      this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
      this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
      this.rightRearLeg = this.body.getChild("right_hind_leg");
      this.rightRearLegTip = this.rightRearLeg.getChild("right_hind_leg_tip");
      this.rightRearFoot = this.rightRearLegTip.getChild("right_hind_foot");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = -16.0F;
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .addBox("upperlip", -6.0F, -1.0F, -24.0F, 12, 5, 16, 176, 44)
            .addBox("upperhead", -8.0F, -8.0F, -10.0F, 16, 16, 16, 112, 30)
            .mirror()
            .addBox("scale", -5.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
            .addBox("nostril", -5.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0)
            .mirror()
            .addBox("scale", 3.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0)
            .addBox("nostril", 3.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0),
         PartPose.offset(0.0F, 20.0F, -62.0F)
      );
      partdefinition1.addOrReplaceChild(
         "jaw", CubeListBuilder.create().addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16, 176, 65), PartPose.offset(0.0F, 4.0F, -8.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create()
         .addBox("box", -5.0F, -5.0F, -5.0F, 10, 10, 10, 192, 104)
         .addBox("scale", -1.0F, -9.0F, -3.0F, 2, 4, 6, 48, 0);

      for (int i = 0; i < 5; i++) {
         partdefinition.addOrReplaceChild(neckName(i), cubelistbuilder, PartPose.offset(0.0F, 20.0F, -12.0F - i * 10.0F));
      }

      for (int j = 0; j < 12; j++) {
         partdefinition.addOrReplaceChild(tailName(j), cubelistbuilder, PartPose.offset(0.0F, 10.0F, 60.0F + j * 10.0F));
      }

      PartDefinition partdefinition12 = partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .addBox("body", -12.0F, 1.0F, -16.0F, 24, 24, 64, 0, 0)
            .addBox("scale", -1.0F, -5.0F, -10.0F, 2, 6, 12, 220, 53)
            .addBox("scale", -1.0F, -5.0F, 10.0F, 2, 6, 12, 220, 53)
            .addBox("scale", -1.0F, -5.0F, 30.0F, 2, 6, 12, 220, 53),
         PartPose.offset(0.0F, 3.0F, 8.0F)
      );
      PartDefinition partdefinition2 = partdefinition12.addOrReplaceChild(
         "left_wing",
         CubeListBuilder.create().mirror().addBox("bone", 0.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
         PartPose.offset(12.0F, 2.0F, -6.0F)
      );
      partdefinition2.addOrReplaceChild(
         "left_wing_tip",
         CubeListBuilder.create().mirror().addBox("bone", 0.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
         PartPose.offset(56.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition3 = partdefinition12.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104),
         PartPose.offsetAndRotation(12.0F, 17.0F, -6.0F, 1.3F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition4 = partdefinition3.addOrReplaceChild(
         "left_front_leg_tip",
         CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138),
         PartPose.offsetAndRotation(0.0F, 20.0F, -1.0F, -0.5F, 0.0F, 0.0F)
      );
      partdefinition4.addOrReplaceChild(
         "left_front_foot",
         CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104),
         PartPose.offsetAndRotation(0.0F, 23.0F, 0.0F, 0.75F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition5 = partdefinition12.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0),
         PartPose.offsetAndRotation(16.0F, 13.0F, 34.0F, 1.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition6 = partdefinition5.addOrReplaceChild(
         "left_hind_leg_tip",
         CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0),
         PartPose.offsetAndRotation(0.0F, 32.0F, -4.0F, 0.5F, 0.0F, 0.0F)
      );
      partdefinition6.addOrReplaceChild(
         "left_hind_foot",
         CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0),
         PartPose.offsetAndRotation(0.0F, 31.0F, 4.0F, 0.75F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition7 = partdefinition12.addOrReplaceChild(
         "right_wing",
         CubeListBuilder.create().addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88),
         PartPose.offset(-12.0F, 2.0F, -6.0F)
      );
      partdefinition7.addOrReplaceChild(
         "right_wing_tip",
         CubeListBuilder.create().addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144),
         PartPose.offset(-56.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition8 = partdefinition12.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104),
         PartPose.offsetAndRotation(-12.0F, 17.0F, -6.0F, 1.3F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition9 = partdefinition8.addOrReplaceChild(
         "right_front_leg_tip",
         CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138),
         PartPose.offsetAndRotation(0.0F, 20.0F, -1.0F, -0.5F, 0.0F, 0.0F)
      );
      partdefinition9.addOrReplaceChild(
         "right_front_foot",
         CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104),
         PartPose.offsetAndRotation(0.0F, 23.0F, 0.0F, 0.75F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition10 = partdefinition12.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0),
         PartPose.offsetAndRotation(-16.0F, 13.0F, 34.0F, 1.0F, 0.0F, 0.0F)
      );
      PartDefinition partdefinition11 = partdefinition10.addOrReplaceChild(
         "right_hind_leg_tip",
         CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0),
         PartPose.offsetAndRotation(0.0F, 32.0F, -4.0F, 0.5F, 0.0F, 0.0F)
      );
      partdefinition11.addOrReplaceChild(
         "right_hind_foot",
         CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0),
         PartPose.offsetAndRotation(0.0F, 31.0F, 4.0F, 0.75F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 256, 256);
   }

   public void setupAnim(EnderDragonRenderState p_460831_) {
      super.setupAnim(p_460831_);
      float f = p_460831_.flapTime * (float) (Math.PI * 2);
      this.jaw.xRot = (Mth.sin(f) + 1.0F) * 0.2F;
      float f1 = Mth.sin(f - 1.0F) + 1.0F;
      f1 = (f1 * f1 + f1 * 2.0F) * 0.05F;
      this.root.y = (f1 - 2.0F) * 16.0F;
      this.root.z = -48.0F;
      this.root.xRot = f1 * 2.0F * (float) (Math.PI / 180.0);
      float f2 = this.neckParts[0].x;
      float f3 = this.neckParts[0].y;
      float f4 = this.neckParts[0].z;
      float f5 = 1.5F;
      DragonFlightHistory.Sample dragonflighthistory$sample = p_460831_.getHistoricalPos(6);
      float f6 = Mth.wrapDegrees(p_460831_.getHistoricalPos(5).yRot() - p_460831_.getHistoricalPos(10).yRot());
      float f7 = Mth.wrapDegrees(p_460831_.getHistoricalPos(5).yRot() + f6 / 2.0F);

      for (int i = 0; i < 5; i++) {
         ModelPart modelpart = this.neckParts[i];
         DragonFlightHistory.Sample dragonflighthistory$sample1 = p_460831_.getHistoricalPos(5 - i);
         float f8 = Mth.cos(i * 0.45F + f) * 0.15F;
         modelpart.yRot = Mth.wrapDegrees(dragonflighthistory$sample1.yRot() - dragonflighthistory$sample.yRot()) * (float) (Math.PI / 180.0) * 1.5F;
         modelpart.xRot = f8
            + p_460831_.getHeadPartYOffset(i, dragonflighthistory$sample, dragonflighthistory$sample1) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
         modelpart.zRot = -Mth.wrapDegrees(dragonflighthistory$sample1.yRot() - f7) * (float) (Math.PI / 180.0) * 1.5F;
         modelpart.y = f3;
         modelpart.z = f4;
         modelpart.x = f2;
         f2 -= Mth.sin(modelpart.yRot) * Mth.cos(modelpart.xRot) * 10.0F;
         f3 += Mth.sin(modelpart.xRot) * 10.0F;
         f4 -= Mth.cos(modelpart.yRot) * Mth.cos(modelpart.xRot) * 10.0F;
      }

      this.head.y = f3;
      this.head.z = f4;
      this.head.x = f2;
      DragonFlightHistory.Sample dragonflighthistory$sample2 = p_460831_.getHistoricalPos(0);
      this.head.yRot = Mth.wrapDegrees(dragonflighthistory$sample2.yRot() - dragonflighthistory$sample.yRot()) * (float) (Math.PI / 180.0);
      this.head.xRot = Mth.wrapDegrees(p_460831_.getHeadPartYOffset(6, dragonflighthistory$sample, dragonflighthistory$sample2))
         * (float) (Math.PI / 180.0)
         * 1.5F
         * 5.0F;
      this.head.zRot = -Mth.wrapDegrees(dragonflighthistory$sample2.yRot() - f7) * (float) (Math.PI / 180.0);
      this.body.zRot = -f6 * 1.5F * (float) (Math.PI / 180.0);
      this.leftWing.xRot = 0.125F - Mth.cos(f) * 0.2F;
      this.leftWing.yRot = -0.25F;
      this.leftWing.zRot = -(Mth.sin(f) + 0.125F) * 0.8F;
      this.leftWingTip.zRot = (Mth.sin(f + 2.0F) + 0.5F) * 0.75F;
      this.rightWing.xRot = this.leftWing.xRot;
      this.rightWing.yRot = -this.leftWing.yRot;
      this.rightWing.zRot = -this.leftWing.zRot;
      this.rightWingTip.zRot = -this.leftWingTip.zRot;
      this.poseLimbs(f1, this.leftFrontLeg, this.leftFrontLegTip, this.leftFrontFoot, this.leftRearLeg, this.leftRearLegTip, this.leftRearFoot);
      this.poseLimbs(f1, this.rightFrontLeg, this.rightFrontLegTip, this.rightFrontFoot, this.rightRearLeg, this.rightRearLegTip, this.rightRearFoot);
      float f9 = 0.0F;
      f3 = this.tailParts[0].y;
      f4 = this.tailParts[0].z;
      f2 = this.tailParts[0].x;
      dragonflighthistory$sample = p_460831_.getHistoricalPos(11);

      for (int j = 0; j < 12; j++) {
         DragonFlightHistory.Sample dragonflighthistory$sample3 = p_460831_.getHistoricalPos(12 + j);
         f9 += Mth.sin(j * 0.45F + f) * 0.05F;
         ModelPart modelpart1 = this.tailParts[j];
         modelpart1.yRot = (Mth.wrapDegrees(dragonflighthistory$sample3.yRot() - dragonflighthistory$sample.yRot()) * 1.5F + 180.0F)
            * (float) (Math.PI / 180.0);
         modelpart1.xRot = f9 + (float)(dragonflighthistory$sample3.y() - dragonflighthistory$sample.y()) * (float) (Math.PI / 180.0) * 1.5F * 5.0F;
         modelpart1.zRot = Mth.wrapDegrees(dragonflighthistory$sample3.yRot() - f7) * (float) (Math.PI / 180.0) * 1.5F;
         modelpart1.y = f3;
         modelpart1.z = f4;
         modelpart1.x = f2;
         f3 += Mth.sin(modelpart1.xRot) * 10.0F;
         f4 -= Mth.cos(modelpart1.yRot) * Mth.cos(modelpart1.xRot) * 10.0F;
         f2 -= Mth.sin(modelpart1.yRot) * Mth.cos(modelpart1.xRot) * 10.0F;
      }
   }

   private void poseLimbs(
      float p_453013_, ModelPart p_453249_, ModelPart p_459312_, ModelPart p_452556_, ModelPart p_460000_, ModelPart p_455087_, ModelPart p_456037_
   ) {
      p_460000_.xRot = 1.0F + p_453013_ * 0.1F;
      p_455087_.xRot = 0.5F + p_453013_ * 0.1F;
      p_456037_.xRot = 0.75F + p_453013_ * 0.1F;
      p_453249_.xRot = 1.3F + p_453013_ * 0.1F;
      p_459312_.xRot = -0.5F - p_453013_ * 0.1F;
      p_452556_.xRot = 0.75F + p_453013_ * 0.1F;
   }
}
