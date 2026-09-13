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
   public static final MapCodec<SetFireworksFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_333231_ -> commonFields(p_333231_)
         .and(
            p_333231_.group(
               ListOperation.StandAlone.codec(FireworkExplosion.CODEC, 256).optionalFieldOf("explosions").forGetter(p_333881_ -> p_333881_.explosions),
               ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("flight_duration").forGetter(p_335946_ -> p_335946_.flightDuration)
            )
         )
         .apply(p_333231_, SetFireworksFunction::new)
   );
   public static final Fireworks DEFAULT_VALUE = new Fireworks(0, List.of());
   private final Optional<ListOperation.StandAlone<FireworkExplosion>> explosions;
   private final Optional<Integer> flightDuration;

   protected SetFireworksFunction(
      List<LootItemCondition> p_335106_, Optional<ListOperation.StandAlone<FireworkExplosion>> p_334501_, Optional<Integer> p_334583_
   ) {
      super(p_335106_);
      this.explosions = p_334501_;
      this.flightDuration = p_334583_;
   }

   @Override
   protected ItemStack run(ItemStack p_331574_, LootContext p_328031_) {
      p_331574_.update(DataComponents.FIREWORKS, DEFAULT_VALUE, this::apply);
      return p_331574_;
   }

   private Fireworks apply(Fireworks p_332116_) {
      return new Fireworks(
         this.flightDuration.orElseGet(p_332116_::flightDuration),
         this.explosions.<List<FireworkExplosion>>map(p_331021_ -> p_331021_.apply(p_332116_.explosions())).orElse(p_332116_.explosions())
      );
   }

   @Override
   public LootItemFunctionType<SetFireworksFunction> getType() {
      return LootItemFunctions.SET_FIREWORKS;
   }
}
