package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.animal.axolotl.AxolotlModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.AxolotlRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AxolotlRenderer extends AgeableMobRenderer<Axolotl, AxolotlRenderState, AxolotlModel> {
   private static final Map<Axolotl.Variant, Identifier> TEXTURE_BY_TYPE = Util.make(
      Maps.newHashMap(),
      p_448312_ -> {
         for (Axolotl.Variant axolotl$variant : Axolotl.Variant.values()) {
            p_448312_.put(
               axolotl$variant,
               Identifier.withDefaultNamespace(String.format(Locale.ROOT, "textures/entity/axolotl/axolotl_%s.png", axolotl$variant.getName()))
            );
         }
      }
   );

   public AxolotlRenderer(EntityRendererProvider.Context p_173921_) {
      super(p_173921_, new AxolotlModel(p_173921_.bakeLayer(ModelLayers.AXOLOTL)), new AxolotlModel(p_173921_.bakeLayer(ModelLayers.AXOLOTL_BABY)), 0.5F);
   }

   public Identifier getTextureLocation(AxolotlRenderState p_366754_) {
      return TEXTURE_BY_TYPE.get(p_366754_.variant);
   }

   public AxolotlRenderState createRenderState() {
      return new AxolotlRenderState();
   }

   public void extractRenderState(Axolotl p_366530_, AxolotlRenderState p_363503_, float p_370086_) {
      super.extractRenderState(p_366530_, p_363503_, p_370086_);
      p_363503_.variant = p_366530_.getVariant();
      p_363503_.playingDeadFactor = p_366530_.playingDeadAnimator.getFactor(p_370086_);
      p_363503_.inWaterFactor = p_366530_.inWaterAnimator.getFactor(p_370086_);
      p_363503_.onGroundFactor = p_366530_.onGroundAnimator.getFactor(p_370086_);
      p_363503_.movingFactor = p_366530_.movingAnimator.getFactor(p_370086_);
   }
}
