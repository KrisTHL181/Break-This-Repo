package net.minecraft.client.model.animal.fish;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishSmallModel extends EntityModel<TropicalFishRenderState> {
   private final ModelPart tail;

   public TropicalFishSmallModel(ModelPart p_458782_) {
      super(p_458782_);
      this.tail = p_458782_.getChild("tail");
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_459322_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 22;
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -3.0F, 2.0F, 3.0F, 6.0F, p_459322_), PartPose.offset(0.0F, 22.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(22, -6).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 6.0F, p_459322_), PartPose.offset(0.0F, 22.0F, 3.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(2, 16).addBox(-2.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, p_459322_),
         PartPose.offsetAndRotation(-1.0F, 22.5F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(2, 12).addBox(0.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, p_459322_),
         PartPose.offsetAndRotation(1.0F, 22.5F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "top_fin", CubeListBuilder.create().texOffs(10, -5).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 6.0F, p_459322_), PartPose.offset(0.0F, 20.5F, -3.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(TropicalFishRenderState p_455116_) {
      super.setupAnim(p_455116_);
      float f = p_455116_.isInWater ? 1.0F : 1.5F;
      this.tail.yRot = -f * 0.45F * Mth.sin(0.6F * p_455116_.ageInTicks);
   }
}
