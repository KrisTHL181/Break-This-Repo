package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SolidBucketItem extends BlockItem implements DispensibleContainerItem {
    private final SoundEvent placeSound;

    public SolidBucketItem(final Block content, final SoundEvent placeSound, final Item.Properties properties) {
        super(content, properties);
        this.placeSound = placeSound;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        InteractionResult placeResult = super.useOn(context);
        Player player = context.getPlayer();
        if (placeResult.consumesAction() && player != null) {
            player.setItemInHand(context.getHand(), BucketItem.getEmptySuccessItem(context.getItemInHand(), player));
        }

        return placeResult;
    }

    @Override
    protected SoundEvent getPlaceSound(final BlockState blockState) {
        return this.placeSound;
    }

    @Override
    public boolean emptyContents(final @Nullable LivingEntity user, final Level level, final BlockPos pos, final @Nullable BlockHitResult hitResult) {
        if (level.isInWorldBounds(pos) && level.isEmptyBlock(pos)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, this.getBlock().defaultBlockState(), 3);
            }

            level.gameEvent(user, GameEvent.FLUID_PLACE, pos);
            level.playSound(user, pos, this.placeSound, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        } else {
            return false;
        }
    }
}