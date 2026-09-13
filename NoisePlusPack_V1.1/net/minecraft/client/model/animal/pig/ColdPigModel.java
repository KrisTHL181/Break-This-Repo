package net.minecraft.client.model.animal.pig;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColdPigModel extends PigModel {
   public ColdPigModel(ModelPart p_460574_) {
      super(p_460574_);
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_453657_) {
      MeshDefinition meshdefinition = createBasePigModel(p_453657_);
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(28, 8)
            .addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F)
            .texOffs(28, 32)
            .addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, new CubeDeformation(0.5F)),
         PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }
}
