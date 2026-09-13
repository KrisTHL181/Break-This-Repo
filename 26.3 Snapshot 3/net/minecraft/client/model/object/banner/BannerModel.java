package net.minecraft.client.model.object.banner;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class BannerModel extends Model<Unit> {
   public static final int BANNER_WIDTH = 20;
   public static final int BANNER_HEIGHT = 40;
   public static final String FLAG = "flag";
   private static final String POLE = "pole";
   private static final String BAR = "bar";

   public BannerModel(final ModelPart root) {
      super(root, RenderTypes::entitySolid);
   }

   public static LayerDefinition createBodyLayer(final boolean standing) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      if (standing) {
         root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -42.0F, -1.0F, 2.0F, 42.0F, 2.0F), PartPose.ZERO);
      }

      root.addOrReplaceChild(
         "bar", CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, standing ? -44.0F : -20.5F, standing ? -1.0F : 9.5F, 20.0F, 2.0F, 2.0F), PartPose.ZERO
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
