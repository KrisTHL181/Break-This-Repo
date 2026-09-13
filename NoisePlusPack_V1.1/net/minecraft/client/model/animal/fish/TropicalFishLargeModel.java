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
public class TropicalFishLargeModel extends EntityModel<TropicalFishRenderState> {
   private final ModelPart tail;

   public TropicalFishLargeModel(ModelPart p_455506_) {
      super(p_455506_);
      this.tail = p_455506_.getChild("tail");
   }

   public static LayerDefinition createBodyLayer(CubeDeformation p_458106_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      int i = 19;
      partdefinition.addOrReplaceChild(
         "body", CubeListBuilder.create().texOffs(0, 20).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 6.0F, 6.0F, p_458106_), PartPose.offset(0.0F, 19.0F, 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "tail", CubeListBuilder.create().texOffs(21, 16).addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 5.0F, p_458106_), PartPose.offset(0.0F, 19.0F, 3.0F)
      );
      partdefinition.addOrReplaceChild(
         "right_fin",
         CubeListBuilder.create().texOffs(2, 16).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, p_458106_),
         PartPose.offsetAndRotation(-1.0F, 20.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "left_fin",
         CubeListBuilder.create().texOffs(2, 12).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, p_458106_),
         PartPose.offsetAndRotation(1.0F, 20.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
      );
      partdefinition.addOrReplaceChild(
         "top_fin", CubeListBuilder.create().texOffs(20, 11).addBox(0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 6.0F, p_458106_), PartPose.offset(0.0F, 16.0F, -3.0F)
      );
      partdefinition.addOrReplaceChild(
         "bottom_fin", CubeListBuilder.create().texOffs(20, 21).addBox(0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 6.0F, p_458106_), PartPose.offset(0.0F, 22.0F, -3.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(TropicalFishRenderState p_458234_) {
      super.setupAnim(p_458234_);
      float f = p_458234_.isInWater ? 1.0F : 1.5F;
      this.tail.yRot = -f * 0.45F * Mth.sin(0.6F * p_458234_.ageInTicks);
   }
}
