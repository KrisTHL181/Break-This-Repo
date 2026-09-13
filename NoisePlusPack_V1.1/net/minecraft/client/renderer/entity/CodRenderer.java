package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CodRenderer extends MobRenderer<Cod, LivingEntityRenderState, CodModel> {
   private static final Identifier COD_LOCATION = Identifier.withDefaultNamespace("textures/entity/fish/cod.png");

   public CodRenderer(EntityRendererProvider.Context p_173954_) {
      super(p_173954_, new CodModel(p_173954_.bakeLayer(ModelLayers.COD)), 0.3F);
   }

   @Override
   public Identifier getTextureLocation(LivingEntityRenderState p_368865_) {
      return COD_LOCATION;
   }

   public LivingEntityRenderState createRenderState() {
      return new LivingEntityRenderState();
   }

   @Override
   protected void setupRotations(LivingEntityRenderState p_368017_, PoseStack p_114010_, float p_114011_, float p_114012_) {
      super.setupRotations(p_368017_, p_114010_, p_114011_, p_114012_);
      float f = 4.3F * Mth.sin(0.6F * p_368017_.ageInTicks);
      p_114010_.mulPose(Axis.YP.rotationDegrees(f));
      if (!p_368017_.isInWater) {
         p_114010_.translate(0.1F, 0.1F, -0.1F);
         p_114010_.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}
