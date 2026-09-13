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

   public GiveGiftToHero(final int timeout) {
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
         timeout
      );
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Villager body) {
      if (!this.isHeroVisible(body)) {
         return false;
      } else if (this.timeUntilNextGift > 0) {
         this.timeUntilNextGift--;
         return false;
      } else {
         return true;
      }
   }

   protected void start(final ServerLevel level, final Villager body, final long timestamp) {
      this.giftGivenDuringThisRun = false;
      this.timeSinceStart = timestamp;
      Player player = this.getNearestTargetableHero(body).get();
      body.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
      BehaviorUtils.lookAtEntity(body, player);
   }

   protected boolean canStillUse(final ServerLevel level, final Villager body, final long timestamp) {
      return this.isHeroVisible(body) && !this.giftGivenDuringThisRun;
   }

   protected void tick(final ServerLevel level, final Villager villager, final long timestamp) {
      Player player = this.getNearestTargetableHero(villager).get();
      BehaviorUtils.lookAtEntity(villager, player);
      if (this.isWithinThrowingDistance(villager, player)) {
         if (timestamp - this.timeSinceStart > 20L) {
            this.throwGift(level, villager, player);
            this.giftGivenDuringThisRun = true;
         }
      } else {
         BehaviorUtils.setWalkAndLookTargetMemories(villager, player, 0.5F, 5);
      }
   }

   protected void stop(final ServerLevel level, final Villager body, final long timestamp) {
      this.timeUntilNextGift = calculateTimeUntilNextGift(level);
      body.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   private void throwGift(final ServerLevel level, final Villager villager, final LivingEntity target) {
      villager.dropFromGiftLootTable(level, getLootTableToThrow(villager), (l, itemStack) -> BehaviorUtils.throwItem(villager, itemStack, target.position()));
   }

   private static ResourceKey<LootTable> getLootTableToThrow(final Villager villager) {
      if (villager.isBaby()) {
         return BuiltInLootTables.BABY_VILLAGER_GIFT;
      }

      Optional<ResourceKey<VillagerProfession>> profession = villager.getVillagerData().profession().unwrapKey();
      return profession.isEmpty() ? BuiltInLootTables.UNEMPLOYED_GIFT : GIFTS.getOrDefault(profession.get(), BuiltInLootTables.UNEMPLOYED_GIFT);
   }

   private boolean isHeroVisible(final Villager body) {
      return this.getNearestTargetableHero(body).isPresent();
   }

   private Optional<Player> getNearestTargetableHero(final Villager body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(this::isHero);
   }

   private boolean isHero(final Player player) {
      return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
   }

   private boolean isWithinThrowingDistance(final Villager villager, final Player player) {
      BlockPos playerPos = player.blockPosition();
      BlockPos villagerPos = villager.blockPosition();
      return villagerPos.closerThan(playerPos, 5.0);
   }

   private static int calculateTimeUntilNextGift(final ServerLevel level) {
      return 600 + level.getRandom().nextInt(6001);
   }
}
