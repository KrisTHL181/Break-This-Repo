package net.minecraft.client.model.object.boat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BoatModel extends AbstractBoatModel {
   private static final int BOTTOM_WIDTH = 28;
   private static final int WIDTH = 32;
   private static final int DEPTH = 6;
   private static final int LENGTH = 20;
   private static final int Y_OFFSET = 4;
   private static final String WATER_PATCH = "water_patch";
   private static final String BACK = "back";
   private static final String FRONT = "front";
   private static final String RIGHT = "right";
   private static final String LEFT = "left";

   public BoatModel(final ModelPart root) {
      super(root);
   }

   private static void addCommonParts(final PartDefinition root) {
      int halfWidth = 16;
      int halfBottomWidth = 14;
      int halfLength = 10;
      root.addOrReplaceChild(
         "bottom",
         CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -9.0F, -3.0F, 28.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 3.0F, 1.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "back",
         CubeListBuilder.create().texOffs(0, 19).addBox(-13.0F, -7.0F, -1.0F, 18.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(-15.0F, 4.0F, 4.0F, 0.0F, (float) (Math.PI * 3.0 / 2.0), 0.0F)
      );
      root.addOrReplaceChild(
         "front",
         CubeListBuilder.create().texOffs(0, 27).addBox(-8.0F, -7.0F, -1.0F, 16.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(15.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      root.addOrReplaceChild(
         "right",
         CubeListBuilder.create().texOffs(0, 35).addBox(-14.0F, -7.0F, -1.0F, 28.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 4.0F, -9.0F, 0.0F, (float) Math.PI, 0.0F)
      );
      root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 43).addBox(-14.0F, -7.0F, -1.0F, 28.0F, 6.0F, 2.0F), PartPose.offset(0.0F, 4.0F, 9.0F));
      int totalLength = 20;
      int bladeLength = 7;
      int bladeWidth = 6;
      float pivot = -5.0F;
      root.addOrReplaceChild(
         "left_paddle",
         CubeListBuilder.create().texOffs(62, 0).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(-1.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -5.0F, 9.0F, 0.0F, 0.0F, (float) (Math.PI / 16))
      );
      root.addOrReplaceChild(
         "right_paddle",
         CubeListBuilder.create().texOffs(62, 20).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F).addBox(0.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
         PartPose.offsetAndRotation(3.0F, -5.0F, -9.0F, 0.0F, (float) Math.PI, (float) (Math.PI / 16))
      );
   }

   public static LayerDefinition createBoatModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      addCommonParts(root);
      return LayerDefinition.create(mesh, 128, 64);
   }

   public static LayerDefinition createChestBoatModel() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      addCommonParts(root);
      root.addOrReplaceChild(
         "chest_bottom",
         CubeListBuilder.create().texOffs(0, 76).addBox(0.0F, 0.0F, 0.0F, 12.0F, 8.0F, 12.0F),
         PartPose.offsetAndRotation(-2.0F, -5.0F, -6.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      root.addOrReplaceChild(
         "chest_lid",
         CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, 0.0F, 0.0F, 12.0F, 4.0F, 12.0F),
         PartPose.offsetAndRotation(-2.0F, -9.0F, -6.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      root.addOrReplaceChild(
         "chest_lock",
         CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 1.0F),
         PartPose.offsetAndRotation(-1.0F, -6.0F, -1.0F, 0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      return LayerDefinition.create(mesh, 128, 128);
   }

   public static LayerDefinition createWaterPatch() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "water_patch",
         CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -9.0F, -3.0F, 28.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 0, 0);
   }
}
