package net.minecraft.client.model.monster.shulker;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public class ShulkerModel extends EntityModel<ShulkerRenderState> {
   public static final String LID = "lid";
   private static final String BASE = "base";
   private final ModelPart lid;
   private final ModelPart head;

   public ShulkerModel(final ModelPart root) {
      super(root, RenderTypes::entityCutoutZOffset);
      this.lid = root.getChild("lid");
      this.head = root.getChild("head");
   }

   private static MeshDefinition createShellMesh() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 12.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "base", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 8.0F, 16.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return mesh;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = createShellMesh();
      mesh.getRoot()
         .addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition createBoxLayer() {
      MeshDefinition mesh = createShellMesh();
      return LayerDefinition.create(mesh, 64, 64);
   }

   public void setupAnim(final ShulkerRenderState state) {
      super.setupAnim(state);
      float bs = (0.5F + state.peekAmount) * (float) Math.PI;
      float q = -1.0F + Mth.sin(bs);
      float extra = 0.0F;
      if (bs > (float) Math.PI) {
         extra = Mth.sin(state.ageInTicks * 0.1F) * 0.7F;
      }

      this.lid.setPos(0.0F, 16.0F + Mth.sin(bs) * 8.0F + extra, 0.0F);
      if (state.peekAmount > 0.3F) {
         this.lid.yRot = q * q * q * q * (float) Math.PI * 0.125F;
      } else {
         this.lid.yRot = 0.0F;
      }

      this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
      this.head.yRot = (state.yHeadRot - 180.0F - state.yBodyRot) * (float) (Math.PI / 180.0);
   }
}
