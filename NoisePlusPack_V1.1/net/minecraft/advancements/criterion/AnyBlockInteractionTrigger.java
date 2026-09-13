package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger<AnyBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<AnyBlockInteractionTrigger.TriggerInstance> codec() {
      return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_456517_, BlockPos p_455887_, ItemStack p_453872_) {
      ServerLevel serverlevel = p_456517_.level();
      BlockState blockstate = serverlevel.getBlockState(p_455887_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_455887_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_456517_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withParameter(LootContextParams.TOOL, p_453872_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_456517_, p_451429_ -> p_451429_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_457300_ -> p_457300_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
            )
            .apply(p_457300_, AnyBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext p_450422_) {
         return this.location.isEmpty() || this.location.get().matches(p_450422_);
      }

      @Override
      public void validate(CriterionValidator p_456025_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_456025_);
         this.location.ifPresent(p_457009_ -> p_456025_.validate(p_457009_, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
      }
   }
}
