package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class GiveGiftToHero extends Behavior<Villager> {
   private static final int THROW_GIFT_AT_DISTANCE = 5;
   private static final int MIN_TIME_BETWEEN_GIFTS = 600;
   private static final int MAX_TIME_BETWEEN_GIFTS = 6600;
   private static final int TIME_TO_DELAY_FOR_HEAD_TO_FINISH_TURNING = 20;
   private static final Map<ResourceKey<VillagerProfession>, ResourceKey<LootTable>> GIFTS = ImmutableMap.builder()
      .put(VillagerProfession.ARMORER, BuiltInLootTables.ARMORER_GIFT)
      .put(VillagerProfession.BUTCHER, BuiltInLootTables.BUTCHER_GIFT)
      .put(VillagerProfession.CARTOGRAPHER, BuiltInLootTables.CARTOGRAPHER_GIFT)
      .put(VillagerProfession.CLERIC, BuiltInLootTables.CLERIC_GIFT)
      .put(VillagerProfession.FARMER, BuiltInLootTables.FARMER_GIFT)
      .put(VillagerProfession.FISHERMAN, BuiltInLootTables.FISHERMAN_GIFT)
      .put(VillagerProfession.FLETCHER, BuiltInLootTables.FLETCHER_GIFT)
      .put(VillagerProfession.LEATHERWORKER, BuiltInLootTables.LEATHERWORKER_GIFT)
      .put(VillagerProfession.LIBRARIAN, BuiltInLootTables.LIBRARIAN_GIFT)
      .put(VillagerProfession.MASON, BuiltInLootTables.MASON_GIFT)
      .put(VillagerProfession.SHEPHERD, BuiltInLootTables.SHEPHERD_GIFT)
      .put(VillagerProfession.TOOLSMITH, BuiltInLootTables.TOOLSMITH_GIFT)
      .put(VillagerProfession.WEAPONSMITH, BuiltInLootTables.WEAPONSMITH_GIFT)
      .build();
   private static final float SPEED_MODIFIER = 0.5F;
   private int timeUntilNextGift = 600;
   private boolean giftGivenDuringThisRun;
   private long timeSinceStart;

   public GiveGiftToHero(int p_22992_) {
      super(
         ImmutableMap.of(
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.INTERACTION_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryStatus.VALUE_PRESENT
         ),
         p_22992_
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel p_23003_, Villager p_454124_) {
      if (!this.isHeroVisible(p_454124_)) {
         return false;
      } else if (this.timeUntilNextGift > 0) {
         this.timeUntilNextGift--;
         return false;
      } else {
         return true;
      }
   }

   protected void start(ServerLevel p_23006_, Villager p_453367_, long p_23008_) {
      this.giftGivenDuringThisRun = false;
      this.timeSinceStart = p_23008_;
      Player player = this.getNearestTargetableHero(p_453367_).get();
      p_453367_.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
      BehaviorUtils.lookAtEntity(p_453367_, player);
   }

   protected boolean canStillUse(ServerLevel p_23026_, Villager p_454970_, long p_23028_) {
      return this.isHeroVisible(p_454970_) && !this.giftGivenDuringThisRun;
   }

   protected void tick(ServerLevel p_23036_, Villager p_458481_, long p_23038_) {
      Player player = this.getNearestTargetableHero(p_458481_).get();
      BehaviorUtils.lookAtEntity(p_458481_, player);
      if (this.isWithinThrowingDistance(p_458481_, player)) {
         if (p_23038_ - this.timeSinceStart > 20L) {
            this.throwGift(p_23036_, p_458481_, player);
            this.giftGivenDuringThisRun = true;
         }
      } else {
         BehaviorUtils.setWalkAndLookTargetMemories(p_458481_, player, 0.5F, 5);
      }
   }

   protected void stop(ServerLevel p_23046_, Villager p_453448_, long p_23048_) {
      this.timeUntilNextGift = calculateTimeUntilNextGift(p_23046_);
      p_453448_.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      p_453448_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      p_453448_.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   private void throwGift(ServerLevel p_361224_, Villager p_460045_, LivingEntity p_23013_) {
      p_460045_.dropFromGiftLootTable(
         p_361224_, getLootTableToThrow(p_460045_), (p_449467_, p_449468_) -> BehaviorUtils.throwItem(p_460045_, p_449468_, p_23013_.position())
      );
   }

   private static ResourceKey<LootTable> getLootTableToThrow(Villager p_460392_) {
      if (p_460392_.isBaby()) {
         return BuiltInLootTables.BABY_VILLAGER_GIFT;
      }

      Optional<ResourceKey<VillagerProfession>> optional = p_460392_.getVillagerData().profession().unwrapKey();
      return optional.isEmpty() ? BuiltInLootTables.UNEMPLOYED_GIFT : GIFTS.getOrDefault(optional.get(), BuiltInLootTables.UNEMPLOYED_GIFT);
   }

   private boolean isHeroVisible(Villager p_458700_) {
      return this.getNearestTargetableHero(p_458700_).isPresent();
   }

   private Optional<Player> getNearestTargetableHero(Villager p_457458_) {
      return p_457458_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(this::isHero);
   }

   private boolean isHero(Player p_23018_) {
      return p_23018_.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
   }

   private boolean isWithinThrowingDistance(Villager p_456641_, Player p_23016_) {
      BlockPos blockpos = p_23016_.blockPosition();
      BlockPos blockpos1 = p_456641_.blockPosition();
      return blockpos1.closerThan(blockpos, 5.0);
   }

   private static int calculateTimeUntilNextGift(ServerLevel p_22994_) {
      return 600 + p_22994_.random.nextInt(6001);
   }
}
