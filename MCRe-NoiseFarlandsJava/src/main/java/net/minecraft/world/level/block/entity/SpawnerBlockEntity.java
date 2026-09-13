package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpawnerBlockEntity extends BlockEntity implements Spawner {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(final Level level, final BlockPos pos, final int id) {
            level.blockEvent(pos, Blocks.SPAWNER, id, 0);
        }

        @Override
        public void setNextSpawnData(final @Nullable Level level, final BlockPos pos, final SpawnData nextSpawnData) {
            super.setNextSpawnData(level, pos, nextSpawnData);
            if (level != null) {
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 260);
            }
        }
    };

    public SpawnerBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
        super(BlockEntityTypes.MOB_SPAWNER, worldPosition, blockState);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.spawner.load(this.level, this.worldPosition, input);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.spawner.save(output);
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final SpawnerBlockEntity entity) {
        entity.spawner.clientTick(level, pos);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final SpawnerBlockEntity entity) {
        entity.spawner.serverTick((ServerLevel)level, pos);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        CompoundTag tag = this.saveCustomOnly(registries);
        tag.remove("SpawnPotentials");
        return tag;
    }

    @Override
    public boolean triggerEvent(final int b0, final int b1) {
        return this.spawner.onEventTriggered(this.level, b0) ? true : super.triggerEvent(b0, b1);
    }

    @Override
    public void setEntityId(final EntityType<?> type, final RandomSource random) {
        this.spawner.setEntityId(type, this.level, random, this.worldPosition);
        this.setChanged();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }
}