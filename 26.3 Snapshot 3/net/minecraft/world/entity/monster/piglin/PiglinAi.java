package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.SpearApproach;
import net.minecraft.world.entity.ai.behavior.SpearAttack;
import net.minecraft.world.entity.ai.behavior.SpearRetreat;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
   public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
   public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
   public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
   private static final int PLAYER_ANGER_RANGE = 16;
   private static final int ANGER_DURATION = 600;
   private static final int ADMIRE_DURATION = 119;
   private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
   private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
   private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
   private static final int CELEBRATION_TIME = 300;
   public static final int MAX_TIME_BETWEEN_HUNTS = 120;
   protected static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
   private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
   private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
   private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
   private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
   private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
   private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
   private static final int MELEE_ATTACK_COOLDOWN = 20;
   private static final int EAT_COOLDOWN = 200;
   private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
   private static final int MAX_LOOK_DIST = 8;
   private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
   private static final int INTERACTION_RANGE = 8;
   private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
   private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
   private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
   private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
   private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
   private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
   private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
   private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

   public static List<ActivityData<Piglin>> getActivities(final Piglin piglin) {
      return List.of(
         initCoreActivity(),
         initIdleActivity(),
         initAdmireItemActivity(),
         initFightActivity(piglin),
         initCelebrateActivity(),
         initRetreatActivity(),
         initRideHoglinActivity()
      );
   }

   protected static void initMemories(final Piglin body, final RandomSource random) {
      int delayUntilFirstHunt = TIME_BETWEEN_HUNTS.sample(random);
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, delayUntilFirstHunt);
   }

   private static ActivityData<Piglin> initCoreActivity() {
      return ActivityData.create(
         Activity.CORE,
         0,
         ImmutableList.of(
            new LookAtTargetSink(45, 90),
            new MoveToTargetSink(),
            InteractWithDoor.create(),
            babyAvoidNemesis(),
            avoidZombified(),
            StopHoldingItemIfNoLongerAdmiring.create(),
            StartAdmiringItemIfSeen.create(119),
            StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance),
            StopBeingAngryIfTargetDead.create()
         )
      );
   }

   private static ActivityData<Piglin> initIdleActivity() {
      return ActivityData.create(
         Activity.IDLE,
         10,
         ImmutableList.of(
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
            StartAttacking.create((level, piglin) -> piglin.isAdult(), PiglinAi::findNearestValidAttackTarget),
            BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create()),
            avoidRepellent(),
            babySometimesRideBabyHoglin(),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            SetLookAndInteract.create(EntityTypes.PLAYER, 4)
         )
      );
   }

   private static ActivityData<Piglin> initFightActivity(final Piglin body) {
      return ActivityData.create(
         Activity.FIGHT,
         10,
         ImmutableList.of(
            StopAttackingIfTargetInvalid.create((level, target) -> !isNearestValidAttackTarget(level, body, target)),
            BehaviorBuilder.triggerIf(PiglinAi::hasCrossbow, BackUpIfTooClose.create(5, 0.75F)),
            SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
            new SpearApproach(1.0, 10.0F),
            new SpearAttack(1.0, 1.0, 2.0F),
            new SpearRetreat(1.0),
            MeleeAttack.create(20),
            new CrossbowAttack(),
            RememberIfHoglinWasKilled.create(),
            EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)
         ),
         MemoryModuleType.ATTACK_TARGET
      );
   }

   private static ActivityData<Piglin> initCelebrateActivity() {
      return ActivityData.create(
         Activity.CELEBRATE,
         10,
         ImmutableList.of(
            avoidRepellent(),
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
            StartAttacking.create((level, piglin) -> piglin.isAdult(), PiglinAi::findNearestValidAttackTarget),
            BehaviorBuilder.triggerIf(piglin -> !piglin.isDancing(), GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 2, 1.0F)),
            BehaviorBuilder.triggerIf(Piglin::isDancing, GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 4, 0.6F)),
            new RunOne(
               ImmutableList.of(
                  Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0F), 1),
                  Pair.of(RandomStroll.stroll(0.6F, 2, 1), 1),
                  Pair.of(new DoNothing(10, 20), 1)
               )
            )
         ),
         MemoryModuleType.CELEBRATE_LOCATION
      );
   }

   private static ActivityData<Piglin> initAdmireItemActivity() {
      return ActivityData.create(
         Activity.ADMIRE_ITEM,
         10,
         ImmutableList.of(
            GoToWantedItem.create(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0F, true, 9),
            StopAdmiringIfItemTooFarAway.create(9),
            StopAdmiringIfTiredOfTryingToReachItem.create(200, 200)
         ),
         MemoryModuleType.ADMIRING_ITEM
      );
   }

   private static ActivityData<Piglin> initRetreatActivity() {
      return ActivityData.create(
         Activity.AVOID,
         10,
         ImmutableList.of(
            SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true),
            createIdleLookBehaviors(),
            createIdleMovementBehaviors(),
            EraseMemoryIf.create(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
         ),
         MemoryModuleType.AVOID_TARGET
      );
   }

   private static ActivityData<Piglin> initRideHoglinActivity() {
      return ActivityData.create(
         Activity.RIDE,
         10,
         ImmutableList.of(
            Mount.create(0.8F),
            SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 8.0F),
            BehaviorBuilder.sequence(
               BehaviorBuilder.triggerIf(Entity::isPassenger),
               TriggerGate.triggerOneShuffled(
                  ImmutableList.builder().addAll(createLookBehaviors()).add(Pair.of(BehaviorBuilder.triggerIf(e -> true), 1)).build()
               )
            ),
            DismountOrSkipMounting.create(8, PiglinAi::wantsToStopRiding)
         ),
         MemoryModuleType.RIDE_TARGET
      );
   }

   private static ImmutableList<Pair<OneShot<LivingEntity>, Integer>> createLookBehaviors() {
      return ImmutableList.of(
         Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0F), 1),
         Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0F), 1),
         Pair.of(SetEntityLookTarget.create(8.0F), 1)
      );
   }

   private static RunOne<LivingEntity> createIdleLookBehaviors() {
      return new RunOne<>(ImmutableList.builder().addAll(createLookBehaviors()).add(Pair.of(new DoNothing(30, 60), 1)).build());
   }

   private static RunOne<Piglin> createIdleMovementBehaviors() {
      return new RunOne<>(
         ImmutableList.of(
            Pair.of(RandomStroll.stroll(0.6F), 2),
            Pair.of(InteractWith.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
            Pair.of(BehaviorBuilder.triggerIf(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, SetWalkTargetFromLookTarget.create(0.6F, 3)), 2),
            Pair.of(new DoNothing(30, 60), 1)
         )
      );
   }

   private static BehaviorControl<PathfinderMob> avoidRepellent() {
      return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
   }

   private static BehaviorControl<Piglin> babyAvoidNemesis() {
      return CopyMemoryWithExpiry.create(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
   }

   private static BehaviorControl<Piglin> avoidZombified() {
      return CopyMemoryWithExpiry.create(
         PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION
      );
   }

   protected static void updateActivity(final Piglin body) {
      Brain<Piglin> brain = body.getBrain();
      Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
      brain.setActiveActivityToFirstValid(
         ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE)
      );
      Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
      if (oldActivity != newActivity) {
         getSoundForCurrentActivity(body).ifPresent(body::makeSound);
      }

      body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
      if (!brain.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(body)) {
         body.stopRiding();
      }

      if (!brain.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
         brain.eraseMemory(MemoryModuleType.DANCING);
      }

      body.setDancing(brain.hasMemoryValue(MemoryModuleType.DANCING));
   }

   private static boolean isBabyRidingBaby(final Piglin body) {
      if (!body.isBaby()) {
         return false;
      }

      Entity vehicle = body.getVehicle();
      return vehicle instanceof Piglin riddenPiglin && riddenPiglin.isBaby() || vehicle instanceof Hoglin riddenHoglin && riddenHoglin.isBaby();
   }

   protected static void pickUpItem(final ServerLevel level, final Piglin body, final ItemEntity itemEntity) {
      stopWalking(body);
      ItemStack taken;
      if (itemEntity.getItem().is(Items.GOLD_NUGGET)) {
         body.take(itemEntity, itemEntity.getItem().getCount());
         taken = itemEntity.getItem();
         itemEntity.discard();
      } else {
         body.take(itemEntity, 1);
         taken = removeOneItemFromItemEntity(itemEntity);
      }

      if (isLovedItem(taken)) {
         body.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
         holdInOffhand(level, body, taken);
         admireGoldItem(body);
      } else if (isFood(taken) && !hasEatenRecently(body)) {
         eat(body);
      } else {
         boolean itemEquipped = !body.equipItemIfPossible(level, taken).equals(ItemStack.EMPTY);
         if (!itemEquipped) {
            putInInventory(body, taken);
         }
      }
   }

   private static void holdInOffhand(final ServerLevel level, final Piglin body, final ItemStack itemStack) {
      if (isHoldingItemInOffHand(body)) {
         body.spawnAtLocation(level, body.getItemInHand(InteractionHand.OFF_HAND));
      }

      body.holdInOffHand(itemStack);
   }

   private static ItemStack removeOneItemFromItemEntity(final ItemEntity itemEntity) {
      ItemStack sourceStack = itemEntity.getItem();
      ItemStack removedStack = sourceStack.split(1);
      if (sourceStack.isEmpty()) {
         itemEntity.discard();
      } else {
         itemEntity.setItem(sourceStack);
      }

      return removedStack;
   }

   protected static void stopHoldingOffHandItem(final ServerLevel level, final Piglin body, final boolean barteringEnabled) {
      ItemStack itemStack = body.getItemInHand(InteractionHand.OFF_HAND);
      body.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      if (body.isAdult()) {
         boolean barterCurrency = isBarterCurrency(itemStack);
         if (barteringEnabled && barterCurrency) {
            throwItems(body, getBarterResponseItems(body));
         } else if (!barterCurrency) {
            boolean equipped = !body.equipItemIfPossible(level, itemStack).isEmpty();
            if (!equipped) {
               putInInventory(body, itemStack);
            }
         }
      } else {
         boolean equipped = !body.equipItemIfPossible(level, itemStack).isEmpty();
         if (!equipped) {
            ItemStack mainHandItem = body.getMainHandItem();
            if (isLovedItem(mainHandItem)) {
               putInInventory(body, mainHandItem);
            } else {
               throwItems(body, Collections.singletonList(mainHandItem));
            }

            body.holdInMainHand(itemStack);
         }
      }
   }

   protected static void cancelAdmiring(final ServerLevel level, final Piglin body) {
      if (isAdmiringItem(body) && !body.getOffhandItem().isEmpty()) {
         body.spawnAtLocation(level, body.getOffhandItem());
         body.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      }
   }

   private static void putInInventory(final Piglin body, final ItemStack itemStack) {
      ItemStack stuffThatCouldntFitInMyInventory = body.addToInventory(itemStack);
      throwItemsTowardRandomPos(body, Collections.singletonList(stuffThatCouldntFitInMyInventory));
   }

   private static void throwItems(final Piglin body, final List<ItemStack> itemStacks) {
      Optional<Player> player = body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
      if (player.isPresent()) {
         throwItemsTowardPlayer(body, player.get(), itemStacks);
      } else {
         throwItemsTowardRandomPos(body, itemStacks);
      }
   }

   private static void throwItemsTowardRandomPos(final Piglin body, final List<ItemStack> itemStacks) {
      throwItemsTowardPos(body, itemStacks, getRandomNearbyPos(body));
   }

   private static void throwItemsTowardPlayer(final Piglin body, final Player player, final List<ItemStack> itemStacks) {
      throwItemsTowardPos(body, itemStacks, player.position());
   }

   private static void throwItemsTowardPos(final Piglin body, final List<ItemStack> itemStacks, final Vec3 targetPos) {
      if (!itemStacks.isEmpty()) {
         body.swing(InteractionHand.OFF_HAND);

         for (ItemStack itemStack : itemStacks) {
            BehaviorUtils.throwItem(body, itemStack, targetPos.add(0.0, 1.0, 0.0));
         }
      }
   }

   private static List<ItemStack> getBarterResponseItems(final Piglin body) {
      LootTable lootTable = body.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
      List<ItemStack> items = lootTable.getRandomItems(
         new LootParams.Builder((ServerLevel)body.level()).withParameter(LootContextParams.THIS_ENTITY, body).create(LootContextParamSets.PIGLIN_BARTER)
      );
      return items;
   }

   private static boolean wantsToDance(final LivingEntity body, final LivingEntity killedTarget) {
      return !killedTarget.is(EntityTypes.HOGLIN) ? false : RandomSource.createThreadLocalInstance(body.level().getGameTime()).nextFloat() < 0.1F;
   }

   protected static boolean wantsToPickup(final Piglin body, final ItemStack itemStack) {
      if (body.isBaby() && itemStack.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
         return false;
      } else if (itemStack.is(ItemTags.PIGLIN_REPELLENTS)) {
         return false;
      } else if (isAdmiringDisabled(body) && body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
         return false;
      } else if (isBarterCurrency(itemStack)) {
         return isNotHoldingLovedItemInOffHand(body);
      } else {
         boolean hasSpace = body.canAddToInventory(itemStack);
         if (itemStack.is(Items.GOLD_NUGGET)) {
            return hasSpace;
         } else if (isFood(itemStack)) {
            return !hasEatenRecently(body) && hasSpace;
         } else {
            return !isLovedItem(itemStack) ? body.canReplaceCurrentItem(itemStack) : isNotHoldingLovedItemInOffHand(body) && hasSpace;
         }
      }
   }

   protected static boolean isLovedItem(final ItemStack itemStack) {
      return itemStack.is(ItemTags.PIGLIN_LOVED);
   }

   private static boolean wantsToStopRiding(final Piglin body, final Entity entityBeingRidden) {
      return !(entityBeingRidden instanceof Mob mobBeingRidden)
         ? false
         : !mobBeingRidden.isBaby()
            || !mobBeingRidden.isAlive()
            || wasHurtRecently(body)
            || wasHurtRecently(mobBeingRidden)
            || mobBeingRidden instanceof Piglin && mobBeingRidden.getVehicle() == null;
   }

   private static boolean isNearestValidAttackTarget(final ServerLevel level, final Piglin body, final LivingEntity target) {
      return findNearestValidAttackTarget(level, body).filter(nearestValidTarget -> nearestValidTarget == target).isPresent();
   }

   private static boolean isNearZombified(final Piglin body) {
      Brain<Piglin> brain = body.getBrain();
      if (brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
         LivingEntity zombified = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
         return body.closerThan(zombified, 6.0);
      } else {
         return false;
      }
   }

   private static Optional<? extends LivingEntity> findNearestValidAttackTarget(final ServerLevel level, final Piglin body) {
      Brain<Piglin> brain = body.getBrain();
      if (isNearZombified(body)) {
         return Optional.empty();
      }

      Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
      if (angryAt.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(level, body, angryAt.get())) {
         return angryAt;
      }

      if (brain.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER)) {
         Optional<Player> player = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
         if (player.isPresent()) {
            return player;
         }
      }

      Optional<Mob> nemesis = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
      if (nemesis.isPresent()) {
         return nemesis;
      }

      Optional<Player> playerNotWearingGold = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
      return playerNotWearingGold.isPresent() && Sensor.isEntityAttackable(level, body, playerNotWearingGold.get()) ? playerNotWearingGold : Optional.empty();
   }

   public static void angerNearbyPiglins(final ServerLevel level, final Player player, final boolean onlyIfTheySeeThePlayer) {
      List<Piglin> nearbyPiglins = player.level().getEntitiesOfClass(Piglin.class, player.getBoundingBox().inflate(16.0));
      nearbyPiglins.stream().filter(PiglinAi::isIdle).filter(piglin -> !onlyIfTheySeeThePlayer || BehaviorUtils.canSee(piglin, player)).forEach(piglin -> {
         if (level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) {
            setAngerTargetToNearestTargetablePlayerIfFound(level, piglin, player);
         } else {
            setAngerTarget(level, piglin, player);
         }
      });
   }

   public static InteractionResult mobInteract(final ServerLevel level, final Piglin body, final Player player, final InteractionHand hand) {
      ItemStack playerHeldItemStack = player.getItemInHand(hand);
      if (canAdmire(body, playerHeldItemStack)) {
         ItemStack taken = playerHeldItemStack.consumeAndReturn(1, player);
         holdInOffhand(level, body, taken);
         admireGoldItem(body);
         stopWalking(body);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected static boolean canAdmire(final Piglin body, final ItemStack playerHeldItemStack) {
      return !isAdmiringDisabled(body) && !isAdmiringItem(body) && body.isAdult() && isBarterCurrency(playerHeldItemStack);
   }

   protected static void wasHurtBy(final ServerLevel level, final Piglin body, final LivingEntity attacker) {
      if (!(attacker instanceof Piglin)) {
         if (isHoldingItemInOffHand(body)) {
            stopHoldingOffHandItem(level, body, false);
         }

         Brain<Piglin> brain = body.getBrain();
         brain.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
         brain.eraseMemory(MemoryModuleType.DANCING);
         brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
         if (attacker instanceof Player) {
            brain.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
         }

         getAvoidTarget(body).ifPresent(avoidTarget -> {
            if (avoidTarget.getType() != attacker.getType()) {
               brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
            }
         });
         if (body.isBaby()) {
            brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, attacker, 100L);
            if (Sensor.isEntityAttackableIgnoringLineOfSight(level, body, attacker)) {
               broadcastAngerTarget(level, body, attacker);
            }
         } else if (attacker.is(EntityTypes.HOGLIN) && hoglinsOutnumberPiglins(body)) {
            setAvoidTargetAndDontHuntForAWhile(body, attacker);
            broadcastRetreat(body, attacker);
         } else {
            maybeRetaliate(level, body, attacker);
         }
      }
   }

   protected static void maybeRetaliate(final ServerLevel level, final AbstractPiglin body, final LivingEntity attacker) {
      if (!body.getBrain().isActive(Activity.AVOID)) {
         if (Sensor.isEntityAttackableIgnoringLineOfSight(level, body, attacker)) {
            if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(body, attacker, 4.0)) {
               if (attacker.is(EntityTypes.PLAYER) && level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) {
                  setAngerTargetToNearestTargetablePlayerIfFound(level, body, attacker);
                  broadcastUniversalAnger(level, body);
               } else {
                  setAngerTarget(level, body, attacker);
                  broadcastAngerTarget(level, body, attacker);
               }
            }
         }
      }
   }

   public static Optional<SoundEvent> getSoundForCurrentActivity(final Piglin body) {
      return body.getBrain().getActiveNonCoreActivity().map(activity -> getSoundForActivity(body, activity));
   }

   private static SoundEvent getSoundForActivity(final Piglin body, final Activity activity) {
      if (activity == Activity.FIGHT) {
         return SoundEvents.PIGLIN_ANGRY;
      } else if (body.isConverting()) {
         return SoundEvents.PIGLIN_RETREAT;
      } else if (activity == Activity.AVOID && isNearAvoidTarget(body)) {
         return SoundEvents.PIGLIN_RETREAT;
      } else if (activity == Activity.ADMIRE_ITEM) {
         return SoundEvents.PIGLIN_ADMIRING_ITEM;
      } else if (activity == Activity.CELEBRATE) {
         return SoundEvents.PIGLIN_CELEBRATE;
      } else if (seesPlayerHoldingLovedItem(body)) {
         return SoundEvents.PIGLIN_JEALOUS;
      } else {
         return isNearRepellent(body) ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT;
      }
   }

   private static boolean isNearAvoidTarget(final Piglin body) {
      Brain<Piglin> brain = body.getBrain();
      return !brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? false : brain.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(body, 12.0);
   }

   protected static List<AbstractPiglin> getVisibleAdultPiglins(final Piglin body) {
      return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
   }

   private static List<AbstractPiglin> getAdultPiglins(final AbstractPiglin body) {
      return body.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
   }

   public static boolean isWearingSafeArmor(final LivingEntity livingEntity) {
      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         if (livingEntity.getItemBySlot(slot).is(ItemTags.PIGLIN_SAFE_ARMOR)) {
            return true;
         }
      }

      return false;
   }

   private static void stopWalking(final Piglin body) {
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getNavigation().stop();
   }

   private static BehaviorControl<LivingEntity> babySometimesRideBabyHoglin() {
      SetEntityLookTargetSometimes.Ticker ticker = new SetEntityLookTargetSometimes.Ticker(RIDE_START_INTERVAL);
      return CopyMemoryWithExpiry.create(
         e -> e.isBaby() && ticker.tickDownAndCheck(e.level().getRandom()),
         MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
         MemoryModuleType.RIDE_TARGET,
         RIDE_DURATION
      );
   }

   protected static void broadcastAngerTarget(final ServerLevel level, final AbstractPiglin body, final LivingEntity target) {
      getAdultPiglins(body).forEach(piglin -> {
         if (!(target instanceof Hoglin hoglin && (!piglin.canHunt() || !hoglin.canBeHunted()))) {
            setAngerTargetIfCloserThanCurrent(level, piglin, target);
         }
      });
   }

   protected static void broadcastUniversalAnger(final ServerLevel level, final AbstractPiglin body) {
      getAdultPiglins(body).forEach(piglin -> getNearestVisibleTargetablePlayer(piglin).ifPresent(player -> setAngerTarget(level, piglin, player)));
   }

   protected static void setAngerTarget(final ServerLevel level, final AbstractPiglin body, final LivingEntity target) {
      if (Sensor.isEntityAttackableIgnoringLineOfSight(level, body, target)) {
         body.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         body.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
         if (target.is(EntityTypes.HOGLIN) && body.canHunt()) {
            dontKillAnyMoreHoglinsForAWhile(body);
         }

         if (target.is(EntityTypes.PLAYER) && level.getGameRules().get(GameRules.UNIVERSAL_ANGER)) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
         }
      }
   }

   private static void setAngerTargetToNearestTargetablePlayerIfFound(
      final ServerLevel level, final AbstractPiglin body, final LivingEntity targetIfNoPlayerFound
   ) {
      Optional<Player> nearestPlayer = getNearestVisibleTargetablePlayer(body);
      if (nearestPlayer.isPresent()) {
         setAngerTarget(level, body, nearestPlayer.get());
      } else {
         setAngerTarget(level, body, targetIfNoPlayerFound);
      }
   }

   private static void setAngerTargetIfCloserThanCurrent(final ServerLevel level, final AbstractPiglin body, final LivingEntity newTarget) {
      Optional<LivingEntity> currentTarget = getAngerTarget(body);
      LivingEntity nearest = BehaviorUtils.getNearestTarget(body, currentTarget, newTarget);
      if (!currentTarget.isPresent() || currentTarget.get() != nearest) {
         setAngerTarget(level, body, nearest);
      }
   }

   private static Optional<LivingEntity> getAngerTarget(final AbstractPiglin body) {
      return BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
   }

   public static Optional<LivingEntity> getAvoidTarget(final Piglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? body.getBrain().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
   }

   public static Optional<Player> getNearestVisibleTargetablePlayer(final AbstractPiglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
         ? body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER)
         : Optional.empty();
   }

   private static void broadcastRetreat(final Piglin body, final LivingEntity target) {
      getVisibleAdultPiglins(body).forEach(abstractPiglin -> {
         if (abstractPiglin instanceof Piglin piglin) {
            retreatFromNearestTarget(piglin, target);
         }
      });
   }

   private static void retreatFromNearestTarget(final Piglin body, final LivingEntity newAvoidTarget) {
      Brain<Piglin> brain = body.getBrain();
      LivingEntity nearest = newAvoidTarget;
      nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.AVOID_TARGET), nearest);
      nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
      setAvoidTargetAndDontHuntForAWhile(body, nearest);
   }

   private static boolean wantsToStopFleeing(final Piglin body) {
      Brain<Piglin> brain = body.getBrain();
      if (!brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
         return true;
      } else {
         LivingEntity avoidedEntity = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
         if (avoidedEntity.is(EntityTypes.HOGLIN)) {
            return piglinsEqualOrOutnumberHoglins(body);
         } else {
            return isZombified(avoidedEntity) ? !brain.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, avoidedEntity) : false;
         }
      }
   }

   private static boolean piglinsEqualOrOutnumberHoglins(final Piglin body) {
      return !hoglinsOutnumberPiglins(body);
   }

   private static boolean hoglinsOutnumberPiglins(final Piglin body) {
      int piglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
      int hoglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
      return hoglinCount > piglinCount;
   }

   private static void setAvoidTargetAndDontHuntForAWhile(final Piglin body, final LivingEntity target) {
      body.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
      body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, RETREAT_DURATION.sample(body.level().getRandom()));
      dontKillAnyMoreHoglinsForAWhile(body);
   }

   protected static void dontKillAnyMoreHoglinsForAWhile(final AbstractPiglin body) {
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, TIME_BETWEEN_HUNTS.sample(body.level().getRandom()));
   }

   private static void eat(final Piglin body) {
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
   }

   private static Vec3 getRandomNearbyPos(final Piglin body) {
      Vec3 targetVec = LandRandomPos.getPos(body, 4, 2);
      return targetVec == null ? body.position() : targetVec;
   }

   private static boolean hasEatenRecently(final Piglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
   }

   protected static boolean isIdle(final AbstractPiglin body) {
      return body.getBrain().isActive(Activity.IDLE);
   }

   private static boolean hasCrossbow(final LivingEntity body) {
      return body.isHolding(Items.CROSSBOW);
   }

   private static void admireGoldItem(final LivingEntity body) {
      body.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 119L);
   }

   private static boolean isAdmiringItem(final Piglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
   }

   private static boolean isBarterCurrency(final ItemStack itemStack) {
      return itemStack.is(BARTERING_ITEM);
   }

   private static boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.PIGLIN_FOOD);
   }

   private static boolean isNearRepellent(final Piglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
   }

   private static boolean seesPlayerHoldingLovedItem(final LivingEntity body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
   }

   private static boolean doesntSeeAnyPlayerHoldingLovedItem(final LivingEntity body) {
      return !seesPlayerHoldingLovedItem(body);
   }

   public static boolean isPlayerHoldingLovedItem(final LivingEntity entity) {
      return entity.is(EntityTypes.PLAYER) && entity.isHolding(PiglinAi::isLovedItem);
   }

   private static boolean isAdmiringDisabled(final Piglin body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
   }

   private static boolean wasHurtRecently(final LivingEntity body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
   }

   private static boolean isHoldingItemInOffHand(final Piglin body) {
      return !body.getOffhandItem().isEmpty();
   }

   private static boolean isNotHoldingLovedItemInOffHand(final Piglin body) {
      return body.getOffhandItem().isEmpty() || !isLovedItem(body.getOffhandItem());
   }

   public static boolean isZombified(final Entity entity) {
      return entity.is(EntityTypes.ZOMBIFIED_PIGLIN) || entity.is(EntityTypes.ZOGLIN);
   }

   public static List<AbstractPiglin> findNearbyAdultPiglins(final Brain<?> brain) {
      List<LivingEntity> livingEntities = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
      List<AbstractPiglin> adultPiglins = new ArrayList<>();

      for (LivingEntity entity : livingEntities) {
         if (entity instanceof AbstractPiglin piglin && piglin.isAdult()) {
            adultPiglins.add(piglin);
         }
      }

      return adultPiglins;
   }
}
