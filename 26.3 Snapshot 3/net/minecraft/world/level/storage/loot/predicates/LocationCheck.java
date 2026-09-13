package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LocationCheck(Optional<LocationPredicate> predicate, Vec3i offset) implements LootItemCondition {
   private static final MapCodec<Vec3i> OFFSET_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Codec.INT.optionalFieldOf("offsetX", 0).forGetter(Vec3i::getX),
            Codec.INT.optionalFieldOf("offsetY", 0).forGetter(Vec3i::getY),
            Codec.INT.optionalFieldOf("offsetZ", 0).forGetter(Vec3i::getZ)
         )
         .apply(i, Vec3i::new)
   );
   public static final MapCodec<LocationCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LocationPredicate.CODEC.optionalFieldOf("predicate").forGetter(LocationCheck::predicate), OFFSET_CODEC.forGetter(LocationCheck::offset))
         .apply(i, LocationCheck::new)
   );

   @Override
   public MapCodec<LocationCheck> codec() {
      return MAP_CODEC;
   }

   public boolean test(final LootContext context) {
      Vec3 pos = context.getOptionalParameter(LootContextParams.ORIGIN);
      return pos != null
         && (
            this.predicate.isEmpty()
               || this.predicate.get().matches(context.getLevel(), pos.x() + this.offset.getX(), pos.y() + this.offset.getY(), pos.z() + this.offset.getZ())
         );
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   public static LootItemCondition.Builder checkLocation(final LocationPredicate.Builder predicate) {
      return () -> new LocationCheck(Optional.of(predicate.build()), Vec3i.ZERO);
   }

   public static LootItemCondition.Builder checkLocation(final LocationPredicate.Builder predicate, final Vec3i offset) {
      return () -> new LocationCheck(Optional.of(predicate.build()), offset);
   }

   public static LootItemCondition.Builder checkLocation(final LocationPredicate.Builder predicate, final Direction direction) {
      return checkLocation(predicate, direction.getUnitVec3i());
   }
}
