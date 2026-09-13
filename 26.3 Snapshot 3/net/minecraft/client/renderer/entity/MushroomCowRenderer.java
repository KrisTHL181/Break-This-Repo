package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.animal.cow.BabyCowModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.layers.MushroomCowMushroomLayer;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.animal.cow.MushroomCow;

public class MushroomCowRenderer extends AgeableMobRenderer<MushroomCow, MushroomCowRenderState, CowModel> {
   public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
   private static final Map<MushroomCow.Variant, MushroomCowRenderer.MushroomCowTexture> TEXTURES = Util.make(
      Maps.newHashMap(),
      map -> {
         map.put(
            MushroomCow.Variant.BROWN,
            new MushroomCowRenderer.MushroomCowTexture(
               Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_brown.png"),
               Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_brown_baby.png")
            )
         );
         map.put(
            MushroomCow.Variant.RED,
            new MushroomCowRenderer.MushroomCowTexture(
               Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_red.png"),
               Identifier.withDefaultNamespace("textures/entity/cow/mooshroom_red_baby.png")
            )
         );
      }
   );
   private final BlockModelResolver blockModelResolver;

   public MushroomCowRenderer(final EntityRendererProvider.Context context) {
      super(context, new CowModel(context.bakeLayer(ModelLayers.MOOSHROOM)), new BabyCowModel(context.bakeLayer(ModelLayers.MOOSHROOM_BABY)), 0.7F);
      this.blockModelResolver = context.getBlockModelResolver();
      this.addLayer(new MushroomCowMushroomLayer(this));
   }

   public Identifier getTextureLocation(final MushroomCowRenderState state) {
      return state.isBaby ? TEXTURES.get(state.variant).baby : TEXTURES.get(state.variant).adult;
   }

   public MushroomCowRenderState createRenderState() {
      return new MushroomCowRenderState();
   }

   public void extractRenderState(final MushroomCow entity, final MushroomCowRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant();
      this.blockModelResolver.update(state.mushroomModel, state.variant.getBlockState(), BLOCK_DISPLAY_CONTEXT);
   }

   private record MushroomCowTexture(Identifier adult, Identifier baby) {
   }
}
