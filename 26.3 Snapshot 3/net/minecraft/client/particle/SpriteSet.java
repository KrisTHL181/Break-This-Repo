package net.minecraft.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

public interface SpriteSet {
   TextureAtlasSprite get(final int index, final int max);

   TextureAtlasSprite get(RandomSource random);

   TextureAtlasSprite first();
}
