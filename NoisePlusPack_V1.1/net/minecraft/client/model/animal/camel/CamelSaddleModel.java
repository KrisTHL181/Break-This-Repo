package net.minecraft.client.model.animal.camel;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.CamelRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CamelSaddleModel extends CamelModel {
   private static final String SADDLE = "saddle";
   private static final String BRIDLE = "bridle";
   private static final String REINS = "reins";
   private final ModelPart reins = this.head.getChild("reins");

   public CamelSaddleModel(ModelPart p_459665_) {
      super(p_459665_);
   }

   public static LayerDefinition createSaddleLayer() {
      MeshDefinition meshdefinition = createBodyMesh();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.getChild("body");
      PartDefinition partdefinition2 = partdefinition1.getChild("head");
      CubeDeformation cubedeformation = new CubeDeformation(0.05F);
      partdefinition1.addOrReplaceChild(
         "saddle",
         CubeListBuilder.create()
            .texOffs(74, 64)
            .addBox(-4.5F, -17.0F, -15.5F, 9.0F, 5.0F, 11.0F, cubedeformation)
            .texOffs(92, 114)
            .addBox(-3.5F, -20.0F, -15.5F, 7.0F, 3.0F, 11.0F, cubedeformation)
            .texOffs(0, 89)
            .addBox(-7.5F, -12.0F, -23.5F, 15.0F, 12.0F, 27.0F, cubedeformation),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "reins",
         CubeListBuilder.create()
            .texOffs(98, 42)
            .addBox(3.51F, -18.0F, -17.0F, 0.0F, 7.0F, 15.0F)
            .texOffs(84, 57)
            .addBox(-3.5F, -18.0F, -2.0F, 7.0F, 7.0F, 0.0F)
            .texOffs(98, 42)
            .addBox(-3.51F, -18.0F, -17.0F, 0.0F, 7.0F, 15.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      partdefinition2.addOrReplaceChild(
         "bridle",
         CubeListBuilder.create()
            .texOffs(60, 87)
            .addBox(-3.5F, -7.0F, -15.0F, 7.0F, 8.0F, 19.0F, cubedeformation)
            .texOffs(21, 64)
            .addBox(-3.5F, -21.0F, -15.0F, 7.0F, 14.0F, 7.0F, cubedeformation)
            .texOffs(50, 64)
            .addBox(-2.5F, -21.0F, -21.0F, 5.0F, 5.0F, 6.0F, cubedeformation)
            .texOffs(74, 70)
            .addBox(2.5F, -19.0F, -18.0F, 1.0F, 2.0F, 2.0F)
            .texOffs(74, 70)
            .mirror()
            .addBox(-3.5F, -19.0F, -18.0F, 1.0F, 2.0F, 2.0F),
         PartPose.offset(0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(meshdefinition, 128, 128);
   }

   @Override
   public void setupAnim(CamelRenderState p_460712_) {
      super.setupAnim(p_460712_);
      this.reins.visible = p_460712_.isRidden;
   }
}
