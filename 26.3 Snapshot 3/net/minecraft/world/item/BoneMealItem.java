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

   public BoneMealItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockPos relative = pos.relative(context.getClickedFace());
      ItemStack boneMealStack = context.getItemInHand();
      if (growCrop(boneMealStack, level, pos)) {
         if (!level.isClientSide()) {
            boneMealStack.causeUseVibration(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
            level.levelEvent(1505, pos, 15);
            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         BlockState clickedState = level.getBlockState(pos);
         boolean solidBlockFace = clickedState.isFaceSturdy(level, pos, context.getClickedFace());
         if (solidBlockFace && growWaterPlant(boneMealStack, level, relative, context.getClickedFace())) {
            if (!level.isClientSide()) {
               boneMealStack.causeUseVibration(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH);
               level.levelEvent(1505, relative, 15);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static boolean growCrop(final ItemStack itemStack, final Level level, final BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof BonemealableBlock block && block.isValidBonemealTarget(level, pos, state)) {
         if (level instanceof ServerLevel serverLevel) {
            if (block.isBonemealSuccess(level, level.getRandom(), pos, state)) {
               block.performBonemeal(serverLevel, level.getRandom(), pos, state);
            }

            itemStack.shrink(1);
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean growWaterPlant(final ItemStack itemStack, final Level level, final BlockPos pos, final @Nullable Direction clickedFace) {
      if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isFull()) {
         if (!(level instanceof ServerLevel serverLevel)) {
            return true;
         } else {
            RandomSource random = level.getRandom();

            label81:
            for (int j = 0; j < 128; j++) {
               BlockPos testPos = pos;
               BlockState stateToGrow = Blocks.SEAGRASS.defaultBlockState();

               for (int i = 0; i < j / 16; i++) {
                  testPos = testPos.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                  if (level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos)) {
                     continue label81;
                  }
               }

               Holder<Biome> testBiome = level.getBiome(testPos);
               if (testBiome.is(BiomeTags.PRODUCES_CORALS_FROM_BONEMEAL)) {
                  if (j == 0 && clickedFace != null && clickedFace.getAxis().isHorizontal()) {
                     stateToGrow = BuiltInRegistries.BLOCK
                        .getRandomElementOf(BlockTags.WALL_CORALS, level.getRandom())
                        .map(h -> h.value().defaultBlockState())
                        .orElse(stateToGrow);
                     if (stateToGrow.hasProperty(BaseCoralWallFanBlock.FACING)) {
                        stateToGrow = stateToGrow.setValue(BaseCoralWallFanBlock.FACING, clickedFace);
                     }
                  } else if (random.nextInt(4) == 0) {
                     stateToGrow = BuiltInRegistries.BLOCK
                        .getRandomElementOf(BlockTags.UNDERWATER_BONEMEALS, level.getRandom())
                        .map(h -> h.value().defaultBlockState())
                        .orElse(stateToGrow);
                  }
               }

               if (stateToGrow.is(BlockTags.WALL_CORALS, s -> s.hasProperty(BaseCoralWallFanBlock.FACING))) {
                  for (int d = 0; !stateToGrow.canSurvive(level, testPos) && d < 4; d++) {
                     stateToGrow = stateToGrow.setValue(BaseCoralWallFanBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                  }
               }

               if (stateToGrow.canSurvive(level, testPos)) {
                  BlockState testState = level.getBlockState(testPos);
                  if (testState.is(Blocks.WATER) && level.getFluidState(testPos).isFull()) {
                     level.setBlockAndUpdate(testPos, stateToGrow);
                  } else if (testState.is(Blocks.SEAGRASS)
                     && ((BonemealableBlock)Blocks.SEAGRASS).isValidBonemealTarget(level, testPos, testState)
                     && random.nextInt(10) == 0) {
                     ((BonemealableBlock)Blocks.SEAGRASS).performBonemeal(serverLevel, random, testPos, testState);
                  }
               }
            }

            itemStack.shrink(1);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void addGrowthParticles(final LevelAccessor level, final BlockPos pos, final int count) {
      BlockState blockState = level.getBlockState(pos);
      if (blockState.getBlock() instanceof BonemealableBlock bonemealableBlock) {
         BlockPos particlePos = bonemealableBlock.getParticlePos(pos);
         switch (bonemealableBlock.getType()) {
            case NEIGHBOR_SPREADER:
               ParticleUtils.spawnParticles(level, particlePos, count * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
               break;
            case GROWER:
               ParticleUtils.spawnParticleInBlock(level, particlePos, count, ParticleTypes.HAPPY_VILLAGER);
         }
      } else if (blockState.is(Blocks.WATER)) {
         ParticleUtils.spawnParticles(level, pos, count * 3, 3.0, 1.0, false, ParticleTypes.HAPPY_VILLAGER);
      }
   }
}
