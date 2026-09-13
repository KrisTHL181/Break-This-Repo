package net.minecraft.world.entity.ai.goal;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GolemRandomStrollInVillageGoal extends RandomStrollGoal {
   private static final int POI_SECTION_SCAN_RADIUS = 2;
   private static final int VILLAGER_SCAN_RADIUS = 32;
   private static final int RANDOM_POS_XY_DISTANCE = 10;
   private static final int RANDOM_POS_Y_DISTANCE = 7;

   public GolemRandomStrollInVillageGoal(final PathfinderMob mob, final double speedModifier) {
      super(mob, speedModifier, 240, false);
   }

   @Override
   protected @Nullable Vec3 getPosition() {
      RandomSource random = this.mob.level().getRandom();
      if (random.nextFloat() < 0.3F) {
         return this.getPositionTowardsAnywhere();
      }

      Vec3 target;
      if (random.nextFloat() < 0.7F) {
         target = this.getPositionTowardsVillagerWhoWantsGolem();
         if (target == null) {
            target = this.getPositionTowardsPoi();
         }
      } else {
         target = this.getPositionTowardsPoi();
         if (target == null) {
            target = this.getPositionTowardsVillagerWhoWantsGolem();
         }
      }

      return target == null ? this.getPositionTowardsAnywhere() : target;
   }

   private @Nullable Vec3 getPositionTowardsAnywhere() {
      return LandRandomPos.getPos(this.mob, 10, 7);
   }

   private @Nullable Vec3 getPositionTowardsVillagerWhoWantsGolem() {
      ServerLevel level = (ServerLevel)this.mob.level();
      List<Villager> villagers = level.getEntities(EntityTypes.VILLAGER, this.mob.getBoundingBox().inflate(32.0), this::doesVillagerWantGolem);
      if (villagers.isEmpty()) {
         return null;
      }

      Villager villager = villagers.get(this.mob.level().getRandom().nextInt(villagers.size()));
      Vec3 targetPos = villager.position();
      return LandRandomPos.getPosTowards(this.mob, 10, 7, targetPos);
   }

   private @Nullable Vec3 getPositionTowardsPoi() {
      SectionPos targetSection = this.getRandomVillageSection();
      if (targetSection == null) {
         return null;
      }

      BlockPos targetPos = this.getRandomPoiWithinSection(targetSection);
      return targetPos == null ? null : LandRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(targetPos));
   }

   private @Nullable SectionPos getRandomVillageSection() {
      ServerLevel level = (ServerLevel)this.mob.level();
      List<SectionPos> villageSections = SectionPos.cube(SectionPos.of(this.mob), 2)
         .filter(sectionPos -> level.sectionsToVillage(sectionPos) == 0)
         .collect(Collectors.toList());
      return villageSections.isEmpty() ? null : villageSections.get(level.getRandom().nextInt(villageSections.size()));
   }

   private @Nullable BlockPos getRandomPoiWithinSection(final SectionPos sectionPos) {
      ServerLevel level = (ServerLevel)this.mob.level();
      PoiManager poiManager = level.getPoiManager();
      List<BlockPos> pois = poiManager.getInRange(poiType -> true, sectionPos.center(), 8, PoiManager.Occupancy.IS_OCCUPIED)
         .map(PoiRecord::getPos)
         .collect(Collectors.toList());
      return pois.isEmpty() ? null : pois.get(level.getRandom().nextInt(pois.size()));
   }

   private boolean doesVillagerWantGolem(final Villager villager) {
      return villager.wantsToSpawnGolem(this.mob.level().getGameTime());
   }
}
