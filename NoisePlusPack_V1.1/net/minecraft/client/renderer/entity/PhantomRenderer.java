package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhantomRenderer extends MobRenderer<Phantom, PhantomRenderState, PhantomModel> {
   private static final Identifier PHANTOM_LOCATION = Identifier.withDefaultNamespace("textures/entity/phantom.png");

   public PhantomRenderer(EntityRendererProvider.Context p_174338_) {
      super(p_174338_, new PhantomModel(p_174338_.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
      this.addLayer(new PhantomEyesLayer(this));
   }

   public Identifier getTextureLocation(PhantomRenderState p_361615_) {
      return PHANTOM_LOCATION;
   }

   public PhantomRenderState createRenderState() {
      return new PhantomRenderState();
   }

   public void extractRenderState(Phantom p_368777_, PhantomRenderState p_368842_, float p_361022_) {
      super.extractRenderState(p_368777_, p_368842_, p_361022_);
      p_368842_.flapTime = p_368777_.getUniqueFlapTickOffset() + p_368842_.ageInTicks;
      p_368842_.size = p_368777_.getPhantomSize();
   }

   protected void scale(PhantomRenderState p_362754_, PoseStack p_115670_) {
      float f = 1.0F + 0.15F * p_362754_.size;
      p_115670_.scale(f, f, f);
      p_115670_.translate(0.0F, 1.3125F, 0.1875F);
   }

   protected void setupRotations(PhantomRenderState p_368093_, PoseStack p_115674_, float p_115675_, float p_115676_) {
      super.setupRotations(p_368093_, p_115674_, p_115675_, p_115676_);
      p_115674_.mulPose(Axis.XP.rotationDegrees(p_368093_.xRot));
   }
}
