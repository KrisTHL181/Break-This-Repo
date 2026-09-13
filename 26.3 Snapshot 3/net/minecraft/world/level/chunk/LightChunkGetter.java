package net.minecraft.world.level.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

public interface LightChunkGetter {
   @Nullable LightChunk getChunkForLighting(final int x, final int z);

   default void onLightUpdate(final LightLayer layer, final SectionPos pos) {
   }

   BlockGetter getLevel();
}
