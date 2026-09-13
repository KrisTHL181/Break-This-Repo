package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BeaconBlock extends BaseEntityBlock implements BeaconBeamBlock {
   public static final MapCodec<BeaconBlock> CODEC = simpleCodec(BeaconBlock::new);

   @Override
   public MapCodec<BeaconBlock> codec() {
      return CODEC;
   }

   public BeaconBlock(BlockBehaviour.Properties p_49421_) {
      super(p_49421_);
   }

   @Override
   public DyeColor getColor() {
      return DyeColor.WHITE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152164_, BlockState p_152165_) {
      return new BeaconBlockEntity(p_152164_, p_152165_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_152160_, BlockState p_152161_, BlockEntityType<T> p_152162_) {
      return createTickerHelper(p_152162_, BlockEntityType.BEACON, BeaconBlockEntity::tick);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_335352_, Level p_329169_, BlockPos p_333104_, Player p_330505_, BlockHitResult p_335231_) {
      if (!p_329169_.isClientSide() && p_329169_.getBlockEntity(p_333104_) instanceof BeaconBlockEntity beaconblockentity) {
         p_330505_.openMenu(beaconblockentity);
         p_330505_.awardStat(Stats.INTERACT_WITH_BEACON);
      }

      return InteractionResult.SUCCESS;
   }
}
