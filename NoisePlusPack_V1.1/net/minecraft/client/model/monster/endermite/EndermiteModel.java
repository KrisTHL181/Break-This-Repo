package net.minecraft.client.model.monster.endermite;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndermiteModel extends EntityModel<EntityRenderState> {
   private static final int BODY_COUNT = 4;
   private static final int[][] BODY_SIZES = new int[][]{{4, 3, 2}, {6, 4, 5}, {3, 3, 1}, {1, 2, 1}};
   private static final int[][] BODY_TEXS = new int[][]{{0, 0}, {0, 5}, {0, 14}, {0, 18}};
   private final ModelPart[] bodyParts = new ModelPart[4];

   public EndermiteModel(ModelPart p_456694_) {
      super(p_456694_);

      for (int i = 0; i < 4; i++) {
         this.bodyParts[i] = p_456694_.getChild(createSegmentName(i));
      }
   }

   private static String createSegmentName(int p_456112_) {
      return "segment" + p_456112_;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = -3.5F;

      for (int i = 0; i < 4; i++) {
         partdefinition.addOrReplaceChild(
            createSegmentName(i),
            CubeListBuilder.create()
               .texOffs(BODY_TEXS[i][0], BODY_TEXS[i][1])
               .addBox(BODY_SIZES[i][0] * -0.5F, 0.0F, BODY_SIZES[i][2] * -0.5F, BODY_SIZES[i][0], BODY_SIZES[i][1], BODY_SIZES[i][2]),
            PartPose.offset(0.0F, 24 - BODY_SIZES[i][1], f)
         );
         if (i < 3) {
            f += (BODY_SIZES[i][2] + BODY_SIZES[i + 1][2]) * 0.5F;
         }
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(EntityRenderState p_458841_) {
      super.setupAnim(p_458841_);

      for (int i = 0; i < this.bodyParts.length; i++) {
         this.bodyParts[i].yRot = Mth.cos(p_458841_.ageInTicks * 0.9F + i * 0.15F * (float) Math.PI) * (float) Math.PI * 0.01F * (1 + Math.abs(i - 2));
         this.bodyParts[i].x = Mth.sin(p_458841_.ageInTicks * 0.9F + i * 0.15F * (float) Math.PI) * (float) Math.PI * 0.1F * Math.abs(i - 2);
      }
   }
}
