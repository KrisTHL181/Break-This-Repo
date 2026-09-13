package net.minecraft.client.model.animal.llama;

import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaModel extends EntityModel<LlamaRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = LlamaModel::transformToBaby;
   private final ModelPart head;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart rightChest;
   private final ModelPart leftChest;

   public LlamaModel(ModelPart p_459572_) {
      super(p_459572_);
      this.head = p_459572_.getChild("head");
      this.rightChest = p_459572_.getChild("right_chest");
      this.leftChest = p_459572_.getChild("left_chest");
      this.rightHindLeg = p_459572_.getChild("right_hind_leg");
      this.leftHindLeg = p_459572_.getChild("left_hind_leg");
      this.rightFrontLeg = p_459572_.getChild("right_front_leg");
      this.leftFrontLeg = p_459572_.getChild("left_front_leg");
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_460566_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, -14.0F, -10.0F, 4.0F, 4.0F, 9.0F, p_460566_)
            .texOffs(0, 14)
            .addBox("neck", -4.0F, -16.0F, -6.0F, 8.0F, 18.0F, 6.0F, p_460566_)
            .texOffs(17, 0)
            .addBox("ear", -4.0F, -19.0F, -4.0F, 3.0F, 3.0F, 2.0F, p_460566_)
            .texOffs(17, 0)
            .addBox("ear", 1.0F, -19.0F, -4.0F, 3.0F, 3.0F, 2.0F, p_460566_),
         PartPose.offset(0.0F, 7.0F, -6.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(29, 0).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F, p_460566_),
         PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_chest",
         CubeListBuilder.create().texOffs(45, 28).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, p_460566_),
         PartPose.offsetAndRotation(-8.5F, 3.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_chest",
         CubeListBuilder.create().texOffs(45, 41).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, p_460566_),
         PartPose.offsetAndRotation(5.5F, 3.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      int i = 4;
      int j = 14;
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(29, 29).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F, p_460566_);
      partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder, PartPose.offset(-3.5F, 10.0F, 6.0F));
      partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(3.5F, 10.0F, 6.0F));
      partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder, PartPose.offset(-3.5F, 10.0F, -5.0F));
      partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder, PartPose.offset(3.5F, 10.0F, -5.0F));
      return LayerDefinition.create(meshdefinition, 128, 64);
   }

   private static MeshDefinition transformToBaby(MeshDefinition p_450928_) {
      float f = 2.0F;
      float f1 = 0.7F;
      float f2 = 1.1F;
      UnaryOperator<PartPose> unaryoperator = p_454033_ -> p_454033_.translated(0.0F, 21.0F, 3.52F).scaled(0.71428573F, 0.64935064F, 0.7936508F);
      UnaryOperator<PartPose> unaryoperator1 = p_460032_ -> p_460032_.translated(0.0F, 33.0F, 0.0F).scaled(0.625F, 0.45454544F, 0.45454544F);
      UnaryOperator<PartPose> unaryoperator2 = p_455892_ -> p_455892_.translated(0.0F, 33.0F, 0.0F).scaled(0.45454544F, 0.41322312F, 0.45454544F);
      MeshDefinition meshdefinition = new MeshDefinition();

      for (Entry<String, PartDefinition> entry : p_450928_.getRoot().getChildren()) {
         String s = entry.getKey();
         PartDefinition partdefinition = entry.getValue();

         UnaryOperator<PartPose> unaryoperator3 = switch (s) {
            case "head" -> unaryoperator;
            case "body" -> unaryoperator1;
            default -> unaryoperator2;
         };
         meshdefinition.getRoot().addOrReplaceChild(s, partdefinition.transformed(unaryoperator3));
      }

      return meshdefinition;
   }

   public void setupAnim(LlamaRenderState p_457624_) {
      super.setupAnim(p_457624_);
      this.head.xRot = p_457624_.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = p_457624_.yRot * (float) (Math.PI / 180.0);
      float f = p_457624_.walkAnimationSpeed;
      float f1 = p_457624_.walkAnimationPos;
      this.rightHindLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
      this.leftHindLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.rightFrontLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f;
      this.leftFrontLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f;
      this.rightChest.visible = p_457624_.hasChest;
      this.leftChest.visible = p_457624_.hasChest;
   }
}
