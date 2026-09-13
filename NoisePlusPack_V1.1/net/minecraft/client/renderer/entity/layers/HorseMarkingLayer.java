package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HorseMarkingLayer extends RenderLayer<HorseRenderState, HorseModel> {
   private static final Identifier INVISIBLE_TEXTURE = Identifier.withDefaultNamespace("invisible");
   private static final Map<Markings, Identifier> TEXTURE_BY_MARKINGS = Maps.newEnumMap(
      Map.of(
         Markings.NONE,
         INVISIBLE_TEXTURE,
         Markings.WHITE,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_white.png"),
         Markings.WHITE_FIELD,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png"),
         Markings.WHITE_DOTS,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png"),
         Markings.BLACK_DOTS,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png")
      )
   );

   public HorseMarkingLayer(RenderLayerParent<HorseRenderState, HorseModel> p_117045_) {
      super(p_117045_);
   }

   public void submit(PoseStack p_422496_, SubmitNodeCollector p_425591_, int p_431559_, HorseRenderState p_428730_, float p_428962_, float p_427320_) {
      Identifier identifier = TEXTURE_BY_MARKINGS.get(p_428730_.markings);
      if (identifier != INVISIBLE_TEXTURE && !p_428730_.isInvisible) {
         p_425591_.order(1)
            .submitModel(
               this.getParentModel(),
               p_428730_,
               p_422496_,
               RenderTypes.entityTranslucent(identifier),
               p_431559_,
               LivingEntityRenderer.getOverlayCoords(p_428730_, 0.0F),
               -1,
               null,
               p_428730_.outlineColor,
               null
            );
      }
   }
}
