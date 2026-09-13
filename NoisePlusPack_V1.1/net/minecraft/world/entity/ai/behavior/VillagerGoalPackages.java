package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class VillagerGoalPackages {
   private static final float STROLL_SPEED_MODIFIER = 0.4F;
   public static final int INTERACT_DIST_SQR = 5;
   public static final int INTERACT_WALKUP_DIST = 2;
   public static final float INTERACT_SPEED_MODIFIER = 0.5F;

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getCorePackage(Holder<VillagerProfession> p_392340_, float p_24587_) {
      return ImmutableList.of(
         Pair.of(0, new Swim(0.8F)),
         Pair.of(0, InteractWithDoor.create()),
         Pair.of(0, new LookAtTargetSink(45, 90)),
         Pair.of(0, new VillagerPanicTrigger()),
         Pair.of(0, WakeUp.create()),
         Pair.of(0, ReactToBell.create()),
         Pair.of(0, SetRaidStatus.create()),
         Pair.of(0, ValidateNearbyPoi.create(p_392340_.value().heldJobSite(), MemoryModuleType.JOB_SITE)),
         Pair.of(0, ValidateNearbyPoi.create(p_392340_.value().acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
         Pair.of(1, new MoveToTargetSink()),
         Pair.of(2, PoiCompetitorScan.create()),
         Pair.of(3, new LookAndFollowTradingPlayerSink(p_24587_)),
         new Pair[]{
            Pair.of(5, GoToWantedItem.create(p_24587_, false, 4)),
            Pair.of(
               6,
               AcquirePoi.create(
                  p_392340_.value().acquirableJobSite(),
                  MemoryModuleType.JOB_SITE,
                  MemoryModuleType.POTENTIAL_JOB_SITE,
                  true,
                  Optional.empty(),
                  (p_375063_, p_375064_) -> true
               )
            ),
            Pair.of(7, new GoToPotentialJobSite(p_24587_)),
            Pair.of(8, YieldJobSite.create(p_24587_)),
            Pair.of(
               10,
               AcquirePoi.create(
                  p_217499_ -> p_217499_.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte)14), VillagerGoalPackages::validateBedPoi
               )
            ),
            Pair.of(10, AcquirePoi.create(p_217497_ -> p_217497_.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte)14))),
            Pair.of(10, AssignProfessionFromJobSite.create()),
            Pair.of(10, ResetProfession.create())
         }
      );
   }

   private static boolean validateBedPoi(ServerLevel p_376537_, BlockPos p_376729_) {
      BlockState blockstate = p_376537_.getBlockState(p_376729_);
      return blockstate.is(BlockTags.BEDS) && !blockstate.getValue(BedBlock.OCCUPIED);
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getWorkPackage(Holder<VillagerProfession> p_394996_, float p_24591_) {
      WorkAtPoi workatpoi;
      if (p_394996_.is(VillagerProfession.FARMER)) {
         workatpoi = new WorkAtComposter();
      } else {
         workatpoi = new WorkAtPoi();
      }

      return ImmutableList.of(
         getMinimalLookBehavior(),
         Pair.of(
            5,
            new RunOne(
               ImmutableList.of(
                  Pair.of(workatpoi, 7),
                  Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                  Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5),
                  Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, p_24591_, 1, 6, MemoryModuleType.JOB_SITE), 5),
                  Pair.of(new HarvestFarmland(), p_394996_.is(VillagerProfession.FARMER) ? 2 : 5),
                  Pair.of(new UseBonemeal(), p_394996_.is(VillagerProfession.FARMER) ? 4 : 7)
               )
            )
         ),
         Pair.of(10, new ShowTradesToPlayer(400, 1600)),
         Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
         Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, p_24591_, 9, 100, 1200)),
         Pair.of(3, new GiveGiftToHero(100)),
         Pair.of(99, UpdateActivityFromSchedule.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPlayPackage(float p_24584_) {
      return ImmutableList.of(
         Pair.of(0, new MoveToTargetSink(80, 120)),
         getFullLookBehavior(),
         Pair.of(5, PlayTagWithOtherKids.create()),
         Pair.of(
            5,
            new RunOne(
               ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_ABSENT),
               ImmutableList.of(
                  Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, p_24584_, 2), 2),
                  Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, p_24584_, 2), 1),
                  Pair.of(VillageBoundRandomStroll.create(p_24584_), 1),
                  Pair.of(SetWalkTargetFromLookTarget.create(p_24584_, 2), 1),
                  Pair.of(new JumpOnBed(p_24584_), 2),
                  Pair.of(new DoNothing(20, 40), 2)
               )
            )
         ),
         Pair.of(99, UpdateActivityFromSchedule.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRestPackage(Holder<VillagerProfession> p_396136_, float p_24594_) {
      return ImmutableList.of(
         Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, p_24594_, 1, 150, 1200)),
         Pair.of(3, ValidateNearbyPoi.create(p_217495_ -> p_217495_.is(PoiTypes.HOME), MemoryModuleType.HOME)),
         Pair.of(3, new SleepInBed()),
         Pair.of(
            5,
            new RunOne(
               ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT),
               ImmutableList.of(
                  Pair.of(SetClosestHomeAsWalkTarget.create(p_24594_), 1),
                  Pair.of(InsideBrownianWalk.create(p_24594_), 4),
                  Pair.of(GoToClosestVillage.create(p_24594_, 4), 2),
                  Pair.of(new DoNothing(20, 40), 2)
               )
            )
         ),
         getMinimalLookBehavior(),
         Pair.of(99, UpdateActivityFromSchedule.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getMeetPackage(Holder<VillagerProfession> p_398018_, float p_24597_) {
      return ImmutableList.of(
         Pair.of(
            2,
            TriggerGate.triggerOneShuffled(
               ImmutableList.of(Pair.of(StrollAroundPoi.create(MemoryModuleType.MEETING_POINT, 0.4F, 40), 2), Pair.of(SocializeAtBell.create(), 2))
            )
         ),
         Pair.of(10, new ShowTradesToPlayer(400, 1600)),
         Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
         Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, p_24597_, 6, 100, 200)),
         Pair.of(3, new GiveGiftToHero(100)),
         Pair.of(3, ValidateNearbyPoi.create(p_217493_ -> p_217493_.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT)),
         Pair.of(
            3,
            new GateBehavior(
               ImmutableMap.of(),
               ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
               GateBehavior.OrderPolicy.ORDERED,
               GateBehavior.RunningPolicy.RUN_ONE,
               ImmutableList.of(Pair.of(new TradeWithVillager(), 1))
            )
         ),
         getFullLookBehavior(),
         Pair.of(99, UpdateActivityFromSchedule.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getIdlePackage(Holder<VillagerProfession> p_391463_, float p_24600_) {
      return ImmutableList.of(
         Pair.of(
            2,
            new RunOne(
               ImmutableList.of(
                  Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, p_24600_, 2), 2),
                  Pair.of(InteractWith.of(EntityType.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, p_24600_, 2), 1),
                  Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, p_24600_, 2), 1),
                  Pair.of(VillageBoundRandomStroll.create(p_24600_), 1),
                  Pair.of(SetWalkTargetFromLookTarget.create(p_24600_, 2), 1),
                  Pair.of(new JumpOnBed(p_24600_), 1),
                  Pair.of(new DoNothing(30, 60), 1)
               )
            )
         ),
         Pair.of(3, new GiveGiftToHero(100)),
         Pair.of(3, SetLookAndInteract.create(EntityType.PLAYER, 4)),
         Pair.of(3, new ShowTradesToPlayer(400, 1600)),
         Pair.of(
            3,
            new GateBehavior(
               ImmutableMap.of(),
               ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
               GateBehavior.OrderPolicy.ORDERED,
               GateBehavior.RunningPolicy.RUN_ONE,
               ImmutableList.of(Pair.of(new TradeWithVillager(), 1))
            )
         ),
         Pair.of(
            3,
            new GateBehavior(
               ImmutableMap.of(),
               ImmutableSet.of(MemoryModuleType.BREED_TARGET),
               GateBehavior.OrderPolicy.ORDERED,
               GateBehavior.RunningPolicy.RUN_ONE,
               ImmutableList.of(Pair.of(new VillagerMakeLove(), 1))
            )
         ),
         getFullLookBehavior(),
         Pair.of(99, UpdateActivityFromSchedule.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPanicPackage(Holder<VillagerProfession> p_396638_, float p_24603_) {
      float f = p_24603_ * 1.5F;
      return ImmutableList.of(
         Pair.of(0, VillagerCalmDown.create()),
         Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, f, 6, false)),
         Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, f, 6, false)),
         Pair.of(3, VillageBoundRandomStroll.create(f, 2, 2)),
         getMinimalLookBehavior()
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPreRaidPackage(
      Holder<VillagerProfession> p_393574_, float p_24606_
   ) {
      return ImmutableList.of(
         Pair.of(0, RingBell.create()),
         Pair.of(
            0,
            TriggerGate.triggerOneShuffled(
               ImmutableList.of(
                  Pair.of(SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, p_24606_ * 1.5F, 2, 150, 200), 6),
                  Pair.of(VillageBoundRandomStroll.create(p_24606_ * 1.5F), 2)
               )
            )
         ),
         getMinimalLookBehavior(),
         Pair.of(99, ResetRaidStatus.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRaidPackage(Holder<VillagerProfession> p_397187_, float p_24609_) {
      return ImmutableList.of(
         Pair.of(
            0,
            BehaviorBuilder.sequence(
               BehaviorBuilder.triggerIf(VillagerGoalPackages::raidExistsAndNotVictory),
               TriggerGate.triggerOneShuffled(
                  ImmutableList.of(Pair.of(MoveToSkySeeingSpot.create(p_24609_), 5), Pair.of(VillageBoundRandomStroll.create(p_24609_ * 1.1F), 2))
               )
            )
         ),
         Pair.of(0, new CelebrateVillagersSurvivedRaid(600, 600)),
         Pair.of(
            2, BehaviorBuilder.sequence(BehaviorBuilder.triggerIf(VillagerGoalPackages::raidExistsAndActive), LocateHidingPlace.create(24, p_24609_ * 1.4F, 1))
         ),
         getMinimalLookBehavior(),
         Pair.of(99, ResetRaidStatus.create())
      );
   }

   public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getHidePackage(Holder<VillagerProfession> p_397551_, float p_24612_) {
      int i = 2;
      return ImmutableList.of(Pair.of(0, SetHiddenState.create(15, 3)), Pair.of(1, LocateHidingPlace.create(32, p_24612_ * 1.25F, 2)), getMinimalLookBehavior());
   }

   private static Pair<Integer, BehaviorControl<LivingEntity>> getFullLookBehavior() {
      return Pair.of(
         5,
         new RunOne(
            ImmutableList.of(
               Pair.of(SetEntityLookTarget.create(EntityType.CAT, 8.0F), 8),
               Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
               Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
               Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 8.0F), 1),
               Pair.of(SetEntityLookTarget.create(MobCategory.WATER_CREATURE, 8.0F), 1),
               Pair.of(SetEntityLookTarget.create(MobCategory.AXOLOTLS, 8.0F), 1),
               Pair.of(SetEntityLookTarget.create(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
               Pair.of(SetEntityLookTarget.create(MobCategory.WATER_AMBIENT, 8.0F), 1),
               Pair.of(SetEntityLookTarget.create(MobCategory.MONSTER, 8.0F), 1),
               Pair.of(new DoNothing(30, 60), 2)
            )
         )
      );
   }

   private static Pair<Integer, BehaviorControl<LivingEntity>> getMinimalLookBehavior() {
      return Pair.of(
         5,
         new RunOne(
            ImmutableList.of(
               Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
               Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
               Pair.of(new DoNothing(30, 60), 8)
            )
         )
      );
   }

   private static boolean raidExistsAndActive(ServerLevel p_260274_, LivingEntity p_260163_) {
      Raid raid = p_260274_.getRaidAt(p_260163_.blockPosition());
      return raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
   }

   private static boolean raidExistsAndNotVictory(ServerLevel p_259939_, LivingEntity p_259384_) {
      Raid raid = p_259939_.getRaidAt(p_259384_.blockPosition());
      return raid != null && raid.isVictory();
   }
}
