package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetStewEffectFunction extends LootItemConditionalFunction {
   private static final Codec<List<SetStewEffectFunction.EffectEntry>> EFFECTS_LIST = SetStewEffectFunction.EffectEntry.CODEC.listOf().validate(p_297178_ -> {
      Set<Holder<MobEffect>> set = new ObjectOpenHashSet();

      for (SetStewEffectFunction.EffectEntry setsteweffectfunction$effectentry : p_297178_) {
         if (!set.add(setsteweffectfunction$effectentry.effect())) {
            return DataResult.error(() -> "Encountered duplicate mob effect: '" + setsteweffectfunction$effectentry.effect() + "'");
         }
      }

      return DataResult.success(p_297178_);
   });
   public static final MapCodec<SetStewEffectFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_327631_ -> commonFields(p_327631_)
         .and(EFFECTS_LIST.optionalFieldOf("effects", List.of()).forGetter(p_297176_ -> p_297176_.effects))
         .apply(p_327631_, SetStewEffectFunction::new)
   );
   private final List<SetStewEffectFunction.EffectEntry> effects;

   SetStewEffectFunction(List<LootItemCondition> p_298902_, List<SetStewEffectFunction.EffectEntry> p_298444_) {
      super(p_298902_);
      this.effects = p_298444_;
   }

   @Override
   public LootItemFunctionType<SetStewEffectFunction> getType() {
      return LootItemFunctions.SET_STEW_EFFECT;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.effects.stream().flatMap(p_450102_ -> p_450102_.duration().getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public ItemStack run(ItemStack p_81223_, LootContext p_81224_) {
      if (p_81223_.is(Items.SUSPICIOUS_STEW) && !this.effects.isEmpty()) {
         SetStewEffectFunction.EffectEntry setsteweffectfunction$effectentry = Util.getRandom(this.effects, p_81224_.getRandom());
         Holder<MobEffect> holder = setsteweffectfunction$effectentry.effect();
         int i = setsteweffectfunction$effectentry.duration().getInt(p_81224_);
         if (!holder.value().isInstantenous()) {
            i *= 20;
         }

         SuspiciousStewEffects.Entry suspicioussteweffects$entry = new SuspiciousStewEffects.Entry(holder, i);
         p_81223_.update(
            DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY, suspicioussteweffects$entry, SuspiciousStewEffects::withEffectAdded
         );
         return p_81223_;
      } else {
         return p_81223_;
      }
   }

   public static SetStewEffectFunction.Builder stewEffect() {
      return new SetStewEffectFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetStewEffectFunction.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<SetStewEffectFunction.EffectEntry> effects = ImmutableList.builder();

      protected SetStewEffectFunction.Builder getThis() {
         return this;
      }

      public SetStewEffectFunction.Builder withEffect(Holder<MobEffect> p_331394_, NumberProvider p_165474_) {
         this.effects.add(new SetStewEffectFunction.EffectEntry(p_331394_, p_165474_));
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetStewEffectFunction(this.getConditions(), this.effects.build());
      }
   }

   record EffectEntry(Holder<MobEffect> effect, NumberProvider duration) {
      public static final Codec<SetStewEffectFunction.EffectEntry> CODEC = RecordCodecBuilder.create(
         p_342013_ -> p_342013_.group(
               MobEffect.CODEC.fieldOf("type").forGetter(SetStewEffectFunction.EffectEntry::effect),
               NumberProviders.CODEC.fieldOf("duration").forGetter(SetStewEffectFunction.EffectEntry::duration)
            )
            .apply(p_342013_, SetStewEffectFunction.EffectEntry::new)
      );
   }
}
