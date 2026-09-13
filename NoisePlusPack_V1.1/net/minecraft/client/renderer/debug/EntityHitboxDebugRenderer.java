package net.minecraft.client.renderer.debug;

import net.minecraft.SharedConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EntityHitboxDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   final Minecraft minecraft;

   public EntityHitboxDebugRenderer(Minecraft p_457923_) {
      this.minecraft = p_457923_;
   }

   @Override
   public void emitGizmos(double p_457580_, double p_452210_, double p_460478_, DebugValueAccess p_453314_, Frustum p_460871_, float p_452842_) {
      if (this.minecraft.level != null) {
         for (Entity entity : this.minecraft.level.entitiesForRendering()) {
            if (!entity.isInvisible()
               && p_460871_.isVisible(entity.getBoundingBox())
               && (entity != this.minecraft.getCameraEntity() || this.minecraft.options.getCameraType() != CameraType.FIRST_PERSON)) {
               this.showHitboxes(entity, p_452842_, false);
               if (SharedConstants.DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES) {
                  Entity entity1 = this.getServerEntity(entity);
                  if (entity1 != null) {
                     this.showHitboxes(entity, p_452842_, true);
                  } else {
                     Gizmos.billboardText(
                        "Missing Server Entity",
                        entity.getPosition(p_452842_).add(0.0, entity.getBoundingBox().getYsize() + 1.5, 0.0),
                        TextGizmo.Style.forColorAndCentered(-65536)
                     );
                  }
               }
            }
         }
      }
   }

   private @Nullable Entity getServerEntity(Entity p_460150_) {
      IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
      if (integratedserver != null) {
         ServerLevel serverlevel = integratedserver.getLevel(p_460150_.level().dimension());
         if (serverlevel != null) {
            return serverlevel.getEntity(p_460150_.getId());
         }
      }

      return null;
   }

   private void showHitboxes(Entity p_450643_, float p_456396_, boolean p_460461_) {
      Vec3 vec3 = p_450643_.position();
      Vec3 vec31 = p_450643_.getPosition(p_456396_);
      Vec3 vec32 = vec31.subtract(vec3);
      int i = p_460461_ ? -16711936 : -1;
      Gizmos.cuboid(p_450643_.getBoundingBox().move(vec32), GizmoStyle.stroke(i));
      Gizmos.point(vec31, i, 2.0F);
      Entity entity = p_450643_.getVehicle();
      if (entity != null) {
         float f = Math.min(entity.getBbWidth(), p_450643_.getBbWidth()) / 2.0F;
         float f1 = 0.0625F;
         Vec3 vec33 = entity.getPassengerRidingPosition(p_450643_).add(vec32);
         Gizmos.cuboid(new AABB(vec33.x - f, vec33.y, vec33.z - f, vec33.x + f, vec33.y + 0.0625, vec33.z + f), GizmoStyle.stroke(-256));
      }

      if (p_450643_ instanceof LivingEntity) {
         AABB aabb = p_450643_.getBoundingBox().move(vec32);
         float f2 = 0.01F;
         Gizmos.cuboid(
            new AABB(aabb.minX, aabb.minY + p_450643_.getEyeHeight() - 0.01F, aabb.minZ, aabb.maxX, aabb.minY + p_450643_.getEyeHeight() + 0.01F, aabb.maxZ),
            GizmoStyle.stroke(-65536)
         );
      }

      if (p_450643_ instanceof EnderDragon enderdragon) {
         for (EnderDragonPart enderdragonpart : enderdragon.getSubEntities()) {
            Vec3 vec34 = enderdragonpart.position();
            Vec3 vec35 = enderdragonpart.getPosition(p_456396_);
            Vec3 vec36 = vec35.subtract(vec34);
            Gizmos.cuboid(enderdragonpart.getBoundingBox().move(vec36), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F)));
         }
      }

      Vec3 vec37 = vec31.add(0.0, p_450643_.getEyeHeight(), 0.0);
      Vec3 vec38 = p_450643_.getViewVector(p_456396_);
      Gizmos.arrow(vec37, vec37.add(vec38.scale(2.0)), -16776961);
      if (p_460461_) {
         Vec3 vec39 = p_450643_.getDeltaMovement();
         Gizmos.arrow(vec31, vec31.add(vec39), -256);
      }
   }
}
