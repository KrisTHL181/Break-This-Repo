package net.minecraft.client.model.object.bell;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class BellModel extends Model<BellModel.State> {
   private static final String BELL_BODY = "bell_body";
   private final ModelPart bellBody;

   public BellModel(final ModelPart root) {
      super(root, RenderTypes::entitySolid);
      this.bellBody = root.getChild("bell_body");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition bellBody = root.addOrReplaceChild(
         "bell_body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(8.0F, 12.0F, 8.0F)
      );
      bellBody.addOrReplaceChild(
         "bell_base", CubeListBuilder.create().texOffs(0, 13).addBox(4.0F, 4.0F, 4.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(-8.0F, -12.0F, -8.0F)
      );
      return LayerDefinition.create(mesh, 32, 32);
   }

   public void setupAnim(final BellModel.State state) {
      super.setupAnim(state);
      float xRot = 0.0F;
      float zRot = 0.0F;
      if (state.shakeDirection != null) {
         float baseRot = Mth.sin(state.ticks / (float) Math.PI) / (4.0F + state.ticks / 3.0F);
         switch (state.shakeDirection) {
            case NORTH:
               xRot = -baseRot;
               break;
            case SOUTH:
               xRot = baseRot;
               break;
            case EAST:
               zRot = -baseRot;
               break;
            case WEST:
               zRot = baseRot;
         }
      }

      this.bellBody.xRot = xRot;
      this.bellBody.zRot = zRot;
   }

   public record State(float ticks, @Nullable Direction shakeDirection) {
   }
}
