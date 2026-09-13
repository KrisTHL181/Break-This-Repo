package net.minecraft.client.renderer.item;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ModelRenderProperties(boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms) {
   public static ModelRenderProperties fromResolvedModel(ModelBaker p_391505_, ResolvedModel p_393493_, TextureSlots p_396428_) {
      TextureAtlasSprite textureatlassprite = p_393493_.resolveParticleSprite(p_396428_, p_391505_);
      return new ModelRenderProperties(p_393493_.getTopGuiLight().lightLikeBlock(), textureatlassprite, p_393493_.getTopTransforms());
   }

   public void applyToLayer(ItemStackRenderState.LayerRenderState p_393213_, ItemDisplayContext p_396418_) {
      p_393213_.setUsesBlockLight(this.usesBlockLight);
      p_393213_.setParticleIcon(this.particleIcon);
      p_393213_.setTransform(this.transforms.getTransform(p_396418_));
   }
}
