package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.llama.LlamaModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.client.renderer.entity.state.LlamaRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LlamaRenderer extends AgeableMobRenderer<Llama, LlamaRenderState, LlamaModel> {
   private static final Identifier CREAMY = Identifier.withDefaultNamespace("textures/entity/llama/creamy.png");
   private static final Identifier WHITE = Identifier.withDefaultNamespace("textures/entity/llama/white.png");
   private static final Identifier BROWN = Identifier.withDefaultNamespace("textures/entity/llama/brown.png");
   private static final Identifier GRAY = Identifier.withDefaultNamespace("textures/entity/llama/gray.png");

   public LlamaRenderer(EntityRendererProvider.Context p_174293_, ModelLayerLocation p_174294_, ModelLayerLocation p_361682_) {
      super(p_174293_, new LlamaModel(p_174293_.bakeLayer(p_174294_)), new LlamaModel(p_174293_.bakeLayer(p_361682_)), 0.7F);
      this.addLayer(new LlamaDecorLayer(this, p_174293_.getModelSet(), p_174293_.getEquipmentRenderer()));
   }

   public Identifier getTextureLocation(LlamaRenderState p_460599_) {
      return switch (p_460599_.variant) {
         case CREAMY -> CREAMY;
         case WHITE -> WHITE;
         case BROWN -> BROWN;
         case GRAY -> GRAY;
      };
   }

   public LlamaRenderState createRenderState() {
      return new LlamaRenderState();
   }

   public void extractRenderState(Llama p_456669_, LlamaRenderState p_369159_, float p_368423_) {
      super.extractRenderState(p_456669_, p_369159_, p_368423_);
      p_369159_.variant = p_456669_.getVariant();
      p_369159_.hasChest = !p_456669_.isBaby() && p_456669_.hasChest();
      p_369159_.bodyItem = p_456669_.getBodyArmorItem();
      p_369159_.isTraderLlama = p_456669_.isTraderLlama();
   }
}
