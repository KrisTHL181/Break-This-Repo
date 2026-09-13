package net.minecraft.client.model.animal.cow;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CowModel extends QuadrupedModel<LivingEntityRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(false, 8.0F, 6.0F, Set.of("head"));
   private static final int LEG_SIZE = 12;

   public CowModel(ModelPart p_460757_) {
      super(p_460757_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = createBaseCowModel();
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   static MeshDefinition createBaseCowModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
            .texOffs(1, 33)
            .addBox(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F)
            .texOffs(22, 0)
            .addBox("right_horn", -5.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F)
            .texOffs(22, 0)
            .addBox("left_horn", 4.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F),
         PartPose.offset(0.0F, 4.0F, -8.0F)
      );
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F).texOffs(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F),
         PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      CubeListBuilder cubelistbuilder1 = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      partdefinition.addOrReplaceChild("right_hind_leg", cubelistbuilder1, PartPose.offset(-4.0F, 12.0F, 7.0F));
      partdefinition.addOrReplaceChild("left_hind_leg", cubelistbuilder, PartPose.offset(4.0F, 12.0F, 7.0F));
      partdefinition.addOrReplaceChild("right_front_leg", cubelistbuilder1, PartPose.offset(-4.0F, 12.0F, -5.0F));
      partdefinition.addOrReplaceChild("left_front_leg", cubelistbuilder, PartPose.offset(4.0F, 12.0F, -5.0F));
      return meshdefinition;
   }

   public ModelPart getHead() {
      return this.head;
   }
}
