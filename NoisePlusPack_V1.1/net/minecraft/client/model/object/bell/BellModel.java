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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BellModel extends Model<BellModel.State> {
   private static final String BELL_BODY = "bell_body";
   private final ModelPart bellBody;

   public BellModel(ModelPart p_456602_) {
      super(p_456602_, RenderTypes::entitySolid);
      this.bellBody = p_456602_.getChild("bell_body");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
         "bell_body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 7.0F, 6.0F), PartPose.offset(8.0F, 12.0F, 8.0F)
      );
      partdefinition1.addOrReplaceChild(
         "bell_base", CubeListBuilder.create().texOffs(0, 13).addBox(4.0F, 4.0F, 4.0F, 8.0F, 2.0F, 8.0F), PartPose.offset(-8.0F, -12.0F, -8.0F)
      );
      return LayerDefinition.create(meshdefinition, 32, 32);
   }

   public void setupAnim(BellModel.State p_457052_) {
      super.setupAnim(p_457052_);
      float f = 0.0F;
      float f1 = 0.0F;
      if (p_457052_.shakeDirection != null) {
         float f2 = Mth.sin(p_457052_.ticks / (float) Math.PI) / (4.0F + p_457052_.ticks / 3.0F);
         switch (p_457052_.shakeDirection) {
            case NORTH:
               f = -f2;
               break;
            case SOUTH:
               f = f2;
               break;
            case EAST:
               f1 = -f2;
               break;
            case WEST:
               f1 = f2;
         }
      }

      this.bellBody.xRot = f;
      this.bellBody.zRot = f1;
   }

   @OnlyIn(Dist.CLIENT)
   public record State(float ticks, @Nullable Direction shakeDirection) {
   }
}
