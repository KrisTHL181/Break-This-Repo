package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetFireworksFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetFireworksFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               ListOperation.StandAlone.codec(FireworkExplosion.CODEC, 256).optionalFieldOf("explosions").forGetter(f -> f.explosions),
               ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("flight_duration").forGetter(f -> f.flightDuration)
            )
         )
         .apply(i, SetFireworksFunction::new)
   );
   public static final Fireworks DEFAULT_VALUE = new Fireworks(0, List.of());
   private final Optional<ListOperation.StandAlone<FireworkExplosion>> explosions;
   private final Optional<Integer> flightDuration;

   protected SetFireworksFunction(
      final List<LootItemCondition> predicates, final Optional<ListOperation.StandAlone<FireworkExplosion>> explosions, final Optional<Integer> flightDuration
   ) {
      super(predicates);
      this.explosions = explosions;
      this.flightDuration = flightDuration;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.FIREWORKS, DEFAULT_VALUE, this::apply);
      return itemStack;
   }

   private Fireworks apply(final Fireworks old) {
      return new Fireworks(
         this.flightDuration.orElseGet(old::flightDuration),
         this.explosions.<List<FireworkExplosion>>map(operation -> operation.apply(old.explosions())).orElse(old.explosions())
      );
   }

   @Override
   public MapCodec<SetFireworksFunction> codec() {
      return MAP_CODEC;
   }
}
