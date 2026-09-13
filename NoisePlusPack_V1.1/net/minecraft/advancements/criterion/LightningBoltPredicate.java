package net.minecraft.advancements.criterion;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record LightningBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, Optional<EntityPredicate> entityStruck) implements EntitySubPredicate {
   public static final MapCodec<LightningBoltPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_455455_ -> p_455455_.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("blocks_set_on_fire", MinMaxBounds.Ints.ANY).forGetter(LightningBoltPredicate::blocksSetOnFire),
            EntityPredicate.CODEC.optionalFieldOf("entity_struck").forGetter(LightningBoltPredicate::entityStruck)
         )
         .apply(p_455455_, LightningBoltPredicate::new)
   );

   public static LightningBoltPredicate blockSetOnFire(MinMaxBounds.Ints p_459681_) {
      return new LightningBoltPredicate(p_459681_, Optional.empty());
   }

   @Override
   public MapCodec<LightningBoltPredicate> codec() {
      return EntitySubPredicates.LIGHTNING;
   }

   @Override
   public boolean matches(Entity p_458423_, ServerLevel p_456850_, @Nullable Vec3 p_458382_) {
      return p_458423_ instanceof LightningBolt lightningbolt
         ? this.blocksSetOnFire.matches(lightningbolt.getBlocksSetOnFire())
            && (
               this.entityStruck.isEmpty()
                  || lightningbolt.getHitEntities().anyMatch(p_457308_ -> this.entityStruck.get().matches(p_456850_, p_458382_, p_457308_))
            )
         : false;
   }
}
