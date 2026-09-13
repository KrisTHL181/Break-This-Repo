package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface Leashable {
   String LEASH_TAG = "leash";
   double LEASH_TOO_FAR_DIST = 12.0;
   double LEASH_ELASTIC_DIST = 6.0;
   double MAXIMUM_ALLOWED_LEASHED_DIST = 16.0;
   Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
   float SPRING_DAMPENING = 0.7F;
   double TORSIONAL_ELASTICITY = 10.0;
   double STIFFNESS = 0.11;
   List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.5));
   List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.0));
   List<Vec3> SHARED_QUAD_ATTACHMENT_POINTS = ImmutableList.of(
      new Vec3(-0.5, 0.5, 0.5), new Vec3(-0.5, 0.5, -0.5), new Vec3(0.5, 0.5, -0.5), new Vec3(0.5, 0.5, 0.5)
   );

   Leashable.@Nullable LeashData getLeashData();

   void setLeashData(Leashable.@Nullable LeashData leashData);

   default boolean isLeashed() {
      return this.getLeashData() != null && this.getLeashData().leashHolder != null;
   }

   default boolean mayBeLeashed() {
      return this.getLeashData() != null;
   }

   default boolean canHaveALeashAttachedTo(final Entity entity) {
      if (this == entity) {
         return false;
      } else {
         return this.leashDistanceTo(entity) > this.leashSnapDistance() ? false : this.canBeLeashed();
      }
   }

   default double leashDistanceTo(final Entity entity) {
      return entity.getBoundingBox().getCenter().distanceTo(((Entity)this).getBoundingBox().getCenter());
   }

   default boolean canBeLeashed() {
      return true;
   }

   default void setDelayedLeashHolderId(final int entityId) {
      this.setLeashData(new Leashable.LeashData(entityId));
      dropLeash((Entity & Leashable)this, false, false);
   }

   default void readLeashData(final ValueInput input) {
      Leashable.LeashData newLeashData = input.<Leashable.LeashData>read("leash", Leashable.LeashData.CODEC).orElse(null);
      if (this.getLeashData() != null && newLeashData == null) {
         this.removeLeash();
      }

      this.setLeashData(newLeashData);
   }

   default void writeLeashData(final ValueOutput output, final Leashable.@Nullable LeashData leashData) {
      output.storeNullable("leash", Leashable.LeashData.CODEC, leashData);
   }

   private static <E extends Entity & Leashable> void restoreLeashFromSave(final E entity, final Leashable.LeashData leashData) {
      if (leashData.delayedLeashInfo != null && entity.level() instanceof ServerLevel serverLevel) {
         Optional<UUID> leashUuid = leashData.delayedLeashInfo.left();
         Optional<BlockPos> pos = leashData.delayedLeashInfo.right();
         if (leashUuid.isPresent()) {
            Entity leasher = serverLevel.getEntity(leashUuid.get());
            if (leasher != null) {
               setLeashedTo(entity, leasher, true);
               return;
            }
         } else if (pos.isPresent()) {
            setLeashedTo(entity, LeashFenceKnotEntity.getOrCreateKnot(serverLevel, pos.get()), true);
            return;
         }

         if (entity.tickCount > 100) {
            entity.spawnAtLocation(serverLevel, Items.LEAD);
            entity.setLeashData(null);
         }
      }
   }

   default void dropLeash() {
      dropLeash((Entity & Leashable)this, true, true);
   }

   default void removeLeash() {
      dropLeash((Entity & Leashable)this, true, false);
   }

   default void onLeashRemoved() {
   }

   private static <E extends Entity & Leashable> void dropLeash(final E entity, final boolean sendPacket, final boolean dropLead) {
      Leashable.LeashData leashData = entity.getLeashData();
      if (leashData != null && leashData.leashHolder != null) {
         entity.setLeashData(null);
         entity.onLeashRemoved();
         if (entity.level() instanceof ServerLevel level) {
            if (dropLead) {
               entity.spawnAtLocation(level, Items.LEAD);
            }

            if (sendPacket) {
               level.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, null));
            }

            leashData.leashHolder.notifyLeasheeRemoved(entity);
         }
      }
   }

   static <E extends Entity & Leashable> void tickLeash(final ServerLevel level, final E entity) {
      Leashable.LeashData leashData = entity.getLeashData();
      if (leashData != null && leashData.delayedLeashInfo != null) {
         restoreLeashFromSave(entity, leashData);
      }

      if (leashData != null && leashData.leashHolder != null) {
         if (!entity.canInteractWithLevel() || !leashData.leashHolder.canInteractWithLevel()) {
            if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
               entity.dropLeash();
            } else {
               entity.removeLeash();
            }
         }

         Entity leashHolder = entity.getLeashHolder();
         if (leashHolder != null && leashHolder.level() == entity.level()) {
            double distanceTo = entity.leashDistanceTo(leashHolder);
            entity.whenLeashedTo(leashHolder);
            if (distanceTo > entity.leashSnapDistance()) {
               level.playSound(null, leashHolder.getX(), leashHolder.getY(), leashHolder.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
               entity.leashTooFarBehaviour();
            } else if (distanceTo > entity.leashElasticDistance() - leashHolder.getBbWidth() - entity.getBbWidth()
               && entity.checkElasticInteractions(leashHolder, leashData)) {
               entity.onElasticLeashPull();
            } else {
               entity.closeRangeLeashBehaviour(leashHolder);
            }

            entity.setYRot((float)(entity.getYRot() - leashData.angularMomentum));
            leashData.angularMomentum = leashData.angularMomentum * angularFriction(entity);
         }
      }
   }

   default void onElasticLeashPull() {
      Entity entity = (Entity)this;
      entity.checkFallDistanceAccumulation();
   }

   default double leashSnapDistance() {
      return 12.0;
   }

   default double leashElasticDistance() {
      return 6.0;
   }

   static <E extends Entity & Leashable> float angularFriction(final E entity) {
      if (entity.onGround()) {
         return entity.level().getBlockState(entity.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
      } else {
         return entity.isInLiquid() ? 0.8F : 0.91F;
      }
   }

   default void whenLeashedTo(final Entity leashHolder) {
      leashHolder.notifyLeashHolder(this);
   }

   default void leashTooFarBehaviour() {
      this.dropLeash();
   }

   default void closeRangeLeashBehaviour(final Entity leashHolder) {
   }

   default boolean checkElasticInteractions(final Entity leashHolder, final Leashable.LeashData leashData) {
      boolean quadConnection = leashHolder.supportQuadLeashAsHolder() && this.supportQuadLeash();
      List<Leashable.Wrench> wrenches = computeElasticInteraction(
         (Entity & Leashable)this,
         leashHolder,
         quadConnection ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT,
         quadConnection ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT
      );
      if (wrenches.isEmpty()) {
         return false;
      }

      Leashable.Wrench result = Leashable.Wrench.accumulate(wrenches).scale(quadConnection ? 0.25 : 1.0);
      leashData.angularMomentum = leashData.angularMomentum + 10.0 * result.torque();
      Vec3 relativeVelocityToLeasher = getHolderMovement(leashHolder).subtract(((Entity)this).getKnownMovement());
      ((Entity)this).addDeltaMovement(result.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(relativeVelocityToLeasher.scale(0.11)));
      return true;
   }

   private static Vec3 getHolderMovement(final Entity leashHolder) {
      return leashHolder instanceof Mob mob && mob.isNoAi() ? Vec3.ZERO : leashHolder.getKnownMovement();
   }

   private static <E extends Entity & Leashable> List<Leashable.Wrench> computeElasticInteraction(
      final E entity, final Entity leashHolder, final List<Vec3> entityAttachmentPoints, final List<Vec3> leasherAttachmentPoints
   ) {
      double slackDistance = entity.leashElasticDistance();
      Vec3 currentMovement = getHolderMovement(entity);
      float entityYRot = entity.getYRot() * (float) (Math.PI / 180.0);
      Vec3 entityDimensions = new Vec3(entity.getBbWidth(), entity.getBbHeight(), entity.getBbWidth());
      float leashHolderYRot = leashHolder.getYRot() * (float) (Math.PI / 180.0);
      Vec3 leasherDimensions = new Vec3(leashHolder.getBbWidth(), leashHolder.getBbHeight(), leashHolder.getBbWidth());
      List<Leashable.Wrench> wrenches = new ArrayList<>();

      for (int i = 0; i < entityAttachmentPoints.size(); i++) {
         Vec3 entityAttachVector = entityAttachmentPoints.get(i).multiply(entityDimensions).yRot(-entityYRot);
         Vec3 entityAttachPos = entity.position().add(entityAttachVector);
         Vec3 leasherAttachVector = leasherAttachmentPoints.get(i).multiply(leasherDimensions).yRot(-leashHolderYRot);
         Vec3 leasherAttachPos = leashHolder.position().add(leasherAttachVector);
         computeDampenedSpringInteraction(leasherAttachPos, entityAttachPos, slackDistance, currentMovement, entityAttachVector).ifPresent(wrenches::add);
      }

      return wrenches;
   }

   private static Optional<Leashable.Wrench> computeDampenedSpringInteraction(
      final Vec3 pivotPoint, final Vec3 objectPosition, final double springSlack, final Vec3 objectMotion, final Vec3 leverArm
   ) {
      double distance = objectPosition.distanceTo(pivotPoint);
      if (distance < springSlack) {
         return Optional.empty();
      }

      Vec3 displacement = pivotPoint.subtract(objectPosition).normalize().scale(distance - springSlack);
      double torque = Leashable.Wrench.torqueFromForce(leverArm, displacement);
      boolean sameDirectionToMovement = objectMotion.dot(displacement) >= 0.0;
      if (sameDirectionToMovement) {
         displacement = displacement.scale(0.3F);
      }

      return Optional.of(new Leashable.Wrench(displacement, torque));
   }

   default boolean supportQuadLeash() {
      return false;
   }

   default Vec3[] getQuadLeashOffsets() {
      return createQuadLeashOffsets((Entity)this, 0.0, 0.5, 0.5, 0.5);
   }

   static Vec3[] createQuadLeashOffsets(final Entity entity, final double frontOffset, final double frontBack, final double leftRight, final double height) {
      float width = entity.getBbWidth();
      double frontOffsetScaled = frontOffset * width;
      double frontBackScaled = frontBack * width;
      double leftRightScaled = leftRight * width;
      double heightScaled = height * entity.getBbHeight();
      return new Vec3[]{
         new Vec3(-leftRightScaled, heightScaled, frontBackScaled + frontOffsetScaled),
         new Vec3(-leftRightScaled, heightScaled, -frontBackScaled + frontOffsetScaled),
         new Vec3(leftRightScaled, heightScaled, -frontBackScaled + frontOffsetScaled),
         new Vec3(leftRightScaled, heightScaled, frontBackScaled + frontOffsetScaled)
      };
   }

   default Vec3 getLeashOffset(final float partialTicks) {
      return this.getLeashOffset();
   }

   default Vec3 getLeashOffset() {
      Entity entity = (Entity)this;
      return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4F);
   }

   default void setLeashedTo(final Entity holder, final boolean synch) {
      if (this != holder) {
         setLeashedTo((Entity & Leashable)this, holder, synch);
      }
   }

   private static <E extends Entity & Leashable> void setLeashedTo(final E entity, final Entity holder, final boolean synch) {
      Leashable.LeashData leashData = entity.getLeashData();
      if (leashData == null) {
         leashData = new Leashable.LeashData(holder);
         entity.setLeashData(leashData);
      } else {
         Entity oldHolder = leashData.leashHolder;
         leashData.setLeashHolder(holder);
         if (oldHolder != null && oldHolder != holder) {
            oldHolder.notifyLeasheeRemoved(entity);
         }
      }

      if (synch && entity.level() instanceof ServerLevel level) {
         level.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, holder));
      }

      if (entity.isPassenger()) {
         entity.stopRiding();
      }
   }

   default @Nullable Entity getLeashHolder() {
      return getLeashHolder((Entity & Leashable)this);
   }

   private static <E extends Entity & Leashable> @Nullable Entity getLeashHolder(final E entity) {
      Leashable.LeashData leashData = entity.getLeashData();
      if (leashData == null) {
         return null;
      }

      Entity ntt = entity.level().getEntity(leashData.delayedLeashHolderId);
      if (leashData.delayedLeashHolderId != 0 && entity.level().isClientSide() && ntt != null) {
         leashData.setLeashHolder(ntt);
      }

      return leashData.leashHolder;
   }

   static List<Leashable> leashableLeashedTo(final Entity entity) {
      return leashableInArea(entity, l -> l.getLeashHolder() == entity);
   }

   static List<Leashable> leashableInArea(final Entity entity, final Predicate<Leashable> test) {
      return leashableInArea(entity.level(), entity.getBoundingBox().getCenter(), test);
   }

   static List<Leashable> leashableInArea(final Level level, final Vec3 pos, final Predicate<Leashable> test) {
      double size = 32.0;
      AABB scanArea = AABB.ofSize(pos, 32.0, 32.0, 32.0);
      return level.getEntitiesOfClass(Entity.class, scanArea, e -> e instanceof Leashable leashable && test.test(leashable))
         .stream()
         .map(Leashable.class::cast)
         .toList();
   }

   final class LeashData {
      public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
         .xmap(
            Leashable.LeashData::new,
            data -> {
               if (data.leashHolder instanceof LeashFenceKnotEntity leashKnot) {
                  return Either.right(leashKnot.getPos());
               } else {
                  return data.leashHolder != null
                     ? Either.left(data.leashHolder.getUUID())
                     : Objects.requireNonNull(data.delayedLeashInfo, "Invalid LeashData had no attachment");
               }
            }
         );
      private int delayedLeashHolderId;
      public @Nullable Entity leashHolder;
      public @Nullable Either<UUID, BlockPos> delayedLeashInfo;
      public double angularMomentum;

      private LeashData(final Either<UUID, BlockPos> delayedLeashInfo) {
         this.delayedLeashInfo = delayedLeashInfo;
      }

      private LeashData(final Entity entity) {
         this.leashHolder = entity;
      }

      private LeashData(final int entityId) {
         this.delayedLeashHolderId = entityId;
      }

      public void setLeashHolder(final Entity leashHolder) {
         this.leashHolder = leashHolder;
         this.delayedLeashInfo = null;
         this.delayedLeashHolderId = 0;
      }
   }

   record Wrench(Vec3 force, double torque) {
      public static final Leashable.Wrench ZERO = new Leashable.Wrench(Vec3.ZERO, 0.0);

      public static double torqueFromForce(final Vec3 leverArm, final Vec3 force) {
         return leverArm.z * force.x - leverArm.x * force.z;
      }

      public static Leashable.Wrench accumulate(final List<Leashable.Wrench> wrenches) {
         if (wrenches.isEmpty()) {
            return ZERO;
         }

         double x = 0.0;
         double y = 0.0;
         double z = 0.0;
         double t = 0.0;

         for (Leashable.Wrench wrench : wrenches) {
            Vec3 force = wrench.force;
            x += force.x;
            y += force.y;
            z += force.z;
            t += wrench.torque;
         }

         return new Leashable.Wrench(new Vec3(x, y, z), t);
      }

      public Leashable.Wrench scale(final double scale) {
         return new Leashable.Wrench(this.force.scale(scale), this.torque * scale);
      }
   }
}
