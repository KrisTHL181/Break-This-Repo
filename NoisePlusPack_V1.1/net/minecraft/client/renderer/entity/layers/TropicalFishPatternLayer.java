package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishPatternLayer extends RenderLayer<TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
   private static final Identifier KOB_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_1.png");
   private static final Identifier SUNSTREAK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_2.png");
   private static final Identifier SNOOPER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_3.png");
   private static final Identifier DASHER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_4.png");
   private static final Identifier BRINELY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_5.png");
   private static final Identifier SPOTTY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_6.png");
   private static final Identifier FLOPPER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_1.png");
   private static final Identifier STRIPEY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_2.png");
   private static final Identifier GLITTER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_3.png");
   private static final Identifier BLOCKFISH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_4.png");
   private static final Identifier BETTY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_5.png");
   private static final Identifier CLAYFISH_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_6.png");
   private final TropicalFishSmallModel modelSmall;
   private final TropicalFishLargeModel modelLarge;

   public TropicalFishPatternLayer(RenderLayerParent<TropicalFishRenderState, EntityModel<TropicalFishRenderState>> p_174547_, EntityModelSet p_174548_) {
      super(p_174547_);
      this.modelSmall = new TropicalFishSmallModel(p_174548_.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL_PATTERN));
      this.modelLarge = new TropicalFishLargeModel(p_174548_.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE_PATTERN));
   }

   public void submit(PoseStack p_117612_, SubmitNodeCollector p_430641_, int p_117614_, TropicalFishRenderState p_366498_, float p_117616_, float p_117617_) {
      TropicalFish.Pattern tropicalfish$pattern = p_366498_.pattern;

      EntityModel<TropicalFishRenderState> entitymodel = switch (tropicalfish$pattern.base()) {
         case SMALL -> this.modelSmall;
         case LARGE -> this.modelLarge;
      };

      Identifier identifier = switch (tropicalfish$pattern) {
         case KOB -> KOB_TEXTURE;
         case SUNSTREAK -> SUNSTREAK_TEXTURE;
         case SNOOPER -> SNOOPER_TEXTURE;
         case DASHER -> DASHER_TEXTURE;
         case BRINELY -> BRINELY_TEXTURE;
         case SPOTTY -> SPOTTY_TEXTURE;
         case FLOPPER -> FLOPPER_TEXTURE;
         case STRIPEY -> STRIPEY_TEXTURE;
         case GLITTER -> GLITTER_TEXTURE;
         case BLOCKFISH -> BLOCKFISH_TEXTURE;
         case BETTY -> BETTY_TEXTURE;
         case CLAYFISH -> CLAYFISH_TEXTURE;
      };
      coloredCutoutModelCopyLayerRender(entitymodel, identifier, p_117612_, p_430641_, p_117614_, p_366498_, p_366498_.patternColor, 1);
   }
}
