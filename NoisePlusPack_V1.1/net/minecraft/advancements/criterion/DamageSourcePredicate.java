package net.minecraft.advancements.criterion;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(
   List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity, Optional<Boolean> isDirect
) {
   public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
      p_452982_ -> p_452982_.group(
            TagPredicate.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("tags", List.of()).forGetter(DamageSourcePredicate::tags),
            EntityPredicate.CODEC.optionalFieldOf("direct_entity").forGetter(DamageSourcePredicate::directEntity),
            EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamageSourcePredicate::sourceEntity),
            Codec.BOOL.optionalFieldOf("is_direct").forGetter(DamageSourcePredicate::isDirect)
         )
         .apply(p_452982_, DamageSourcePredicate::new)
   );

   public boolean matches(ServerPlayer p_453149_, DamageSource p_457680_) {
      return this.matches(p_453149_.level(), p_453149_.position(), p_457680_);
   }

   public boolean matches(ServerLevel p_455195_, Vec3 p_456381_, DamageSource p_456934_) {
      for (TagPredicate<DamageType> tagpredicate : this.tags) {
         if (!tagpredicate.matches(p_456934_.typeHolder())) {
            return false;
         }
      }

      if (this.directEntity.isPresent() && !this.directEntity.get().matches(p_455195_, p_456381_, p_456934_.getDirectEntity())) {
         return false;
      } else {
         return this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(p_455195_, p_456381_, p_456934_.getEntity())
            ? false
            : !this.isDirect.isPresent() || this.isDirect.get() == p_456934_.isDirect();
      }
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
      private Optional<EntityPredicate> directEntity = Optional.empty();
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> isDirect = Optional.empty();

      public static DamageSourcePredicate.Builder damageType() {
         return new DamageSourcePredicate.Builder();
      }

      public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> p_456139_) {
         this.tags.add(p_456139_);
         return this;
      }

      public DamageSourcePredicate.Builder direct(EntityPredicate.Builder p_460067_) {
         this.directEntity = Optional.of(p_460067_.build());
         return this;
      }

      public DamageSourcePredicate.Builder source(EntityPredicate.Builder p_459061_) {
         this.sourceEntity = Optional.of(p_459061_.build());
         return this;
      }

      public DamageSourcePredicate.Builder isDirect(boolean p_460844_) {
         this.isDirect = Optional.of(p_460844_);
         return this;
      }

      public DamageSourcePredicate build() {
         return new DamageSourcePredicate(this.tags.build(), this.directEntity, this.sourceEntity, this.isDirect);
      }
   }
}
