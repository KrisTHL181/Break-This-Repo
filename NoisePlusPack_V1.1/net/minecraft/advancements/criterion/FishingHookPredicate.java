package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record FishingHookPredicate(Optional<Boolean> inOpenWater) implements EntitySubPredicate {
   public static final FishingHookPredicate ANY = new FishingHookPredicate(Optional.empty());
   public static final MapCodec<FishingHookPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_459605_ -> p_459605_.group(Codec.BOOL.optionalFieldOf("in_open_water").forGetter(FishingHookPredicate::inOpenWater))
         .apply(p_459605_, FishingHookPredicate::new)
   );

   public static FishingHookPredicate inOpenWater(boolean p_457576_) {
      return new FishingHookPredicate(Optional.of(p_457576_));
   }

   @Override
   public MapCodec<FishingHookPredicate> codec() {
      return EntitySubPredicates.FISHING_HOOK;
   }

   @Override
   public boolean matches(Entity p_454883_, ServerLevel p_457629_, @Nullable Vec3 p_453482_) {
      if (this.inOpenWater.isEmpty()) {
         return true;
      } else {
         return p_454883_ instanceof FishingHook fishinghook ? this.inOpenWater.get() == fishinghook.isOpenWaterFishing() : false;
      }
   }
}
