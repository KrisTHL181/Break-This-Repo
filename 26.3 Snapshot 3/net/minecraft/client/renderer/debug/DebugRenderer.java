package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DebugRenderer {
   private final List<DebugRenderer.SimpleDebugRenderer> renderers = new ArrayList<>();
   private long lastDebugEntriesVersion;

   public DebugRenderer() {
      this.refreshRendererList();
   }

   public void refreshRendererList() {
      Minecraft minecraft = Minecraft.getInstance();
      this.renderers.clear();
      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS)) {
         this.renderers.add(new ChunkBorderRenderer(minecraft));
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_OCTREE)) {
         this.renderers.add(new OctreeDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_PATHFINDING) {
         this.renderers.add(new PathfindingRenderer());
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_WATER_LEVELS)) {
         this.renderers.add(new WaterDebugRenderer(minecraft));
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_HEIGHTMAP)) {
         this.renderers.add(new HeightMapRenderer(minecraft));
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_COLLISION_BOXES)) {
         this.renderers.add(new CollisionBoxRenderer(minecraft));
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_ENTITY_SUPPORTING_BLOCKS)) {
         this.renderers.add(new SupportBlockRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_NEIGHBORSUPDATE) {
         this.renderers.add(new NeighborsUpdateRenderer());
      }

      if (SharedConstants.DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER) {
         this.renderers.add(new RedstoneWireOrientationsRenderer());
      }

      if (SharedConstants.DEBUG_STRUCTURES) {
         this.renderers.add(new StructureRenderer());
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_BLOCK_LIGHT_LEVELS)
         || minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SKY_LIGHT_LEVELS)) {
         this.renderers
            .add(
               new LightDebugRenderer(
                  minecraft,
                  minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_BLOCK_LIGHT_LEVELS),
                  minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SKY_LIGHT_LEVELS)
               )
            );
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SOLID_FACES)) {
         this.renderers.add(new SolidFaceRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_VILLAGE_SECTIONS) {
         this.renderers.add(new VillageSectionsDebugRenderer());
      }

      if (SharedConstants.DEBUG_BRAIN) {
         this.renderers.add(new BrainDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_POI) {
         this.renderers.add(new PoiDebugRenderer(new BrainDebugRenderer(minecraft)));
      }

      if (SharedConstants.DEBUG_BEES) {
         this.renderers.add(new BeeDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_RAIDS) {
         this.renderers.add(new RaidDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_GOAL_SELECTOR) {
         this.renderers.add(new GoalSelectorDebugRenderer(minecraft));
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
         this.renderers.add(new ChunkDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_GAME_EVENT_LISTENERS) {
         this.renderers.add(new GameEventListenerRenderer());
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SKY_LIGHT_SECTIONS)) {
         this.renderers.add(new LightSectionDebugRenderer(minecraft, LightLayer.SKY));
      }

      if (SharedConstants.DEBUG_BREEZE_MOB) {
         this.renderers.add(new BreezeDebugRenderer(minecraft));
      }

      if (SharedConstants.DEBUG_ENTITY_BLOCK_INTERSECTION) {
         this.renderers.add(new EntityBlockIntersectionDebugRenderer());
      }

      if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
         this.renderers.add(new EntityHitboxDebugRenderer(minecraft));
      }

      this.renderers.add(new ChunkCullingDebugRenderer(minecraft));
   }

   public void emitGizmos(final Frustum frustum, final double camX, final double camY, final double camZ, final float partialTicks) {
      Minecraft minecraft = Minecraft.getInstance();
      DebugValueAccess debugValues = minecraft.getConnection().createDebugValueAccess();
      if (minecraft.debugEntries.getCurrentlyEnabledVersion() != this.lastDebugEntriesVersion) {
         this.lastDebugEntriesVersion = minecraft.debugEntries.getCurrentlyEnabledVersion();
         this.refreshRendererList();
      }

      for (DebugRenderer.SimpleDebugRenderer renderer : this.renderers) {
         renderer.emitGizmos(camX, camY, camZ, debugValues, frustum, partialTicks);
      }
   }

   public static Optional<Entity> getTargetedEntity(final @Nullable Entity cameraEntity, final int maxTargetingRange) {
      if (cameraEntity == null) {
         return Optional.empty();
      } else {
         Vec3 from = cameraEntity.getEyePosition();
         Vec3 pick = cameraEntity.getViewVector(1.0F).scale(maxTargetingRange);
         Vec3 to = from.add(pick);
         AABB box = cameraEntity.getBoundingBox().expandTowards(pick).inflate(1.0);
         int rangeSquared = maxTargetingRange * maxTargetingRange;
         EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(cameraEntity, from, to, box, EntitySelector.CAN_BE_PICKED, rangeSquared);
         if (hitResult == null) {
            return Optional.empty();
         } else {
            return from.distanceToSqr(hitResult.getLocation()) > rangeSquared ? Optional.empty() : Optional.of(hitResult.getEntity());
         }
      }
   }

   private static Vec3 mixColor(final float hueShift) {
      float regions = 5.99999F;
      int region = (int)(Mth.clamp(hueShift, 0.0F, 1.0F) * 5.99999F);
      float progress = hueShift * 5.99999F - region;

      return switch (region) {
         case 0 -> new Vec3(1.0, progress, 0.0);
         case 1 -> new Vec3(1.0F - progress, 1.0, 0.0);
         case 2 -> new Vec3(0.0, 1.0, progress);
         case 3 -> new Vec3(0.0, 1.0 - progress, 1.0);
         case 4 -> new Vec3(progress, 0.0, 1.0);
         case 5 -> new Vec3(1.0, 0.0, 1.0 - progress);
         default -> throw new IllegalStateException("Unexpected value: " + region);
      };
   }

   private static Vec3 shiftHue(final float r, final float g, final float b, final float hs) {
      Vec3 rshifted = mixColor(hs).scale(r);
      Vec3 gshifted = mixColor((hs + 0.33333334F) % 1.0F).scale(g);
      Vec3 bshifted = mixColor((hs + 0.6666667F) % 1.0F).scale(b);
      Vec3 combined = rshifted.add(gshifted).add(bshifted);
      double max = Math.max(Math.max(1.0, combined.x), Math.max(combined.y, combined.z));
      return new Vec3(combined.x / max, combined.y / max, combined.z / max);
   }

   public interface SimpleDebugRenderer {
      void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, final Frustum frustum, final float partialTicks);
   }
}
