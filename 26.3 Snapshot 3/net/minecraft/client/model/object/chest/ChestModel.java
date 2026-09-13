package net.minecraft.client.model.object.chest;

import java.util.Set;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

public class ChestModel extends Model<Float> {
   private static final String BOTTOM = "bottom";
   private static final String LID = "lid";
   private static final String LOCK = "lock";
   private final ModelPart lid;
   private final ModelPart lock;

   public ChestModel(final ModelPart root) {
      super(root, RenderTypes::entityCutoutCull);
      this.lid = root.getChild("lid");
      this.lock = root.getChild("lock");
   }

   public static LayerDefinition createSingleBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F), PartPose.ZERO);
      root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 9.0F, 1.0F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createDoubleBodyRightLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      Set<Direction> visibleFaces = Util.allOfEnumExcept(Direction.EAST);
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(1.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, visibleFaces), PartPose.ZERO);
      root.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, visibleFaces), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      root.addOrReplaceChild(
         "lock", CubeListBuilder.create().texOffs(0, 0).addBox(15.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, visibleFaces), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createDoubleBodyLeftLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      Set<Direction> visibleFaces = Util.allOfEnumExcept(Direction.WEST);
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, 0.0F, 1.0F, 15.0F, 10.0F, 14.0F, visibleFaces), PartPose.ZERO);
      root.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 15.0F, 5.0F, 14.0F, visibleFaces), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      root.addOrReplaceChild(
         "lock", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.0F, 14.0F, 1.0F, 4.0F, 1.0F, visibleFaces), PartPose.offset(0.0F, 9.0F, 1.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final Float open) {
      super.setupAnim(open);
      this.lid.xRot = -(open * (float) (Math.PI / 2));
      this.lock.xRot = this.lid.xRot;
   }
}
