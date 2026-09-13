package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.axolotl.AdultAxolotlModel;
import net.minecraft.client.model.animal.axolotl.BabyAxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

public class AxolotlRenderer extends AgeableMobRenderer<Axolotl, AxolotlRenderState, EntityModel<AxolotlRenderState>> {
   private static final Map<Axolotl.Variant, AxolotlRenderer.AxolotlTextures> TEXTURE_BY_TYPE = Util.make(
      Maps.newHashMap(),
      map -> {
         for (Axolotl.Variant variant : Axolotl.Variant.values()) {
            AxolotlRenderer.AxolotlTextures textures = new AxolotlRenderer.AxolotlTextures(
               Identifier.withDefaultNamespace(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", variant.getName())),
               Identifier.withDefaultNamespace(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s_baby.png", variant.getName()))
            );
            map.put(variant, textures);
         }
      }
   );

   public AxolotlRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultAxolotlModel(context.bakeLayer(ModelLayers.AXOLOTL)), new BabyAxolotlModel(context.bakeLayer(ModelLayers.AXOLOTL_BABY)), 0.5F);
   }

   public Identifier getTextureLocation(final AxolotlRenderState state) {
      AxolotlRenderer.AxolotlTextures textures = TEXTURE_BY_TYPE.get(state.variant);
      return state.isBaby ? textures.baby : textures.adult;
   }

   public AxolotlRenderState createRenderState() {
      return new AxolotlRenderState();
   }

   public void extractRenderState(final Axolotl entity, final AxolotlRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant();
      state.playingDeadFactor = entity.playingDeadAnimator.getFactor(partialTicks);
      state.inWaterFactor = entity.inWaterAnimator.getFactor(partialTicks);
      state.onGroundFactor = entity.onGroundAnimator.getFactor(partialTicks);
      state.movingFactor = entity.movingAnimator.getFactor(partialTicks);
      state.swimAnimation.copyFrom(entity.swimAnimationState);
      state.walkAnimationState.copyFrom(entity.walkAnimationState);
      state.walkUnderWaterAnimationState.copyFrom(entity.walkUnderWaterAnimationState);
      state.idleOnGroundAnimationState.copyFrom(entity.idleOnGroundAnimationState);
      state.idleUnderWaterOnGroundAnimationState.copyFrom(entity.idleUnderWaterOnGroundAnimationState);
      state.idleUnderWaterAnimationState.copyFrom(entity.idleUnderWaterAnimationState);
      state.playDeadAnimationState.copyFrom(entity.playDeadAnimationState);
   }

   private record AxolotlTextures(Identifier adult, Identifier baby) {
   }
}
