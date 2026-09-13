package net.minecraft.client.model.monster.slime;

import java.util.Arrays;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeModel extends EntityModel<SlimeRenderState> {
   private static final int SEGMENT_COUNT = 8;
   private final ModelPart[] bodyCubes = new ModelPart[8];

   public MagmaCubeModel(ModelPart p_457696_) {
      super(p_457696_);
      Arrays.setAll(this.bodyCubes, p_460541_ -> p_457696_.getChild(getSegmentName(p_460541_)));
   }

   private static String getSegmentName(int p_458371_) {
      return "cube" + p_458371_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();

      for (int i = 0; i < 8; i++) {
         int j = 0;
         int k = 0;
         if (i > 0 && i < 4) {
            k += 9 * i;
         } else if (i > 3) {
            j = 32;
            k += 9 * i - 36;
         }

         partdefinition.addOrReplaceChild(
            getSegmentName(i), CubeListBuilder.create().texOffs(j, k).addBox(-4.0F, 16 + i, -4.0F, 8.0F, 1.0F, 8.0F), PartPose.ZERO
         );
      }

      partdefinition.addOrReplaceChild("inside_cube", CubeListBuilder.create().texOffs(24, 40).addBox(-2.0F, 18.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 64);
   }

   public void setupAnim(SlimeRenderState p_456128_) {
      super.setupAnim(p_456128_);
      float f = Math.max(0.0F, p_456128_.squish);

      for (int i = 0; i < this.bodyCubes.length; i++) {
         this.bodyCubes[i].y = -(4 - i) * f * 1.7F;
      }
   }
}
