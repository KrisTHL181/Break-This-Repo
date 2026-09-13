package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndermanRenderer extends MobRenderer<EnderMan, EndermanRenderState, EndermanModel<EndermanRenderState>> {
   private static final Identifier ENDERMAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
   private final RandomSource random = RandomSource.create();

   public EndermanRenderer(EntityRendererProvider.Context p_173992_) {
      super(p_173992_, new EndermanModel<>(p_173992_.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
      this.addLayer(new EnderEyesLayer(this));
      this.addLayer(new CarriedBlockLayer(this));
   }

   public Vec3 getRenderOffset(EndermanRenderState p_361852_) {
      Vec3 vec3 = super.getRenderOffset(p_361852_);
      if (p_361852_.isCreepy) {
         double d0 = 0.02 * p_361852_.scale;
         return vec3.add(this.random.nextGaussian() * d0, 0.0, this.random.nextGaussian() * d0);
      } else {
         return vec3;
      }
   }

   public Identifier getTextureLocation(EndermanRenderState p_452287_) {
      return ENDERMAN_LOCATION;
   }

   public EndermanRenderState createRenderState() {
      return new EndermanRenderState();
   }

   public void extractRenderState(EnderMan p_364627_, EndermanRenderState p_364804_, float p_362083_) {
      super.extractRenderState(p_364627_, p_364804_, p_362083_);
      HumanoidMobRenderer.extractHumanoidRenderState(p_364627_, p_364804_, p_362083_, this.itemModelResolver);
      p_364804_.isCreepy = p_364627_.isCreepy();
      p_364804_.carriedBlock = p_364627_.getCarriedBlock();
   }
}
