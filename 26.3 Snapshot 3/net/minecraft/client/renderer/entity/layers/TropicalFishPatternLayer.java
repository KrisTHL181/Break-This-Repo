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

   public TropicalFishPatternLayer(
      final RenderLayerParent<TropicalFishRenderState, EntityModel<TropicalFishRenderState>> renderer, final EntityModelSet modelSet
   ) {
      super(renderer);
      this.modelSmall = new TropicalFishSmallModel(modelSet.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL_PATTERN));
      this.modelLarge = new TropicalFishLargeModel(modelSet.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE_PATTERN));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final TropicalFishRenderState state,
      final float yRot,
      final float xRot
   ) {
      TropicalFish.Pattern variant = state.pattern;

      EntityModel<TropicalFishRenderState> model = switch (variant.base()) {
         case SMALL -> this.modelSmall;
         case LARGE -> this.modelLarge;
      };

      Identifier patternTexture = switch (variant) {
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
      coloredCutoutModelCopyLayerRender(model, patternTexture, poseStack, submitNodeCollector, lightCoords, state, state.patternColor, 1);
   }
}
