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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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

   public void emitGizmos(Frustum p_450271_, double p_456660_, double p_456605_, double p_454166_, float p_454210_) {
      Minecraft minecraft = Minecraft.getInstance();
      DebugValueAccess debugvalueaccess = minecraft.getConnection().createDebugValueAccess();
      if (minecraft.debugEntries.getCurrentlyEnabledVersion() != this.lastDebugEntriesVersion) {
         this.lastDebugEntriesVersion = minecraft.debugEntries.getCurrentlyEnabledVersion();
         this.refreshRendererList();
      }

      for (DebugRenderer.SimpleDebugRenderer debugrenderer$simpledebugrenderer : this.renderers) {
         debugrenderer$simpledebugrenderer.emitGizmos(p_456660_, p_456605_, p_454166_, debugvalueaccess, p_450271_, p_454210_);
      }
   }

   public static Optional<Entity> getTargetedEntity(@Nullable Entity p_113449_, int p_113450_) {
      if (p_113449_ == null) {
         return Optional.empty();
      } else {
         Vec3 vec3 = p_113449_.getEyePosition();
         Vec3 vec31 = p_113449_.getViewVector(1.0F).scale(p_113450_);
         Vec3 vec32 = vec3.add(vec31);
         AABB aabb = p_113449_.getBoundingBox().expandTowards(vec31).inflate(1.0);
         int i = p_113450_ * p_113450_;
         EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(p_113449_, vec3, vec32, aabb, EntitySelector.CAN_BE_PICKED, i);
         if (entityhitresult == null) {
            return Optional.empty();
         } else {
            return vec3.distanceToSqr(entityhitresult.getLocation()) > i ? Optional.empty() : Optional.of(entityhitresult.getEntity());
         }
      }
   }

   private static Vec3 mixColor(float p_362317_) {
      float f = 5.99999F;
      int i = (int)(Mth.clamp(p_362317_, 0.0F, 1.0F) * 5.99999F);
      float f1 = p_362317_ * 5.99999F - i;

      return switch (i) {
         case 0 -> new Vec3(1.0, f1, 0.0);
         case 1 -> new Vec3(1.0F - f1, 1.0, 0.0);
         case 2 -> new Vec3(0.0, 1.0, f1);
         case 3 -> new Vec3(0.0, 1.0 - f1, 1.0);
         case 4 -> new Vec3(f1, 0.0, 1.0);
         case 5 -> new Vec3(1.0, 0.0, 1.0 - f1);
         default -> throw new IllegalStateException("Unexpected value: " + i);
      };
   }

   private static Vec3 shiftHue(float p_366349_, float p_365255_, float p_365397_, float p_365478_) {
      Vec3 vec3 = mixColor(p_365478_).scale(p_366349_);
      Vec3 vec31 = mixColor((p_365478_ + 0.33333334F) % 1.0F).scale(p_365255_);
      Vec3 vec32 = mixColor((p_365478_ + 0.6666667F) % 1.0F).scale(p_365397_);
      Vec3 vec33 = vec3.add(vec31).add(vec32);
      double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
      return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
   }

   @OnlyIn(Dist.CLIENT)
   public interface SimpleDebugRenderer {
      void emitGizmos(double var1, double var3, double var5, DebugValueAccess var7, Frustum var8, float var9);
   }
}
