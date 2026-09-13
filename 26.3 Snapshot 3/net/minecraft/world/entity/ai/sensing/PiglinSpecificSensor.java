package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PiglinSpecificSensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(
         MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
         MemoryModuleType.NEAREST_LIVING_ENTITIES,
         MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
         MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
         MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
         MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,
         new MemoryModuleType[]{
            MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
            MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
            MemoryModuleType.NEARBY_ADULT_PIGLINS,
            MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
            MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
            MemoryModuleType.NEAREST_REPELLENT
         }
      );
   }

   @Override
   protected void doTick(final ServerLevel level, final LivingEntity body) {
      Brain<?> brain = body.getBrain();
      brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, findNearestRepellent(level, body));
      Optional<Mob> nemesis = Optional.empty();
      Optional<Hoglin> huntableHoglin = Optional.empty();
      Optional<Hoglin> babyHoglin = Optional.empty();
      Optional<Piglin> babyPiglin = Optional.empty();
      Optional<LivingEntity> zombified = Optional.empty();
      Optional<Player> playerNotWearingGold = Optional.empty();
      Optional<Player> playerHoldingWantedItem = Optional.empty();
      int visibleAdultHoglinCount = 0;
      List<AbstractPiglin> visibleAdultPiglins = new ArrayList<>();
      NearestVisibleLivingEntities visibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .orElse(NearestVisibleLivingEntities.empty());

      for (LivingEntity entity : visibleLivingEntities.findAll(ignored -> true)) {
         if (entity instanceof Hoglin hoglin) {
            if (hoglin.isBaby() && babyHoglin.isEmpty()) {
               babyHoglin = Optional.of(hoglin);
            } else if (hoglin.isAdult()) {
               visibleAdultHoglinCount++;
               if (huntableHoglin.isEmpty() && hoglin.canBeHunted()) {
                  huntableHoglin = Optional.of(hoglin);
               }
            }
         } else if (entity instanceof PiglinBrute piglinBrute) {
            visibleAdultPiglins.add(piglinBrute);
         } else if (entity instanceof Piglin piglin) {
            if (piglin.isBaby() && babyPiglin.isEmpty()) {
               babyPiglin = Optional.of(piglin);
            } else if (piglin.isAdult()) {
               visibleAdultPiglins.add(piglin);
            }
         } else if (entity instanceof Player player) {
            if (playerNotWearingGold.isEmpty() && !PiglinAi.isWearingSafeArmor(player) && body.canAttack(entity)) {
               playerNotWearingGold = Optional.of(player);
            }

            if (playerHoldingWantedItem.isEmpty() && !player.isSpectator() && PiglinAi.isPlayerHoldingLovedItem(player)) {
               playerHoldingWantedItem = Optional.of(player);
            }
         } else if (!nemesis.isEmpty() || !(entity instanceof WitherSkeleton) && !(entity instanceof WitherBoss)) {
            if (zombified.isEmpty() && PiglinAi.isZombified(entity)) {
               zombified = Optional.of(entity);
            }
         } else {
            nemesis = Optional.of((Mob)entity);
         }
      }

      List<AbstractPiglin> adultPiglins = PiglinAi.findNearbyAdultPiglins(brain);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nemesis);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, huntableHoglin);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, babyHoglin);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, zombified);
      brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, playerNotWearingGold);
      brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, playerHoldingWantedItem);
      brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, adultPiglins);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, visibleAdultPiglins);
      brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, visibleAdultPiglins.size());
      brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, visibleAdultHoglinCount);
   }

   private static Optional<BlockPos> findNearestRepellent(final ServerLevel level, final LivingEntity body) {
      return BlockPos.findClosestMatch(body.blockPosition(), 8, 4, pos -> isValidRepellent(level, pos));
   }

   private static boolean isValidRepellent(final ServerLevel level, final BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      boolean isRepellent = blockState.is(BlockTags.PIGLIN_REPELLENTS);
      return isRepellent && blockState.is(Blocks.SOUL_CAMPFIRE) ? CampfireBlock.isLitCampfire(blockState) : isRepellent;
   }
}
