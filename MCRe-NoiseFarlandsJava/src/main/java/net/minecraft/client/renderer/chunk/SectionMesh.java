package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.IndexType;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SectionMesh extends AutoCloseable {
    default boolean isDifferentPointOfView(final TranslucencyPointOfView pointOfView) {
        return false;
    }

    default boolean hasRenderableLayers() {
        return false;
    }

    default boolean hasTranslucentGeometry() {
        return false;
    }

    default boolean isEmpty(final ChunkSectionLayer layer) {
        return true;
    }

    default List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    boolean facesCanSeeEachother(Direction direction1, Direction direction2);

    default SectionMesh.@Nullable SectionDraw getSectionDraw(final ChunkSectionLayer layer) {
        return null;
    }

    @Override
    default void close() {
    }

    @OnlyIn(Dist.CLIENT)
    record SectionDraw(int indexCount, IndexType indexType, boolean hasCustomIndexBuffer) {
    }
}