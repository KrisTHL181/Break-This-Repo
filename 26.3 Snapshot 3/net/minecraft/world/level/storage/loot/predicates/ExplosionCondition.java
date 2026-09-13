package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ExplosionCondition implements LootItemCondition {
   private static final ExplosionCondition INSTANCE = new ExplosionCondition();
   public static final MapCodec<ExplosionCondition> MAP_CODEC = MapCodec.unit(INSTANCE);

   private ExplosionCondition() {
   }

   @Override
   public MapCodec<ExplosionCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.EXPLOSION_RADIUS);
   }

   public boolean test(final LootContext context) {
      Float explosionRadius = context.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
      if (explosionRadius != null) {
         RandomSource random = context.getRandom();
         float probability = 1.0F / explosionRadius;
         return random.nextFloat() <= probability;
      } else {
         return true;
      }
   }

   public static LootItemCondition.Builder survivesExplosion() {
      return () -> INSTANCE;
   }
}
