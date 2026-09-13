package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityTagPredicate(Optional<List<String>> anyOf, Optional<List<String>> allOf, Optional<List<String>> noneOf) implements EntitySubPredicate {
   public static final Codec<EntityTagPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("any_of").forGetter(EntityTagPredicate::anyOf),
            Codec.STRING.listOf().optionalFieldOf("all_of").forGetter(EntityTagPredicate::allOf),
            Codec.STRING.listOf().optionalFieldOf("none_of").forGetter(EntityTagPredicate::noneOf)
         )
         .apply(i, EntityTagPredicate::new)
   );

   public boolean matches(final Set<String> tags) {
      if (this.anyOf.isPresent() && !containsAtLeastOne(tags, this.anyOf.get())) {
         return false;
      } else {
         return this.noneOf.isPresent() && containsAtLeastOne(tags, this.noneOf.get())
            ? false
            : !this.allOf.isPresent() || containsAllOf(tags, this.allOf.get());
      }
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.matches(entity.entityTags());
   }

   private static boolean containsAtLeastOne(final Set<String> provided, final List<String> tags) {
      for (String tag : tags) {
         if (provided.contains(tag)) {
            return true;
         }
      }

      return false;
   }

   private static boolean containsAllOf(final Set<String> provided, final List<String> tags) {
      for (String tag : tags) {
         if (!provided.contains(tag)) {
            return false;
         }
      }

      return true;
   }
}
