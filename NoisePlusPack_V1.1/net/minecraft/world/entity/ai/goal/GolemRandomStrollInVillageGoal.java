package net.minecraft.world.entity.ai.goal;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
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

   public GolemRandomStrollInVillageGoal(PathfinderMob p_25398_, double p_25399_) {
      super(p_25398_, p_25399_, 240, false);
   }

   @Override
   protected @Nullable Vec3 getPosition() {
      float f = this.mob.level().random.nextFloat();
      if (this.mob.level().random.nextFloat() < 0.3F) {
         return this.getPositionTowardsAnywhere();
      }

      Vec3 vec3;
      if (f < 0.7F) {
         vec3 = this.getPositionTowardsVillagerWhoWantsGolem();
         if (vec3 == null) {
            vec3 = this.getPositionTowardsPoi();
         }
      } else {
         vec3 = this.getPositionTowardsPoi();
         if (vec3 == null) {
            vec3 = this.getPositionTowardsVillagerWhoWantsGolem();
         }
      }

      return vec3 == null ? this.getPositionTowardsAnywhere() : vec3;
   }

   private @Nullable Vec3 getPositionTowardsAnywhere() {
      return LandRandomPos.getPos(this.mob, 10, 7);
   }

   private @Nullable Vec3 getPositionTowardsVillagerWhoWantsGolem() {
      ServerLevel serverlevel = (ServerLevel)this.mob.level();
      List<Villager> list = serverlevel.getEntities(EntityType.VILLAGER, this.mob.getBoundingBox().inflate(32.0), this::doesVillagerWantGolem);
      if (list.isEmpty()) {
         return null;
      }

      Villager villager = list.get(this.mob.level().random.nextInt(list.size()));
      Vec3 vec3 = villager.position();
      return LandRandomPos.getPosTowards(this.mob, 10, 7, vec3);
   }

   private @Nullable Vec3 getPositionTowardsPoi() {
      SectionPos sectionpos = this.getRandomVillageSection();
      if (sectionpos == null) {
         return null;
      }

      BlockPos blockpos = this.getRandomPoiWithinSection(sectionpos);
      return blockpos == null ? null : LandRandomPos.getPosTowards(this.mob, 10, 7, Vec3.atBottomCenterOf(blockpos));
   }

   private @Nullable SectionPos getRandomVillageSection() {
      ServerLevel serverlevel = (ServerLevel)this.mob.level();
      List<SectionPos> list = SectionPos.cube(SectionPos.of(this.mob), 2)
         .filter(p_25402_ -> serverlevel.sectionsToVillage(p_25402_) == 0)
         .collect(Collectors.toList());
      return list.isEmpty() ? null : list.get(serverlevel.random.nextInt(list.size()));
   }

   private @Nullable BlockPos getRandomPoiWithinSection(SectionPos p_25408_) {
      ServerLevel serverlevel = (ServerLevel)this.mob.level();
      PoiManager poimanager = serverlevel.getPoiManager();
      List<BlockPos> list = poimanager.getInRange(p_217747_ -> true, p_25408_.center(), 8, PoiManager.Occupancy.IS_OCCUPIED)
         .map(PoiRecord::getPos)
         .collect(Collectors.toList());
      return list.isEmpty() ? null : list.get(serverlevel.random.nextInt(list.size()));
   }

   private boolean doesVillagerWantGolem(Villager p_460464_) {
      return p_460464_.wantsToSpawnGolem(this.mob.level().getGameTime());
   }
}
