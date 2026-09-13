package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
   TextureAtlasSprite get(Material var1, ModelDebugName var2);

   TextureAtlasSprite reportMissingReference(String var1, ModelDebugName var2);

   default TextureAtlasSprite resolveSlot(TextureSlots p_396097_, String p_396912_, ModelDebugName p_394437_) {
      Material material = p_396097_.getMaterial(p_396912_);
      return material != null ? this.get(material, p_394437_) : this.reportMissingReference(p_396912_, p_394437_);
   }
}
