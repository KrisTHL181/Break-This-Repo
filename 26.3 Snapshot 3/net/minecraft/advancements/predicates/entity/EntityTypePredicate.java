package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityTypePredicate(HolderSet<EntityType<?>> types) implements EntitySubPredicate {
   public static final Codec<EntityTypePredicate> CODEC = RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
      .xmap(EntityTypePredicate::new, EntityTypePredicate::types);

   public static EntityTypePredicate of(final HolderGetter<EntityType<?>> lookup, final EntityType<?> type) {
      return new EntityTypePredicate(HolderSet.direct(type.builtInRegistryHolder()));
   }

   public static EntityTypePredicate of(final HolderGetter<EntityType<?>> lookup, final TagKey<EntityType<?>> type) {
      return new EntityTypePredicate(lookup.getOrThrow(type));
   }

   public boolean matches(final Holder<EntityType<?>> type) {
      return this.types.contains(type);
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.matches(entity.typeHolder());
   }
}
