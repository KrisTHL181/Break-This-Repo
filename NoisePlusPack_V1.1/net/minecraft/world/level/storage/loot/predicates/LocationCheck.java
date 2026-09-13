package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LocationCheck(Optional<LocationPredicate> predicate, BlockPos offset) implements LootItemCondition {
   private static final MapCodec<BlockPos> OFFSET_CODEC = RecordCodecBuilder.mapCodec(
      p_327649_ -> p_327649_.group(
            Codec.INT.optionalFieldOf("offsetX", 0).forGetter(Vec3i::getX),
            Codec.INT.optionalFieldOf("offsetY", 0).forGetter(Vec3i::getY),
            Codec.INT.optionalFieldOf("offsetZ", 0).forGetter(Vec3i::getZ)
         )
         .apply(p_327649_, BlockPos::new)
   );
   public static final MapCodec<LocationCheck> CODEC = RecordCodecBuilder.mapCodec(
      p_450105_ -> p_450105_.group(
            LocationPredicate.CODEC.optionalFieldOf("predicate").forGetter(LocationCheck::predicate), OFFSET_CODEC.forGetter(LocationCheck::offset)
         )
         .apply(p_450105_, LocationCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.LOCATION_CHECK;
   }

   public boolean test(LootContext p_81731_) {
      Vec3 vec3 = p_81731_.getOptionalParameter(LootContextParams.ORIGIN);
      return vec3 != null
         && (
            this.predicate.isEmpty()
               || this.predicate
                  .get()
                  .matches(p_81731_.getLevel(), vec3.x() + this.offset.getX(), vec3.y() + this.offset.getY(), vec3.z() + this.offset.getZ())
         );
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_453066_) {
      return () -> new LocationCheck(Optional.of(p_453066_.build()), BlockPos.ZERO);
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_450481_, BlockPos p_81729_) {
      return () -> new LocationCheck(Optional.of(p_450481_.build()), p_81729_);
   }
}
