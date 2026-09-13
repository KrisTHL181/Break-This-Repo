package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record SetBlockProperties(BlockItemStateProperties properties, Vec3i offset, Optional<Holder<GameEvent>> triggerGameEvent)
    implements EnchantmentEntityEffect {
    public static final MapCodec<SetBlockProperties> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                BlockItemStateProperties.CODEC.fieldOf("properties").forGetter(SetBlockProperties::properties),
                Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(SetBlockProperties::offset),
                GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(SetBlockProperties::triggerGameEvent)
            )
            .apply(i, SetBlockProperties::new)
    );

    public SetBlockProperties(final BlockItemStateProperties properties) {
        this(properties, Vec3i.ZERO, Optional.of(GameEvent.BLOCK_CHANGE));
    }

    @Override
    public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
        BlockPos blockPos = BlockPos.containing(position).offset(this.offset);
        BlockState state = entity.level().getBlockState(blockPos);
        BlockState modified = this.properties.apply(state);
        if (state != modified && entity.level().setBlock(blockPos, modified, 3)) {
            this.triggerGameEvent.ifPresent(event -> serverLevel.gameEvent(entity, (Holder<GameEvent>)event, blockPos));
        }
    }

    @Override
    public MapCodec<SetBlockProperties> codec() {
        return CODEC;
    }
}