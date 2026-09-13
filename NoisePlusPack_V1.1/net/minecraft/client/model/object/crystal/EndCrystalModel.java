package net.minecraft.client.model.object.crystal;

import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class EndCrystalModel extends EntityModel<EndCrystalRenderState> {
   private static final String OUTER_GLASS = "outer_glass";
   private static final String INNER_GLASS = "inner_glass";
   private static final String BASE = "base";
   private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
   public final ModelPart base;
   public final ModelPart outerGlass;
   public final ModelPart innerGlass;
   public final ModelPart cube;

   public EndCrystalModel(ModelPart p_460663_) {
      super(p_460663_);
      this.base = p_460663_.getChild("base");
      this.outerGlass = p_460663_.getChild("outer_glass");
      this.innerGlass = this.outerGlass.getChild("inner_glass");
      this.cube = this.innerGlass.getChild("cube");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = 0.875F;
      CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("outer_glass", cubelistbuilder, PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("inner_glass", cubelistbuilder, PartPose.ZERO.withScale(0.875F));
      partdefinition2.addOrReplaceChild(
         "cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO.withScale(0.765625F)
      );
      partdefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public void setupAnim(EndCrystalRenderState p_451747_) {
      super.setupAnim(p_451747_);
      this.base.visible = p_451747_.showsBottom;
      float f = p_451747_.ageInTicks * 3.0F;
      float f1 = EndCrystalRenderer.getY(p_451747_.ageInTicks) * 16.0F;
      this.outerGlass.y += f1 / 2.0F;
      this.outerGlass.rotateBy(Axis.YP.rotationDegrees(f).rotateAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      this.innerGlass.rotateBy(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45).rotateY(f * (float) (Math.PI / 180.0)));
      this.cube.rotateBy(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45).rotateY(f * (float) (Math.PI / 180.0)));
   }
}
