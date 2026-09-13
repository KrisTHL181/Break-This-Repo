package net.minecraft.client.model.object.boat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaftModel extends AbstractBoatModel {
   public RaftModel(ModelPart p_454197_) {
      super(p_454197_);
   }

   private static void addCommonParts(PartDefinition p_460837_) {
      p_460837_.addOrReplaceChild(
         "bottom",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-14.0F, -11.0F, -4.0F, 28.0F, 20.0F, 4.0F)
            .texOffs(0, 0)
            .addBox(-14.0F, -9.0F, -8.0F, 28.0F, 16.0F, 4.0F),
         PartPose.offsetAndRotation(0.0F, -2.1F, 1.0F, 1.5708F, 0.0F, 0.0F)
      );
      int i = 20;
      int j = 7;
      int k = 6;
      float f = -5.0F;
      p_460837_.addOrReplaceChild(
         "left_paddle",
         CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(-1.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -4.0F, 9.0F, 0.0F, 0.0F, (float) (Math.PI / 16))
      );
      p_460837_.addOrReplaceChild(
         "right_paddle",
         CubeListBuilder.create().texOffs(40, 24).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(0.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -4.0F, -9.0F, 0.0F, (float) Math.PI, (float) (Math.PI / 16))
      );
   }

   public static LayerDefinition createRaftModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      addCommonParts(partdefinition);
      return LayerDefinition.create(meshdefinition, 128, 64);
   }

   public static LayerDefinition createChestRaftModel() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      addCommonParts(partdefinition);
      partdefinition.addOrReplaceChild(
         "chest_bottom",
         CubeListBuilder.create().texOffs(0, 76).addBox(0.0F, 0.0F, 0.0F, 12.0F, 8.0F, 12.0F),
         PartPose.offsetAndRotation(-2.0F, -10.1F, -6.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "chest_lid",
         CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, 0.0F, 0.0F, 12.0F, 4.0F, 12.0F),
         PartPose.offsetAndRotation(-2.0F, -14.1F, -6.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "chest_lock",
         CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 1.0F),
         PartPose.offsetAndRotation(-1.0F, -11.1F, -1.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }
}
