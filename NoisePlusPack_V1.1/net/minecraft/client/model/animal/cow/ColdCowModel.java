package net.minecraft.client.model.animal.cow;

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
public class ColdCowModel extends CowModel {
   public ColdCowModel(ModelPart p_461034_) {
      super(p_461034_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = createBaseCowModel();
      meshdefinition.getRoot()
         .addOrReplaceChild(
            "body",
            CubeListBuilder.create()
               .texOffs(20, 32)
               .addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F, new CubeDeformation(0.5F))
               .texOffs(18, 4)
               .addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F)
               .texOffs(52, 0)
               .addBox(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
         );
      PartDefinition partdefinition = meshdefinition.getRoot()
         .addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F).texOffs(9, 33).addBox(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F),
            PartPose.offset(0.0F, 4.0F, -8.0F)
         );
      partdefinition.addOrReplaceChild(
         "right_horn",
         CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, -4.5F, -0.5F, 2.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(-4.5F, -2.5F, -3.5F, 1.5708F, 0.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_horn",
         CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, -3.0F, -0.5F, 2.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(5.5F, -2.5F, -5.0F, 1.5708F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }
}
