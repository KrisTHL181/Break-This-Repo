package net.minecraft.commands.arguments.blocks;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BlockInput implements Predicate<BlockInWorld> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockState state;
    private final Set<Property<?>> properties;
    private final @Nullable CompoundTag tag;

    public BlockInput(final BlockState state, final Set<Property<?>> properties, final @Nullable CompoundTag tag) {
        this.state = state;
        this.properties = properties;
        this.tag = tag;
    }

    public BlockState getState() {
        return this.state;
    }

    public Set<Property<?>> getDefinedProperties() {
        return this.properties;
    }

    public boolean test(final BlockInWorld blockInWorld) {
        BlockState state = blockInWorld.getState();
        if (!state.is(this.state.getBlock())) {
            return false;
        }

        for (Property<?> property : this.properties) {
            if (state.getValue(property) != this.state.getValue(property)) {
                return false;
            }
        }

        if (this.tag == null) {
            return true;
        }

        BlockEntity entity = blockInWorld.getEntity();
        return entity != null && NbtUtils.compareNbt(this.tag, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
    }

    public boolean test(final ServerLevel level, final BlockPos pos) {
        return this.test(new BlockInWorld(level, pos, false));
    }

    public boolean place(final ServerLevel level, final BlockPos pos, final @Block.UpdateFlags int update) {
        BlockState state = (update & 16) != 0 ? this.state : Block.updateFromNeighbourShapes(this.state, level, pos);
        if (state.isAir()) {
            state = this.state;
        }

        state = this.overwriteWithDefinedProperties(state);
        boolean affected = false;
        if (level.setBlock(pos, state, update)) {
            affected = true;
        }

        if (this.tag != null) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
                    HolderLookup.Provider registries = level.registryAccess();
                    ProblemReporter blockEntityReporter = reporter.forChild(entity.problemPath());
                    TagValueOutput initialOutput = TagValueOutput.createWithContext(blockEntityReporter.forChild(() -> "(before)"), registries);
                    entity.saveWithoutMetadata(initialOutput);
                    CompoundTag before = initialOutput.buildResult();
                    entity.loadWithComponents(TagValueInput.create(reporter, registries, this.tag));
                    TagValueOutput updatedOutput = TagValueOutput.createWithContext(blockEntityReporter.forChild(() -> "(after)"), registries);
                    entity.saveWithoutMetadata(updatedOutput);
                    CompoundTag after = updatedOutput.buildResult();
                    if (!after.equals(before)) {
                        affected = true;
                        entity.setChanged();
                        level.getChunkSource().blockChanged(pos);
                    }
                }
            }
        }

        return affected;
    }

    private BlockState overwriteWithDefinedProperties(BlockState state) {
        if (state == this.state) {
            return state;
        }

        for (Property<?> property : this.properties) {
            state = copyProperty(state, this.state, property);
        }

        return state;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(final BlockState target, final BlockState source, final Property<T> property) {
        return target.trySetValue(property, source.getValue(property));
    }
}