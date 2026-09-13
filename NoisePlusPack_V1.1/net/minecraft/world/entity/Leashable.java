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

   void setLeashData(Leashable.@Nullable LeashData var1);

   default boolean isLeashed() {
      return this.getLeashData() != null && this.getLeashData().leashHolder != null;
   }

   default boolean mayBeLeashed() {
      return this.getLeashData() != null;
   }

   default boolean canHaveALeashAttachedTo(Entity p_406168_) {
      if (this == p_406168_) {
         return false;
      } else {
         return this.leashDistanceTo(p_406168_) > this.leashSnapDistance() ? false : this.canBeLeashed();
      }
   }

   default double leashDistanceTo(Entity p_407569_) {
      return p_407569_.getBoundingBox().getCenter().distanceTo(((Entity)this).getBoundingBox().getCenter());
   }

   default boolean canBeLeashed() {
      return true;
   }

   default void setDelayedLeashHolderId(int p_345000_) {
      this.setLeashData(new Leashable.LeashData(p_345000_));
      dropLeash((Entity & Leashable)this, false, false);
   }

   default void readLeashData(ValueInput p_407493_) {
      Leashable.LeashData leashable$leashdata = p_407493_.<Leashable.LeashData>read("leash", Leashable.LeashData.CODEC).orElse(null);
      if (this.getLeashData() != null && leashable$leashdata == null) {
         this.removeLeash();
      }

      this.setLeashData(leashable$leashdata);
   }

   default void writeLeashData(ValueOutput p_409985_, Leashable.@Nullable LeashData p_345503_) {
      p_409985_.storeNullable("leash", Leashable.LeashData.CODEC, p_345503_);
   }

   private static <E extends Entity & Leashable> void restoreLeashFromSave(E p_343564_, Leashable.LeashData p_344259_) {
      if (p_344259_.delayedLeashInfo != null && p_343564_.level() instanceof ServerLevel serverlevel) {
         Optional<UUID> optional1 = p_344259_.delayedLeashInfo.left();
         Optional<BlockPos> optional = p_344259_.delayedLeashInfo.right();
         if (optional1.isPresent()) {
            Entity entity = serverlevel.getEntity(optional1.get());
            if (entity != null) {
               setLeashedTo(p_343564_, entity, true);
               return;
            }
         } else if (optional.isPresent()) {
            setLeashedTo(p_343564_, LeashFenceKnotEntity.getOrCreateKnot(serverlevel, optional.get()), true);
            return;
         }

         if (p_343564_.tickCount > 100) {
            p_343564_.spawnAtLocation(serverlevel, Items.LEAD);
            p_343564_.setLeashData(null);
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

   private static <E extends Entity & Leashable> void dropLeash(E p_343459_, boolean p_342580_, boolean p_344786_) {
      Leashable.LeashData leashable$leashdata = p_343459_.getLeashData();
      if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
         p_343459_.setLeashData(null);
         p_343459_.onLeashRemoved();
         if (p_343459_.level() instanceof ServerLevel serverlevel) {
            if (p_344786_) {
               p_343459_.spawnAtLocation(serverlevel, Items.LEAD);
            }

            if (p_342580_) {
               serverlevel.getChunkSource().sendToTrackingPlayers(p_343459_, new ClientboundSetEntityLinkPacket(p_343459_, null));
            }

            leashable$leashdata.leashHolder.notifyLeasheeRemoved(p_343459_);
         }
      }
   }

   static <E extends Entity & Leashable> void tickLeash(ServerLevel p_366578_, E p_343570_) {
      Leashable.LeashData leashable$leashdata = p_343570_.getLeashData();
      if (leashable$leashdata != null && leashable$leashdata.delayedLeashInfo != null) {
         restoreLeashFromSave(p_343570_, leashable$leashdata);
      }

      if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
         if (!p_343570_.canInteractWithLevel() || !leashable$leashdata.leashHolder.canInteractWithLevel()) {
            if (p_366578_.getGameRules().get(GameRules.ENTITY_DROPS)) {
               p_343570_.dropLeash();
            } else {
               p_343570_.removeLeash();
            }
         }

         Entity entity = p_343570_.getLeashHolder();
         if (entity != null && entity.level() == p_343570_.level()) {
            double d0 = p_343570_.leashDistanceTo(entity);
            p_343570_.whenLeashedTo(entity);
            if (d0 > p_343570_.leashSnapDistance()) {
               p_366578_.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
               p_343570_.leashTooFarBehaviour();
            } else if (d0 > p_343570_.leashElasticDistance() - entity.getBbWidth() - p_343570_.getBbWidth()
               && p_343570_.checkElasticInteractions(entity, leashable$leashdata)) {
               p_343570_.onElasticLeashPull();
            } else {
               p_343570_.closeRangeLeashBehaviour(entity);
            }

            p_343570_.setYRot((float)(p_343570_.getYRot() - leashable$leashdata.angularMomentum));
            leashable$leashdata.angularMomentum = leashable$leashdata.angularMomentum * angularFriction(p_343570_);
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

   static <E extends Entity & Leashable> float angularFriction(E p_408063_) {
      if (p_408063_.onGround()) {
         return p_408063_.level().getBlockState(p_408063_.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91F;
      } else {
         return p_408063_.isInLiquid() ? 0.8F : 0.91F;
      }
   }

   default void whenLeashedTo(Entity p_407530_) {
      p_407530_.notifyLeashHolder(this);
   }

   default void leashTooFarBehaviour() {
      this.dropLeash();
   }

   default void closeRangeLeashBehaviour(Entity p_344596_) {
   }

   default boolean checkElasticInteractions(Entity p_406343_, Leashable.LeashData p_407235_) {
      boolean flag = p_406343_.supportQuadLeashAsHolder() && this.supportQuadLeash();
      List<Leashable.Wrench> list = computeElasticInteraction(
         (Entity & Leashable)this,
         p_406343_,
         flag ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT,
         flag ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT
      );
      if (list.isEmpty()) {
         return false;
      }

      Leashable.Wrench leashable$wrench = Leashable.Wrench.accumulate(list).scale(flag ? 0.25 : 1.0);
      p_407235_.angularMomentum = p_407235_.angularMomentum + 10.0 * leashable$wrench.torque();
      Vec3 vec3 = getHolderMovement(p_406343_).subtract(((Entity)this).getKnownMovement());
      ((Entity)this).addDeltaMovement(leashable$wrench.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(vec3.scale(0.11)));
      return true;
   }

   private static Vec3 getHolderMovement(Entity p_407164_) {
      return p_407164_ instanceof Mob mob && mob.isNoAi() ? Vec3.ZERO : p_407164_.getKnownMovement();
   }

   private static <E extends Entity & Leashable> List<Leashable.Wrench> computeElasticInteraction(
      E p_408183_, Entity p_406565_, List<Vec3> p_410674_, List<Vec3> p_409022_
   ) {
      double d0 = p_408183_.leashElasticDistance();
      Vec3 vec3 = getHolderMovement(p_408183_);
      float f = p_408183_.getYRot() * (float) (Math.PI / 180.0);
      Vec3 vec31 = new Vec3(p_408183_.getBbWidth(), p_408183_.getBbHeight(), p_408183_.getBbWidth());
      float f1 = p_406565_.getYRot() * (float) (Math.PI / 180.0);
      Vec3 vec32 = new Vec3(p_406565_.getBbWidth(), p_406565_.getBbHeight(), p_406565_.getBbWidth());
      List<Leashable.Wrench> list = new ArrayList<>();

      for (int i = 0; i < p_410674_.size(); i++) {
         Vec3 vec33 = p_410674_.get(i).multiply(vec31).yRot(-f);
         Vec3 vec34 = p_408183_.position().add(vec33);
         Vec3 vec35 = p_409022_.get(i).multiply(vec32).yRot(-f1);
         Vec3 vec36 = p_406565_.position().add(vec35);
         computeDampenedSpringInteraction(vec36, vec34, d0, vec3, vec33).ifPresent(list::add);
      }

      return list;
   }

   private static Optional<Leashable.Wrench> computeDampenedSpringInteraction(Vec3 p_409865_, Vec3 p_405872_, double p_409391_, Vec3 p_410398_, Vec3 p_409272_) {
      double d0 = p_405872_.distanceTo(p_409865_);
      if (d0 < p_409391_) {
         return Optional.empty();
      }

      Vec3 vec3 = p_409865_.subtract(p_405872_).normalize().scale(d0 - p_409391_);
      double d1 = Leashable.Wrench.torqueFromForce(p_409272_, vec3);
      boolean flag = p_410398_.dot(vec3) >= 0.0;
      if (flag) {
         vec3 = vec3.scale(0.3F);
      }

      return Optional.of(new Leashable.Wrench(vec3, d1));
   }

   default boolean supportQuadLeash() {
      return false;
   }

   default Vec3[] getQuadLeashOffsets() {
      return createQuadLeashOffsets((Entity)this, 0.0, 0.5, 0.5, 0.5);
   }

   static Vec3[] createQuadLeashOffsets(Entity p_408706_, double p_406447_, double p_406490_, double p_407443_, double p_408814_) {
      float f = p_408706_.getBbWidth();
      double d0 = p_406447_ * f;
      double d1 = p_406490_ * f;
      double d2 = p_407443_ * f;
      double d3 = p_408814_ * p_408706_.getBbHeight();
      return new Vec3[]{new Vec3(-d2, d3, d1 + d0), new Vec3(-d2, d3, -d1 + d0), new Vec3(d2, d3, -d1 + d0), new Vec3(d2, d3, d1 + d0)};
   }

   default Vec3 getLeashOffset(float p_409188_) {
      return this.getLeashOffset();
   }

   default Vec3 getLeashOffset() {
      Entity entity = (Entity)this;
      return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4F);
   }

   default void setLeashedTo(Entity p_342408_, boolean p_342255_) {
      if (this != p_342408_) {
         setLeashedTo((Entity & Leashable)this, p_342408_, p_342255_);
      }
   }

   private static <E extends Entity & Leashable> void setLeashedTo(E p_342775_, Entity p_342643_, boolean p_343557_) {
      Leashable.LeashData leashable$leashdata = p_342775_.getLeashData();
      if (leashable$leashdata == null) {
         leashable$leashdata = new Leashable.LeashData(p_342643_);
         p_342775_.setLeashData(leashable$leashdata);
      } else {
         Entity entity = leashable$leashdata.leashHolder;
         leashable$leashdata.setLeashHolder(p_342643_);
         if (entity != null && entity != p_342643_) {
            entity.notifyLeasheeRemoved(p_342775_);
         }
      }

      if (p_343557_ && p_342775_.level() instanceof ServerLevel serverlevel) {
         serverlevel.getChunkSource().sendToTrackingPlayers(p_342775_, new ClientboundSetEntityLinkPacket(p_342775_, p_342643_));
      }

      if (p_342775_.isPassenger()) {
         p_342775_.stopRiding();
      }
   }

   default @Nullable Entity getLeashHolder() {
      return getLeashHolder((Entity & Leashable)this);
   }

   private static <E extends Entity & Leashable> @Nullable Entity getLeashHolder(E p_342282_) {
      Leashable.LeashData leashable$leashdata = p_342282_.getLeashData();
      if (leashable$leashdata == null) {
         return null;
      }

      if (leashable$leashdata.delayedLeashHolderId != 0 && p_342282_.level().isClientSide()) {
         Entity entity = p_342282_.level().getEntity(leashable$leashdata.delayedLeashHolderId);
         if (entity instanceof Entity) {
            leashable$leashdata.setLeashHolder(entity);
         }
      }

      return leashable$leashdata.leashHolder;
   }

   static List<Leashable> leashableLeashedTo(Entity p_409040_) {
      return leashableInArea(p_409040_, p_409624_ -> p_409624_.getLeashHolder() == p_409040_);
   }

   static List<Leashable> leashableInArea(Entity p_410041_, Predicate<Leashable> p_410564_) {
      return leashableInArea(p_410041_.level(), p_410041_.getBoundingBox().getCenter(), p_410564_);
   }

   static List<Leashable> leashableInArea(Level p_409830_, Vec3 p_407309_, Predicate<Leashable> p_409916_) {
      double d0 = 32.0;
      AABB aabb = AABB.ofSize(p_407309_, 32.0, 32.0, 32.0);
      return p_409830_.getEntitiesOfClass(Entity.class, aabb, p_410526_ -> p_410526_ instanceof Leashable leashable && p_409916_.test(leashable))
         .stream()
         .map(Leashable.class::cast)
         .toList();
   }

   final class LeashData {
      public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
         .xmap(
            Leashable.LeashData::new,
            p_394128_ -> {
               if (p_394128_.leashHolder instanceof LeashFenceKnotEntity leashfenceknotentity) {
                  return Either.right(leashfenceknotentity.getPos());
               } else {
                  return p_394128_.leashHolder != null
                     ? Either.left(p_394128_.leashHolder.getUUID())
                     : Objects.requireNonNull(p_394128_.delayedLeashInfo, "Invalid LeashData had no attachment");
               }
            }
         );
      int delayedLeashHolderId;
      public @Nullable Entity leashHolder;
      public @Nullable Either<UUID, BlockPos> delayedLeashInfo;
      public double angularMomentum;

      private LeashData(Either<UUID, BlockPos> p_345305_) {
         this.delayedLeashInfo = p_345305_;
      }

      LeashData(Entity p_345447_) {
         this.leashHolder = p_345447_;
      }

      LeashData(int p_345400_) {
         this.delayedLeashHolderId = p_345400_;
      }

      public void setLeashHolder(Entity p_342311_) {
         this.leashHolder = p_342311_;
         this.delayedLeashInfo = null;
         this.delayedLeashHolderId = 0;
      }
   }

   record Wrench(Vec3 force, double torque) {
      static Leashable.Wrench ZERO = new Leashable.Wrench(Vec3.ZERO, 0.0);

      static double torqueFromForce(Vec3 p_410224_, Vec3 p_409711_) {
         return p_410224_.z * p_409711_.x - p_410224_.x * p_409711_.z;
      }

      static Leashable.Wrench accumulate(List<Leashable.Wrench> p_405866_) {
         if (p_405866_.isEmpty()) {
            return ZERO;
         }

         double d0 = 0.0;
         double d1 = 0.0;
         double d2 = 0.0;
         double d3 = 0.0;

         for (Leashable.Wrench leashable$wrench : p_405866_) {
            Vec3 vec3 = leashable$wrench.force;
            d0 += vec3.x;
            d1 += vec3.y;
            d2 += vec3.z;
            d3 += leashable$wrench.torque;
         }

         return new Leashable.Wrench(new Vec3(d0, d1, d2), d3);
      }

      public Leashable.Wrench scale(double p_409872_) {
         return new Leashable.Wrench(this.force.scale(p_409872_), this.torque * p_409872_);
      }
   }
}
