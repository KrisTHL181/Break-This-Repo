package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugHiveInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
   private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
   private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
   private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
   private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_MEMBERS = true;
   private static final boolean SHOW_BLACKLISTS = true;
   private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
   private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private final Minecraft minecraft;
   private @Nullable UUID lastLookedAtUuid;

   public BeeDebugRenderer(Minecraft p_113053_) {
      this.minecraft = p_113053_;
   }

   @Override
   public void emitGizmos(double p_459943_, double p_454832_, double p_455674_, DebugValueAccess p_459931_, Frustum p_453038_, float p_454372_) {
      this.doRender(p_459931_);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void doRender(DebugValueAccess p_429458_) {
      BlockPos blockpos = this.getCamera().blockPosition();
      p_429458_.forEachEntity(DebugSubscriptions.BEES, (p_448225_, p_448226_) -> {
         if (this.minecraft.player.closerThan(p_448225_, 30.0)) {
            DebugGoalInfo debuggoalinfo = p_429458_.getEntityValue(DebugSubscriptions.GOAL_SELECTORS, p_448225_);
            this.renderBeeInfo(p_448225_, p_448226_, debuggoalinfo);
         }
      });
      this.renderFlowerInfos(p_429458_);
      Map<BlockPos, Set<UUID>> map = this.createHiveBlacklistMap(p_429458_);
      p_429458_.forEachBlock(DebugSubscriptions.BEE_HIVES, (p_448235_, p_448236_) -> {
         if (blockpos.closerThan(p_448235_, 30.0)) {
            highlightHive(p_448235_);
            Set<UUID> set = map.getOrDefault(p_448235_, Set.of());
            this.renderHiveInfo(p_448235_, p_448236_, set, p_429458_);
         }
      });
      this.getGhostHives(p_429458_).forEach((p_448230_, p_448231_) -> {
         if (blockpos.closerThan(p_448230_, 30.0)) {
            this.renderGhostHive(p_448230_, (List<String>)p_448231_);
         }
      });
   }

   private Map<BlockPos, Set<UUID>> createHiveBlacklistMap(DebugValueAccess p_427268_) {
      Map<BlockPos, Set<UUID>> map = new HashMap<>();
      p_427268_.forEachEntity(DebugSubscriptions.BEES, (p_420937_, p_420938_) -> {
         for (BlockPos blockpos : p_420938_.blacklistedHives()) {
            map.computeIfAbsent(blockpos, p_296252_ -> new HashSet<>()).add(p_420937_.getUUID());
         }
      });
      return map;
   }

   private void renderFlowerInfos(DebugValueAccess p_428203_) {
      Map<BlockPos, Set<UUID>> map = new HashMap<>();
      p_428203_.forEachEntity(DebugSubscriptions.BEES, (p_420955_, p_420956_) -> {
         if (p_420956_.flowerPos().isPresent()) {
            map.computeIfAbsent(p_420956_.flowerPos().get(), p_420926_ -> new HashSet<>()).add(p_420955_.getUUID());
         }
      });
      map.forEach((p_448227_, p_448228_) -> {
         Set<String> set = p_448228_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
         int i = 1;
         Gizmos.billboardTextOverBlock(set.toString(), p_448227_, i++, -256, 0.32F);
         Gizmos.billboardTextOverBlock("Flower", p_448227_, i++, -1, 0.32F);
         Gizmos.cuboid(p_448227_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.8F, 0.8F, 0.0F)));
      });
   }

   private static String getBeeUuidsAsString(Collection<UUID> p_113116_) {
      if (p_113116_.isEmpty()) {
         return "-";
      } else {
         return p_113116_.size() > 3
            ? p_113116_.size() + " bees"
            : p_113116_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
      }
   }

   private static void highlightHive(BlockPos p_270687_) {
      float f = 0.05F;
      Gizmos.cuboid(p_270687_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostHive(BlockPos p_270550_, List<String> p_270221_) {
      float f = 0.05F;
      Gizmos.cuboid(p_270550_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(p_270221_.toString(), p_270550_, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost Hive", p_270550_, 1, -65536, 0.32F);
   }

   private void renderHiveInfo(BlockPos p_427329_, DebugHiveInfo p_422944_, Collection<UUID> p_270946_, DebugValueAccess p_429657_) {
      int i = 0;
      if (!p_270946_.isEmpty()) {
         renderTextOverHive("Blacklisted by " + getBeeUuidsAsString(p_270946_), p_427329_, i++, -65536);
      }

      renderTextOverHive("Out: " + getBeeUuidsAsString(this.getHiveMembers(p_427329_, p_429657_)), p_427329_, i++, -3355444);
      if (p_422944_.occupantCount() == 0) {
         renderTextOverHive("In: -", p_427329_, i++, -256);
      } else if (p_422944_.occupantCount() == 1) {
         renderTextOverHive("In: 1 bee", p_427329_, i++, -256);
      } else {
         renderTextOverHive("In: " + p_422944_.occupantCount() + " bees", p_427329_, i++, -256);
      }

      renderTextOverHive("Honey: " + p_422944_.honeyLevel(), p_427329_, i++, -23296);
      renderTextOverHive(p_422944_.type().getName().getString() + (p_422944_.sedated() ? " (sedated)" : ""), p_427329_, i++, -1);
   }

   private void renderBeeInfo(Entity p_427794_, DebugBeeInfo p_427535_, @Nullable DebugGoalInfo p_422631_) {
      boolean flag = this.isBeeSelected(p_427794_);
      int i = 0;
      Gizmos.billboardTextOverMob(p_427794_, i++, p_427535_.toString(), -1, 0.48F);
      if (p_427535_.hivePos().isEmpty()) {
         Gizmos.billboardTextOverMob(p_427794_, i++, "No hive", -98404, 0.32F);
      } else {
         Gizmos.billboardTextOverMob(p_427794_, i++, "Hive: " + this.getPosDescription(p_427794_, p_427535_.hivePos().get()), -256, 0.32F);
      }

      if (p_427535_.flowerPos().isEmpty()) {
         Gizmos.billboardTextOverMob(p_427794_, i++, "No flower", -98404, 0.32F);
      } else {
         Gizmos.billboardTextOverMob(p_427794_, i++, "Flower: " + this.getPosDescription(p_427794_, p_427535_.flowerPos().get()), -256, 0.32F);
      }

      if (p_422631_ != null) {
         for (DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal : p_422631_.goals()) {
            if (debuggoalinfo$debuggoal.isRunning()) {
               Gizmos.billboardTextOverMob(p_427794_, i++, debuggoalinfo$debuggoal.name(), -16711936, 0.32F);
            }
         }
      }

      if (p_427535_.travelTicks() > 0) {
         int j = p_427535_.travelTicks() < 2400 ? -3355444 : -23296;
         Gizmos.billboardTextOverMob(p_427794_, i++, "Travelling: " + p_427535_.travelTicks() + " ticks", j, 0.32F);
      }
   }

   private static void renderTextOverHive(String p_270119_, BlockPos p_430067_, int p_270930_, int p_270094_) {
      Gizmos.billboardTextOverBlock(p_270119_, p_430067_, p_270930_, p_270094_, 0.32F);
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }

   private String getPosDescription(Entity p_428780_, BlockPos p_113070_) {
      double d0 = p_113070_.distToCenterSqr(p_428780_.position());
      double d1 = Math.round(d0 * 10.0) / 10.0;
      return p_113070_.toShortString() + " (dist " + d1 + ")";
   }

   private boolean isBeeSelected(Entity p_424141_) {
      return Objects.equals(this.lastLookedAtUuid, p_424141_.getUUID());
   }

   private Collection<UUID> getHiveMembers(BlockPos p_113130_, DebugValueAccess p_430632_) {
      Set<UUID> set = new HashSet<>();
      p_430632_.forEachEntity(DebugSubscriptions.BEES, (p_420948_, p_420949_) -> {
         if (p_420949_.hasHive(p_113130_)) {
            set.add(p_420948_.getUUID());
         }
      });
      return set;
   }

   private Map<BlockPos, List<String>> getGhostHives(DebugValueAccess p_423818_) {
      Map<BlockPos, List<String>> map = new HashMap<>();
      p_423818_.forEachEntity(DebugSubscriptions.BEES, (p_420929_, p_420930_) -> {
         if (p_420930_.hivePos().isPresent() && p_423818_.getBlockValue(DebugSubscriptions.BEE_HIVES, p_420930_.hivePos().get()) == null) {
            map.computeIfAbsent(p_420930_.hivePos().get(), p_113140_ -> Lists.newArrayList()).add(DebugEntityNameGenerator.getEntityName(p_420929_));
         }
      });
      return map;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
   }
}
