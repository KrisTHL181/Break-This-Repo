package net.minecraft.client.model.animal.equine;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DonkeyModel extends AbstractEquineModel<DonkeyRenderState> {
   public static final float DONKEY_SCALE = 0.87F;
   public static final float MULE_SCALE = 0.92F;
   private static final MeshTransformer DONKEY_TRANSFORMER = p_454207_ -> {
      modifyMesh(p_454207_.getRoot());
      return p_454207_;
   };
   private final ModelPart leftChest = this.body.getChild("left_chest");
   private final ModelPart rightChest = this.body.getChild("right_chest");

   public DonkeyModel(ModelPart p_452662_) {
      super(p_452662_);
   }

   public static LayerDefinition createBodyLayer(float p_450712_) {
      return LayerDefinition.create(AbstractEquineModel.createBodyMesh(CubeDeformation.NONE), 64, 64)
         .apply(DONKEY_TRANSFORMER)
         .apply(MeshTransformer.scaling(p_450712_));
   }

   public static LayerDefinition createBabyLayer(float p_455735_) {
      return LayerDefinition.create(AbstractEquineModel.createFullScaleBabyMesh(CubeDeformation.NONE), 64, 64)
         .apply(DONKEY_TRANSFORMER)
         .apply(BABY_TRANSFORMER)
         .apply(MeshTransformer.scaling(p_455735_));
   }

   public static LayerDefinition createSaddleLayer(float p_460407_, boolean p_458441_) {
      return EquineSaddleModel.createFullScaleSaddleLayer(p_458441_)
         .apply(DONKEY_TRANSFORMER)
         .apply(p_458441_ ? AbstractEquineModel.BABY_TRANSFORMER : MeshTransformer.IDENTITY)
         .apply(MeshTransformer.scaling(p_460407_));
   }

   private static void modifyMesh(PartDefinition p_456597_) {
      PartDefinition partdefinition = p_456597_.getChild("body");
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(26, 21).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 8.0F, 3.0F);
      partdefinition.addOrReplaceChild("left_chest", cubelistbuilder, PartPose.offsetAndRotation(6.0F, -8.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F));
      partdefinition.addOrReplaceChild("right_chest", cubelistbuilder, PartPose.offsetAndRotation(-6.0F, -8.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
      PartDefinition partdefinition1 = p_456597_.getChild("head_parts").getChild("head");
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, -7.0F, 0.0F, 2.0F, 7.0F, 1.0F);
      partdefinition1.addOrReplaceChild(
         "left_ear", cubelistbuilder1, PartPose.offsetAndRotation(1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (Math.PI / 12))
      );
      partdefinition1.addOrReplaceChild(
         "right_ear", cubelistbuilder1, PartPose.offsetAndRotation(-1.25F, -10.0F, 4.0F, (float) (Math.PI / 12), 0.0F, (float) (-Math.PI / 12))
      );
   }

   public void setupAnim(DonkeyRenderState p_457506_) {
      super.setupAnim(p_457506_);
      this.leftChest.visible = p_457506_.hasChest;
      this.rightChest.visible = p_457506_.hasChest;
   }
}
