package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record FishingHookPredicate(Optional<Boolean> inOpenWater) implements EntitySubPredicate {
   public static final FishingHookPredicate ANY = new FishingHookPredicate(Optional.empty());
   public static final Codec<FishingHookPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.BOOL.optionalFieldOf("in_open_water").forGetter(FishingHookPredicate::inOpenWater)).apply(i, FishingHookPredicate::new)
   );

   public static FishingHookPredicate inOpenWater(final boolean requirement) {
      return new FishingHookPredicate(Optional.of(requirement));
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      if (this.inOpenWater.isEmpty()) {
         return true;
      } else {
         return entity instanceof FishingHook hook ? this.inOpenWater.get() == hook.isOpenWaterFishing() : false;
      }
   }
}
