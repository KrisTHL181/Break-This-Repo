package net.minecraft.client.model.object.banner;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerFlagModel extends Model<Float> {
   private final ModelPart flag;

   public BannerFlagModel(ModelPart p_457322_) {
      super(p_457322_, RenderTypes::entitySolid);
      this.flag = p_457322_.getChild("flag");
   }

   public static LayerDefinition createFlagLayer(boolean p_456747_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "flag",
         CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F),
         PartPose.offset(0.0F, p_456747_ ? -44.0F : -20.5F, p_456747_ ? 0.0F : 10.5F)
      );
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(Float p_457513_) {
      super.setupAnim(p_457513_);
      this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * p_457513_)) * (float) Math.PI;
   }
}
