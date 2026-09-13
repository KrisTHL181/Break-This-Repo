package net.minecraft.client.renderer.chunk;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SectionMesh extends AutoCloseable {
   default boolean isDifferentPointOfView(TranslucencyPointOfView p_406250_) {
      return false;
   }

   default boolean hasRenderableLayers() {
      return false;
   }

   default boolean hasTranslucentGeometry() {
      return false;
   }

   default boolean isEmpty(ChunkSectionLayer p_410400_) {
      return true;
   }

   default List<BlockEntity> getRenderableBlockEntities() {
      return Collections.emptyList();
   }

   boolean facesCanSeeEachother(Direction var1, Direction var2);

   default @Nullable SectionBuffers getBuffers(ChunkSectionLayer p_409041_) {
      return null;
   }

   @Override
   default void close() {
   }
}
