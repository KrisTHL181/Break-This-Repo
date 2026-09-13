package net.minecraft.world.level.block;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import org.jspecify.annotations.Nullable;

public class WitherSkullBlock extends SkullBlock {
   private static @Nullable BlockPattern witherPatternFull;
   private static @Nullable BlockPattern witherPatternBase;

   protected WitherSkullBlock(final BlockBehaviour.Properties properties) {
      super(SkullBlock.Types.WITHER_SKELETON, properties);
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      checkSpawn(level, pos);
   }

   public static void checkSpawn(final Level level, final BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof SkullBlockEntity placedSkull) {
         checkSpawn(level, pos, placedSkull);
      }
   }

   public static void checkSpawn(final Level level, final BlockPos pos, final SkullBlockEntity placedSkull) {
      if (!level.isClientSide()) {
         BlockState blockState = placedSkull.getBlockState();
         boolean correctBlock = blockState.is(Blocks.WITHER_SKELETON_SKULL) || blockState.is(Blocks.WITHER_SKELETON_WALL_SKULL);
         if (correctBlock && pos.getY() >= level.getMinY() && level.getDifficulty() != Difficulty.PEACEFUL) {
            BlockPattern.BlockPatternMatch match = getOrCreateWitherFull().find(level, pos);
            if (match != null) {
               WitherBoss witherBoss = EntityTypes.WITHER.create(level, EntitySpawnReason.TRIGGERED);
               if (witherBoss != null) {
                  CarvedPumpkinBlock.clearPatternBlocks(level, match);
                  BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
                  witherBoss.snapTo(
                     spawnPos.getX() + 0.5,
                     spawnPos.getY() + 0.55,
                     spawnPos.getZ() + 0.5,
                     match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F,
                     0.0F
                  );
                  witherBoss.yBodyRot = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
                  witherBoss.makeInvulnerable();

                  for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, witherBoss.getBoundingBox().inflate(50.0))) {
                     CriteriaTriggers.SUMMONED_ENTITY.trigger(player, witherBoss);
                  }

                  level.addFreshEntity(witherBoss);
                  CarvedPumpkinBlock.updatePatternBlocks(level, match);
               }
            }
         }
      }
   }

   public static boolean canSpawnMob(final Level level, final BlockPos pos, final ItemStack itemStack) {
      return itemStack.is(Items.WITHER_SKELETON_SKULL)
            && pos.getY() >= level.getMinY() + 2
            && level.getDifficulty() != Difficulty.PEACEFUL
            && !level.isClientSide()
         ? getOrCreateWitherBase().find(level, pos) != null
         : false;
   }

   private static BlockPattern getOrCreateWitherFull() {
      if (witherPatternFull == null) {
         witherPatternFull = BlockPatternBuilder.start()
            .aisle("^^^", "###", "~#~")
            .where('#', block -> block.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
            .where(
               '^',
               BlockInWorld.hasState(
                  BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL).or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL))
               )
            )
            .where('~', block -> block.getState().isAir())
            .build();
      }

      return witherPatternFull;
   }

   private static BlockPattern getOrCreateWitherBase() {
      if (witherPatternBase == null) {
         witherPatternBase = BlockPatternBuilder.start()
            .aisle("   ", "###", "~#~")
            .where('#', block -> block.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
            .where('~', block -> block.getState().isAir())
            .build();
      }

      return witherPatternBase;
   }
}
