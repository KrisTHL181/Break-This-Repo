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

public class BannerFlagModel extends Model<Float> {
   private final ModelPart flag;

   public BannerFlagModel(final ModelPart root) {
      super(root, RenderTypes::entitySolid);
      this.flag = root.getChild("flag");
   }

   public static LayerDefinition createFlagLayer(final boolean standing) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "flag",
         CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F),
         PartPose.offset(0.0F, standing ? -44.0F : -20.5F, standing ? 0.0F : 10.5F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final Float phase) {
      super.setupAnim(phase);
      this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * phase)) * (float) Math.PI;
   }
}
