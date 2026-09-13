package net.minecraft.advancements.criterion;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record SlimePredicate(MinMaxBounds.Ints size) implements EntitySubPredicate {
   public static final MapCodec<SlimePredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_453733_ -> p_453733_.group(MinMaxBounds.Ints.CODEC.optionalFieldOf("size", MinMaxBounds.Ints.ANY).forGetter(SlimePredicate::size))
         .apply(p_453733_, SlimePredicate::new)
   );

   public static SlimePredicate sized(MinMaxBounds.Ints p_457568_) {
      return new SlimePredicate(p_457568_);
   }

   @Override
   public boolean matches(Entity p_455514_, ServerLevel p_452639_, @Nullable Vec3 p_460596_) {
      return p_455514_ instanceof Slime slime ? this.size.matches(slime.getSize()) : false;
   }

   @Override
   public MapCodec<SlimePredicate> codec() {
      return EntitySubPredicates.SLIME;
   }
}
