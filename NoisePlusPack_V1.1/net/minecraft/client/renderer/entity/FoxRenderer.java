package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.fox.FoxModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoxRenderer extends AgeableMobRenderer<Fox, FoxRenderState, FoxModel> {
   private static final Identifier RED_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox.png");
   private static final Identifier RED_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/fox_sleep.png");
   private static final Identifier SNOW_FOX_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/snow_fox.png");
   private static final Identifier SNOW_FOX_SLEEP_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fox/snow_fox_sleep.png");

   public FoxRenderer(EntityRendererProvider.Context p_174127_) {
      super(p_174127_, new FoxModel(p_174127_.bakeLayer(ModelLayers.FOX)), new FoxModel(p_174127_.bakeLayer(ModelLayers.FOX_BABY)), 0.4F);
      this.addLayer(new FoxHeldItemLayer(this));
   }

   protected void setupRotations(FoxRenderState p_368844_, PoseStack p_114731_, float p_114732_, float p_114733_) {
      super.setupRotations(p_368844_, p_114731_, p_114732_, p_114733_);
      if (p_368844_.isPouncing || p_368844_.isFaceplanted) {
         p_114731_.mulPose(Axis.XP.rotationDegrees(-p_368844_.xRot));
      }
   }

   public Identifier getTextureLocation(FoxRenderState p_454222_) {
      if (p_454222_.variant == Fox.Variant.RED) {
         return p_454222_.isSleeping ? RED_FOX_SLEEP_TEXTURE : RED_FOX_TEXTURE;
      } else {
         return p_454222_.isSleeping ? SNOW_FOX_SLEEP_TEXTURE : SNOW_FOX_TEXTURE;
      }
   }

   public FoxRenderState createRenderState() {
      return new FoxRenderState();
   }

   public void extractRenderState(Fox p_454633_, FoxRenderState p_363044_, float p_362619_) {
      super.extractRenderState(p_454633_, p_363044_, p_362619_);
      HoldingEntityRenderState.extractHoldingEntityRenderState(p_454633_, p_363044_, this.itemModelResolver);
      p_363044_.headRollAngle = p_454633_.getHeadRollAngle(p_362619_);
      p_363044_.isCrouching = p_454633_.isCrouching();
      p_363044_.crouchAmount = p_454633_.getCrouchAmount(p_362619_);
      p_363044_.isSleeping = p_454633_.isSleeping();
      p_363044_.isSitting = p_454633_.isSitting();
      p_363044_.isFaceplanted = p_454633_.isFaceplanted();
      p_363044_.isPouncing = p_454633_.isPouncing();
      p_363044_.variant = p_454633_.getVariant();
   }
}
