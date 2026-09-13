package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public record EntityPredicate(
   Optional<EntityTypePredicate> entityType,
   Optional<DistancePredicate> distanceToPlayer,
   Optional<MovementPredicate> movement,
   EntityPredicate.LocationWrapper location,
   Optional<MobEffectsPredicate> effects,
   Optional<NbtPredicate> nbt,
   Optional<EntityFlagsPredicate> flags,
   Optional<EntityEquipmentPredicate> equipment,
   Optional<EntitySubPredicate> subPredicate,
   Optional<Integer> periodicTick,
   Optional<EntityPredicate> vehicle,
   Optional<EntityPredicate> passenger,
   Optional<EntityPredicate> targetedEntity,
   Optional<String> team,
   Optional<SlotsPredicate> slots,
   DataComponentMatchers components
) {
   public static final Codec<EntityPredicate> CODEC = Codec.recursive(
      "EntityPredicate",
      p_452527_ -> RecordCodecBuilder.create(
         p_450970_ -> p_450970_.group(
               EntityTypePredicate.CODEC.optionalFieldOf("type").forGetter(EntityPredicate::entityType),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(EntityPredicate::distanceToPlayer),
               MovementPredicate.CODEC.optionalFieldOf("movement").forGetter(EntityPredicate::movement),
               EntityPredicate.LocationWrapper.CODEC.forGetter(EntityPredicate::location),
               MobEffectsPredicate.CODEC.optionalFieldOf("effects").forGetter(EntityPredicate::effects),
               NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(EntityPredicate::nbt),
               EntityFlagsPredicate.CODEC.optionalFieldOf("flags").forGetter(EntityPredicate::flags),
               EntityEquipmentPredicate.CODEC.optionalFieldOf("equipment").forGetter(EntityPredicate::equipment),
               EntitySubPredicate.CODEC.optionalFieldOf("type_specific").forGetter(EntityPredicate::subPredicate),
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("periodic_tick").forGetter(EntityPredicate::periodicTick),
               p_452527_.optionalFieldOf("vehicle").forGetter(EntityPredicate::vehicle),
               p_452527_.optionalFieldOf("passenger").forGetter(EntityPredicate::passenger),
               p_452527_.optionalFieldOf("targeted_entity").forGetter(EntityPredicate::targetedEntity),
               Codec.STRING.optionalFieldOf("team").forGetter(EntityPredicate::team),
               SlotsPredicate.CODEC.optionalFieldOf("slots").forGetter(EntityPredicate::slots),
               DataComponentMatchers.CODEC.forGetter(EntityPredicate::components)
            )
            .apply(p_450970_, EntityPredicate::new)
      )
   );
   public static final Codec<ContextAwarePredicate> ADVANCEMENT_CODEC = Codec.withAlternative(ContextAwarePredicate.CODEC, CODEC, EntityPredicate::wrap);

   public static ContextAwarePredicate wrap(EntityPredicate.Builder p_452803_) {
      return wrap(p_452803_.build());
   }

   public static Optional<ContextAwarePredicate> wrap(Optional<EntityPredicate> p_457353_) {
      return p_457353_.map(EntityPredicate::wrap);
   }

   public static List<ContextAwarePredicate> wrap(EntityPredicate.Builder... p_456992_) {
      return Stream.of(p_456992_).map(EntityPredicate::wrap).toList();
   }

   public static ContextAwarePredicate wrap(EntityPredicate p_456088_) {
      LootItemCondition lootitemcondition = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, p_456088_).build();
      return new ContextAwarePredicate(List.of(lootitemcondition));
   }

   public boolean matches(ServerPlayer p_455345_, @Nullable Entity p_451383_) {
      return this.matches(p_455345_.level(), p_455345_.position(), p_451383_);
   }

   public boolean matches(ServerLevel p_454176_, @Nullable Vec3 p_459224_, @Nullable Entity p_457793_) {
      if (p_457793_ == null) {
         return false;
      }

      if (this.entityType.isPresent() && !this.entityType.get().matches(p_457793_.getType())) {
         return false;
      }

      if (p_459224_ == null) {
         if (this.distanceToPlayer.isPresent()) {
            return false;
         }
      } else if (this.distanceToPlayer.isPresent()
         && !this.distanceToPlayer.get().matches(p_459224_.x, p_459224_.y, p_459224_.z, p_457793_.getX(), p_457793_.getY(), p_457793_.getZ())) {
         return false;
      }

      if (this.movement.isPresent()) {
         Vec3 vec3 = p_457793_.getKnownMovement();
         Vec3 vec31 = vec3.scale(20.0);
         if (!this.movement.get().matches(vec31.x, vec31.y, vec31.z, p_457793_.fallDistance)) {
            return false;
         }
      }

      if (this.location.located.isPresent() && !this.location.located.get().matches(p_454176_, p_457793_.getX(), p_457793_.getY(), p_457793_.getZ())) {
         return false;
      }

      if (this.location.steppingOn.isPresent()) {
         Vec3 vec32 = Vec3.atCenterOf(p_457793_.getOnPos());
         if (!p_457793_.onGround() || !this.location.steppingOn.get().matches(p_454176_, vec32.x(), vec32.y(), vec32.z())) {
            return false;
         }
      }

      if (this.location.affectsMovement.isPresent()) {
         Vec3 vec33 = Vec3.atCenterOf(p_457793_.getBlockPosBelowThatAffectsMyMovement());
         if (!this.location.affectsMovement.get().matches(p_454176_, vec33.x(), vec33.y(), vec33.z())) {
            return false;
         }
      }

      if (this.effects.isPresent() && !this.effects.get().matches(p_457793_)) {
         return false;
      }

      if (this.flags.isPresent() && !this.flags.get().matches(p_457793_)) {
         return false;
      }

      if (this.equipment.isPresent() && !this.equipment.get().matches(p_457793_)) {
         return false;
      }

      if (this.subPredicate.isPresent() && !this.subPredicate.get().matches(p_457793_, p_454176_, p_459224_)) {
         return false;
      }

      if (this.vehicle.isPresent() && !this.vehicle.get().matches(p_454176_, p_459224_, p_457793_.getVehicle())) {
         return false;
      }

      if (this.passenger.isPresent()
         && p_457793_.getPassengers().stream().noneMatch(p_450264_ -> this.passenger.get().matches(p_454176_, p_459224_, p_450264_))) {
         return false;
      }

      if (this.targetedEntity.isPresent()
         && !this.targetedEntity.get().matches(p_454176_, p_459224_, p_457793_ instanceof Mob ? ((Mob)p_457793_).getTarget() : null)) {
         return false;
      }

      if (this.periodicTick.isPresent() && p_457793_.tickCount % this.periodicTick.get() != 0) {
         return false;
      }

      if (this.team.isPresent()) {
         Team team = p_457793_.getTeam();
         if (team == null || !this.team.get().equals(team.getName())) {
            return false;
         }
      }

      if (this.slots.isPresent() && !this.slots.get().matches(p_457793_)) {
         return false;
      } else {
         return !this.components.test(p_457793_) ? false : this.nbt.isEmpty() || this.nbt.get().matches(p_457793_);
      }
   }

   public static LootContext createContext(ServerPlayer p_454169_, Entity p_456251_) {
      LootParams lootparams = new LootParams.Builder(p_454169_.level())
         .withParameter(LootContextParams.THIS_ENTITY, p_456251_)
         .withParameter(LootContextParams.ORIGIN, p_454169_.position())
         .create(LootContextParamSets.ADVANCEMENT_ENTITY);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   public static class Builder {
      private Optional<EntityTypePredicate> entityType = Optional.empty();
      private Optional<DistancePredicate> distanceToPlayer = Optional.empty();
      private Optional<MovementPredicate> movement = Optional.empty();
      private Optional<LocationPredicate> located = Optional.empty();
      private Optional<LocationPredicate> steppingOnLocation = Optional.empty();
      private Optional<LocationPredicate> movementAffectedBy = Optional.empty();
      private Optional<MobEffectsPredicate> effects = Optional.empty();
      private Optional<NbtPredicate> nbt = Optional.empty();
      private Optional<EntityFlagsPredicate> flags = Optional.empty();
      private Optional<EntityEquipmentPredicate> equipment = Optional.empty();
      private Optional<EntitySubPredicate> subPredicate = Optional.empty();
      private Optional<Integer> periodicTick = Optional.empty();
      private Optional<EntityPredicate> vehicle = Optional.empty();
      private Optional<EntityPredicate> passenger = Optional.empty();
      private Optional<EntityPredicate> targetedEntity = Optional.empty();
      private Optional<String> team = Optional.empty();
      private Optional<SlotsPredicate> slots = Optional.empty();
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      public static EntityPredicate.Builder entity() {
         return new EntityPredicate.Builder();
      }

      public EntityPredicate.Builder of(HolderGetter<EntityType<?>> p_455673_, EntityType<?> p_460224_) {
         this.entityType = Optional.of(EntityTypePredicate.of(p_455673_, p_460224_));
         return this;
      }

      public EntityPredicate.Builder of(HolderGetter<EntityType<?>> p_454898_, TagKey<EntityType<?>> p_451343_) {
         this.entityType = Optional.of(EntityTypePredicate.of(p_454898_, p_451343_));
         return this;
      }

      public EntityPredicate.Builder entityType(EntityTypePredicate p_452656_) {
         this.entityType = Optional.of(p_452656_);
         return this;
      }

      public EntityPredicate.Builder distance(DistancePredicate p_458221_) {
         this.distanceToPlayer = Optional.of(p_458221_);
         return this;
      }

      public EntityPredicate.Builder moving(MovementPredicate p_454678_) {
         this.movement = Optional.of(p_454678_);
         return this;
      }

      public EntityPredicate.Builder located(LocationPredicate.Builder p_451160_) {
         this.located = Optional.of(p_451160_.build());
         return this;
      }

      public EntityPredicate.Builder steppingOn(LocationPredicate.Builder p_451936_) {
         this.steppingOnLocation = Optional.of(p_451936_.build());
         return this;
      }

      public EntityPredicate.Builder movementAffectedBy(LocationPredicate.Builder p_460854_) {
         this.movementAffectedBy = Optional.of(p_460854_.build());
         return this;
      }

      public EntityPredicate.Builder effects(MobEffectsPredicate.Builder p_457904_) {
         this.effects = p_457904_.build();
         return this;
      }

      public EntityPredicate.Builder nbt(NbtPredicate p_452592_) {
         this.nbt = Optional.of(p_452592_);
         return this;
      }

      public EntityPredicate.Builder flags(EntityFlagsPredicate.Builder p_455351_) {
         this.flags = Optional.of(p_455351_.build());
         return this;
      }

      public EntityPredicate.Builder equipment(EntityEquipmentPredicate.Builder p_459890_) {
         this.equipment = Optional.of(p_459890_.build());
         return this;
      }

      public EntityPredicate.Builder equipment(EntityEquipmentPredicate p_453954_) {
         this.equipment = Optional.of(p_453954_);
         return this;
      }

      public EntityPredicate.Builder subPredicate(EntitySubPredicate p_454557_) {
         this.subPredicate = Optional.of(p_454557_);
         return this;
      }

      public EntityPredicate.Builder periodicTick(int p_457856_) {
         this.periodicTick = Optional.of(p_457856_);
         return this;
      }

      public EntityPredicate.Builder vehicle(EntityPredicate.Builder p_451007_) {
         this.vehicle = Optional.of(p_451007_.build());
         return this;
      }

      public EntityPredicate.Builder passenger(EntityPredicate.Builder p_455656_) {
         this.passenger = Optional.of(p_455656_.build());
         return this;
      }

      public EntityPredicate.Builder targetedEntity(EntityPredicate.Builder p_457961_) {
         this.targetedEntity = Optional.of(p_457961_.build());
         return this;
      }

      public EntityPredicate.Builder team(String p_458153_) {
         this.team = Optional.of(p_458153_);
         return this;
      }

      public EntityPredicate.Builder slots(SlotsPredicate p_459138_) {
         this.slots = Optional.of(p_459138_);
         return this;
      }

      public EntityPredicate.Builder components(DataComponentMatchers p_455460_) {
         this.components = p_455460_;
         return this;
      }

      public EntityPredicate build() {
         return new EntityPredicate(
            this.entityType,
            this.distanceToPlayer,
            this.movement,
            new EntityPredicate.LocationWrapper(this.located, this.steppingOnLocation, this.movementAffectedBy),
            this.effects,
            this.nbt,
            this.flags,
            this.equipment,
            this.subPredicate,
            this.periodicTick,
            this.vehicle,
            this.passenger,
            this.targetedEntity,
            this.team,
            this.slots,
            this.components
         );
      }
   }

   public record LocationWrapper(Optional<LocationPredicate> located, Optional<LocationPredicate> steppingOn, Optional<LocationPredicate> affectsMovement) {
      public static final MapCodec<EntityPredicate.LocationWrapper> CODEC = RecordCodecBuilder.mapCodec(
         p_452237_ -> p_452237_.group(
               LocationPredicate.CODEC.optionalFieldOf("location").forGetter(EntityPredicate.LocationWrapper::located),
               LocationPredicate.CODEC.optionalFieldOf("stepping_on").forGetter(EntityPredicate.LocationWrapper::steppingOn),
               LocationPredicate.CODEC.optionalFieldOf("movement_affected_by").forGetter(EntityPredicate.LocationWrapper::affectsMovement)
            )
            .apply(p_452237_, EntityPredicate.LocationWrapper::new)
      );
   }
}
