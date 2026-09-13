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
import org.joml.Quaternionf;

public class EndCrystalModel extends EntityModel<EndCrystalRenderState> {
   private static final String OUTER_GLASS = "outer_glass";
   private static final String INNER_GLASS = "inner_glass";
   private static final String BASE = "base";
   private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
   public final ModelPart base;
   public final ModelPart outerGlass;
   public final ModelPart innerGlass;
   public final ModelPart cube;

   public EndCrystalModel(final ModelPart root) {
      super(root);
      this.base = root.getChild("base");
      this.outerGlass = root.getChild("outer_glass");
      this.innerGlass = this.outerGlass.getChild("inner_glass");
      this.cube = this.innerGlass.getChild("cube");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float scale = 0.875F;
      CubeListBuilder glassCube = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      PartDefinition outerGlass = root.addOrReplaceChild("outer_glass", glassCube, PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition innerGlass = outerGlass.addOrReplaceChild("inner_glass", glassCube, PartPose.ZERO.withScale(0.875F));
      innerGlass.addOrReplaceChild(
         "cube", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO.withScale(0.765625F)
      );
      root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 16).addBox(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public void setupAnim(final EndCrystalRenderState state) {
      super.setupAnim(state);
      this.base.visible = state.showsBottom;
      float animationSpeed = state.ageInTicks * 3.0F;
      float crystalY = EndCrystalRenderer.getY(state.ageInTicks) * 16.0F;
      this.outerGlass.y += crystalY / 2.0F;
      this.outerGlass.rotateBy(Axis.YP.rotationDegrees(animationSpeed).rotateAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
      this.innerGlass.rotateBy(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45).rotateY(animationSpeed * (float) (Math.PI / 180.0)));
      this.cube.rotateBy(new Quaternionf().setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45).rotateY(animationSpeed * (float) (Math.PI / 180.0)));
   }
}
