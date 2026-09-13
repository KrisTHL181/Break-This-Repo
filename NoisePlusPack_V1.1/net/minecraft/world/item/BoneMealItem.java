package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

public class BoneMealItem extends Item {
   public static final int GRASS_SPREAD_WIDTH = 3;
   public static final int GRASS_SPREAD_HEIGHT = 1;
   public static final int GRASS_COUNT_MULTIPLIER = 3;

   public BoneMealItem(Item.Properties p_40626_) {
      super(p_40626_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40637_) {
      Level level = p_40637_.getLevel();
      BlockPos blockpos = p_40637_.getClickedPos();
      BlockPos blockpos1 = blockpos.relative(p_40637_.getClickedFace());
      ItemStack itemstack = p_40637_.getItemInHand();
      if (growCrop(itemstack, level, blockpos)) {
         if (!level.isClientSide()) {
            itemstack.causeUseVibration(p_40637_.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
            level.levelEvent(1505, blockpos, 15);
         }

         return InteractionResult.SUCCESS;
      } else {
         BlockState blockstate = level.getBlockState(blockpos);
         boolean flag = blockstate.isFaceSturdy(level, blockpos, p_40637_.getClickedFace());
         if (flag && growWaterPlant(itemstack, level, blockpos1, p_40637_.getClickedFace())) {
            if (!level.isClientSide()) {
               itemstack.causeUseVibration(p_40637_.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
               level.levelEvent(1505, blockpos1, 15);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static boolean growCrop(ItemStack p_40628_, Level p_40629_, BlockPos p_40630_) {
      BlockState blockstate = p_40629_.getBlockState(p_40630_);
      if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock && bonemealableblock.isValidBonemealTarget(p_40629_, p_40630_, blockstate)) {
         if (p_40629_ instanceof ServerLevel) {
            if (bonemealableblock.isBonemealSuccess(p_40629_, p_40629_.random, p_40630_, blockstate)) {
               bonemealableblock.performBonemeal((ServerLevel)p_40629_, p_40629_.random, p_40630_, blockstate);
            }

            p_40628_.shrink(1);
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean growWaterPlant(ItemStack p_40632_, Level p_40633_, BlockPos p_40634_, @Nullable Direction p_40635_) {
      if (p_40633_.getBlockState(p_40634_).is(Blocks.WATER) && p_40633_.getFluidState(p_40634_).getAmount() == 8) {
         if (!(p_40633_ instanceof ServerLevel)) {
            return true;
         }

         RandomSource randomsource = p_40633_.getRandom();

         label79:
         for (int i = 0; i < 128; i++) {
            BlockPos blockpos = p_40634_;
            BlockState blockstate = Blocks.SEAGRASS.defaultBlockState();

            for (int j = 0; j < i / 16; j++) {
               blockpos = blockpos.offset(randomsource.nextInt(3) - 1, (randomsource.nextInt(3) - 1) * randomsource.nextInt(3) / 2, randomsource.nextInt(3) - 1);
               if (p_40633_.getBlockState(blockpos).isCollisionShapeFullBlock(p_40633_, blockpos)) {
                  continue label79;
               }
            }

            Holder<Biome> holder = p_40633_.getBiome(blockpos);
            if (holder.is(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
               if (i == 0 && p_40635_ != null && p_40635_.getAxis().isHorizontal()) {
                  blockstate = BuiltInRegistries.BLOCK
                     .getRandomElementOf(BlockTags.WALL_CORALS, p_40633_.random)
                     .map(p_204100_ -> p_204100_.value().defaultBlockState())
                     .orElse(blockstate);
                  if (blockstate.hasProperty(BaseCoralWallFanBlock.FACING)) {
                     blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, p_40635_);
                  }
               } else if (randomsource.nextInt(4) == 0) {
                  blockstate = BuiltInRegistries.BLOCK
                     .getRandomElementOf(BlockTags.UNDERWATER_BONEMEALS, p_40633_.random)
                     .map(p_204095_ -> p_204095_.value().defaultBlockState())
                     .orElse(blockstate);
               }
            }

            if (blockstate.is(BlockTags.WALL_CORALS, p_359379_ -> p_359379_.hasProperty(BaseCoralWallFanBlock.FACING))) {
               for (int k = 0; !blockstate.canSurvive(p_40633_, blockpos) && k < 4; k++) {
                  blockstate = blockstate.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(randomsource));
               }
            }

            if (blockstate.canSurvive(p_40633_, blockpos)) {
               BlockState blockstate1 = p_40633_.getBlockState(blockpos);
               if (blockstate1.is(Blocks.WATER) && p_40633_.getFluidState(blockpos).getAmount() == 8) {
                  p_40633_.setBlock(blockpos, blockstate, 3);
               } else if (blockstate1.is(Blocks.SEAGRASS)
                  && ((BonemealableBlock)Blocks.SEAGRASS).isValidBonemealTarget(p_40633_, blockpos, blockstate1)
                  && randomsource.nextInt(10) == 0) {
                  ((BonemealableBlock)Blocks.SEAGRASS).performBonemeal((ServerLevel)p_40633_, randomsource, blockpos, blockstate1);
               }
            }
         }

         p_40632_.shrink(1);
         return true;
      } else {
         return false;
      }
   }

   public static void addGrowthParticles(LevelAccessor p_40639_, BlockPos p_40640_, int p_40641_) {
      BlockState blockstate = p_40639_.getBlockState(p_40640_);
      if (blockstate.getBlock() instanceof BonemealableBlock bonemealableblock) {
         BlockPos blockpos = bonemealableblock.getParticlePos(p_40640_);
         switch (bonemealableblock.getType()) {
            case NEIGHBOR_SPREADER:
               ParticleUtils.spawnParticles(p_40639_, blockpos, p_40641_ * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
               break;
            case GROWER:
               ParticleUtils.spawnParticleInBlock(p_40639_, blockpos, p_40641_, ParticleTypes.HAPPY_VILLAGER);
         }
      } else if (blockstate.is(Blocks.WATER)) {
         ParticleUtils.spawnParticles(p_40639_, p_40640_, p_40641_ * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
      }
   }
}
