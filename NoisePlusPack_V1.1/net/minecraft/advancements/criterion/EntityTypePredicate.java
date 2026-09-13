package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record EntityTypePredicate(HolderSet<EntityType<?>> types) {
   public static final Codec<EntityTypePredicate> CODEC = RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
      .xmap(EntityTypePredicate::new, EntityTypePredicate::types);

   public static EntityTypePredicate of(HolderGetter<EntityType<?>> p_459504_, EntityType<?> p_457623_) {
      return new EntityTypePredicate(HolderSet.direct(p_457623_.builtInRegistryHolder()));
   }

   public static EntityTypePredicate of(HolderGetter<EntityType<?>> p_460031_, TagKey<EntityType<?>> p_455572_) {
      return new EntityTypePredicate(p_460031_.getOrThrow(p_455572_));
   }

   public boolean matches(EntityType<?> p_455797_) {
      return p_455797_.is(this.types);
   }
}
