package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AbstractPiglinModel<S extends HumanoidRenderState> extends HumanoidModel<S> {
   private static final String LEFT_SLEEVE = "left_sleeve";
   private static final String RIGHT_SLEEVE = "right_sleeve";
   private static final String LEFT_PANTS = "left_pants";
   private static final String RIGHT_PANTS = "right_pants";
   public final ModelPart leftSleeve = this.leftArm.getChild("left_sleeve");
   public final ModelPart rightSleeve = this.rightArm.getChild("right_sleeve");
   public final ModelPart leftPants = this.leftLeg.getChild("left_pants");
   public final ModelPart rightPants = this.rightLeg.getChild("right_pants");
   public final ModelPart jacket = this.body.getChild("jacket");
   public final ModelPart rightEar = this.head.getChild("right_ear");
   public final ModelPart leftEar = this.head.getChild("left_ear");

   public AbstractPiglinModel(ModelPart p_451902_) {
      super(p_451902_, RenderTypes::entityTranslucent);
   }

   public static MeshDefinition createMesh(CubeDeformation p_457104_) {
      MeshDefinition meshdefinition = PlayerModel.createMesh(p_457104_, false);
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, p_457104_), PartPose.ZERO);
      PartDefinition partdefinition1 = addHead(p_457104_, meshdefinition);
      partdefinition1.clearChild("hat");
      return meshdefinition;
   }

   public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation p_455920_, CubeDeformation p_453678_) {
      return PlayerModel.createArmorMeshSet(p_455920_, p_453678_).map(p_457046_ -> {
         PartDefinition partdefinition = p_457046_.getRoot();
         PartDefinition partdefinition1 = partdefinition.getChild("head");
         partdefinition1.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.ZERO);
         partdefinition1.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.ZERO);
         return (MeshDefinition)p_457046_;
      });
   }

   public static PartDefinition addHead(CubeDeformation p_450175_, MeshDefinition p_451221_) {
      PartDefinition partdefinition = p_451221_.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, p_450175_)
            .texOffs(31, 1)
            .addBox(-2.0F, -4.0F, -5.0F, 4.0F, 4.0F, 1.0F, p_450175_)
            .texOffs(2, 4)
            .addBox(2.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F, p_450175_)
            .texOffs(2, 0)
            .addBox(-3.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F, p_450175_),
         PartPose.ZERO
      );
      partdefinition1.addOrReplaceChild(
         "left_ear",
         CubeListBuilder.create().texOffs(51, 6).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, p_450175_),
         PartPose.offsetAndRotation(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 6))
      );
      partdefinition1.addOrReplaceChild(
         "right_ear",
         CubeListBuilder.create().texOffs(39, 6).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, p_450175_),
         PartPose.offsetAndRotation(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 6))
      );
      return partdefinition1;
   }

   @Override
   public void setupAnim(S p_455223_) {
      super.setupAnim(p_455223_);
      float f = p_455223_.walkAnimationPos;
      float f1 = p_455223_.walkAnimationSpeed;
      float f2 = (float) (Math.PI / 6);
      float f3 = p_455223_.ageInTicks * 0.1F + f * 0.5F;
      float f4 = 0.08F + f1 * 0.4F;
      this.leftEar.zRot = (float) (-Math.PI / 6) - Mth.cos(f3 * 1.2F) * f4;
      this.rightEar.zRot = (float) (Math.PI / 6) + Mth.cos(f3) * f4;
   }

   @Override
   public void setAllVisible(boolean p_458653_) {
      super.setAllVisible(p_458653_);
      this.leftSleeve.visible = p_458653_;
      this.rightSleeve.visible = p_458653_;
      this.leftPants.visible = p_458653_;
      this.rightPants.visible = p_458653_;
      this.jacket.visible = p_458653_;
   }
}
