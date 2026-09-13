package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EquineSaddleModel extends AbstractEquineModel<EquineRenderState> {
   private static final String SADDLE = "saddle";
   private static final String LEFT_SADDLE_MOUTH = "left_saddle_mouth";
   private static final String LEFT_SADDLE_LINE = "left_saddle_line";
   private static final String RIGHT_SADDLE_MOUTH = "right_saddle_mouth";
   private static final String RIGHT_SADDLE_LINE = "right_saddle_line";
   private static final String HEAD_SADDLE = "head_saddle";
   private static final String MOUTH_SADDLE_WRAP = "mouth_saddle_wrap";
   private final ModelPart[] ridingParts;

   public EquineSaddleModel(ModelPart p_455474_) {
      super(p_455474_);
      ModelPart modelpart = this.headParts.getChild("left_saddle_line");
      ModelPart modelpart1 = this.headParts.getChild("right_saddle_line");
      this.ridingParts = new ModelPart[]{modelpart, modelpart1};
   }

   public static LayerDefinition createSaddleLayer(boolean p_457578_) {
      return createFullScaleSaddleLayer(p_457578_).apply(p_457578_ ? BABY_TRANSFORMER : MeshTransformer.IDENTITY);
   }

   public static LayerDefinition createFullScaleSaddleLayer(boolean p_459864_) {
      MeshDefinition meshdefinition = p_459864_ ? createFullScaleBabyMesh(CubeDeformation.NONE) : createBodyMesh(CubeDeformation.NONE);
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.getChild("body");
      PartDefinition partdefinition2 = partdefinition.getChild("head_parts");
      partdefinition1.addOrReplaceChild(
         "saddle", CubeListBuilder.create().texOffs(26, 0).addBox(-5.0F, -8.0F, -9.0F, 10.0F, 9.0F, 9.0F, new CubeDeformation(0.5F)), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "left_saddle_mouth", CubeListBuilder.create().texOffs(29, 5).addBox(2.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "right_saddle_mouth", CubeListBuilder.create().texOffs(29, 5).addBox(-3.0F, -9.0F, -6.0F, 1.0F, 2.0F, 2.0F), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "left_saddle_line",
         CubeListBuilder.create().texOffs(32, 2).addBox(3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
         PartPose.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "right_saddle_line",
         CubeListBuilder.create().texOffs(32, 2).addBox(-3.1F, -6.0F, -8.0F, 0.0F, 3.0F, 16.0F),
         PartPose.rotation((float) (-Math.PI / 6), 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "head_saddle", CubeListBuilder.create().texOffs(1, 1).addBox(-3.0F, -11.0F, -1.9F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.22F)), PartPose.ZERO
      );
      partdefinition2.addOrReplaceChild(
         "mouth_saddle_wrap", CubeListBuilder.create().texOffs(19, 0).addBox(-2.0F, -11.0F, -4.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.2F)), PartPose.ZERO
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   @Override
   public void setupAnim(EquineRenderState p_459595_) {
      super.setupAnim(p_459595_);

      for (ModelPart modelpart : this.ridingParts) {
         modelpart.visible = p_459595_.isRidden;
      }
   }
}
