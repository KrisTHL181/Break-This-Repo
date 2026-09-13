package net.minecraft.client.model.animal.pig;

import java.util.Set;
import net.minecraft.client.model.BabyModelTransform;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PigModel extends QuadrupedModel<LivingEntityRenderState> {
   public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(false, 4.0F, 4.0F, Set.of("head"));

   public PigModel(ModelPart p_452821_) {
      super(p_452821_);
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_455472_) {
      return LayerDefinition.create(createBasePigModel(p_455472_), 64, 64);
   }

   protected static MeshDefinition createBasePigModel(CubeDeformation p_453849_) {
      MeshDefinition meshdefinition = QuadrupedModel.createBodyMesh(6, true, false, p_453849_);
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, p_453849_)
            .texOffs(16, 16)
            .addBox(-2.0F, 0.0F, -9.0F, 4.0F, 3.0F, 1.0F, p_453849_),
         PartPose.offset(0.0F, 12.0F, -6.0F)
      );
      return meshdefinition;
   }
}
