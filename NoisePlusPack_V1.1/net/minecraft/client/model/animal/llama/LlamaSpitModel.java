package net.minecraft.client.model.animal.llama;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaSpitModel extends EntityModel<EntityRenderState> {
   private static final String MAIN = "main";

   public LlamaSpitModel(ModelPart p_460271_) {
      super(p_460271_);
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 2;
      partdefinition.addOrReplaceChild(
         "main",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, -4.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 2.0F, 0.0F, 2.0F, 2.0F, 2.0F)
            .addBox(0.0F, 0.0F, 2.0F, 2.0F, 2.0F, 2.0F),
         PartPose.ZERO
      );
      return LayerDefinition.create(meshdefinition, 64, 32);
   }
}
