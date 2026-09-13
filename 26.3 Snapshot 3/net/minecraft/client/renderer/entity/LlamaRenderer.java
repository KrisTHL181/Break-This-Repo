package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.animal.llama.BabyLlamaModel;
import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Llama;

public class LlamaRenderer extends AgeableMobRenderer<Llama, LlamaRenderState, LlamaModel> {
   private static final Map<Llama.Variant, Identifier> TEXTURES = Maps.newEnumMap(
      Map.of(
         Llama.Variant.CREAMY,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_creamy.png"),
         Llama.Variant.WHITE,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_white.png"),
         Llama.Variant.BROWN,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_brown.png"),
         Llama.Variant.GRAY,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_gray.png")
      )
   );
   private static final Map<Llama.Variant, Identifier> BABY_TEXTURES = Maps.newEnumMap(
      Map.of(
         Llama.Variant.CREAMY,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_creamy_baby.png"),
         Llama.Variant.WHITE,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_white_baby.png"),
         Llama.Variant.BROWN,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_brown_baby.png"),
         Llama.Variant.GRAY,
         Identifier.withDefaultNamespace("textures/entity/llama/llama_gray_baby.png")
      )
   );

   public LlamaRenderer(final EntityRendererProvider.Context context, final ModelLayerLocation model, final ModelLayerLocation babyModel) {
      super(context, new LlamaModel(context.bakeLayer(model)), new BabyLlamaModel(context.bakeLayer(babyModel)), 0.7F);
      this.addLayer(new LlamaDecorLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
   }

   public Identifier getTextureLocation(final LlamaRenderState state) {
      Map<Llama.Variant, Identifier> textures = state.isBaby ? BABY_TEXTURES : TEXTURES;
      return textures.get(state.variant);
   }

   public LlamaRenderState createRenderState() {
      return new LlamaRenderState();
   }

   public void extractRenderState(final Llama entity, final LlamaRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant();
      state.hasChest = !entity.isBaby() && entity.hasChest();
      state.bodyItem = entity.getBodyArmorItem();
      state.isTraderLlama = entity.isTraderLlama();
   }
}
