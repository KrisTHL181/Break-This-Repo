package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class DefaultBlockInteractionTrigger extends SimpleCriterionTrigger<DefaultBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<DefaultBlockInteractionTrigger.TriggerInstance> codec() {
      return DefaultBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_455031_, BlockPos p_457422_) {
      ServerLevel serverlevel = p_455031_.level();
      BlockState blockstate = serverlevel.getBlockState(p_457422_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_457422_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_455031_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .create(LootContextParamSets.BLOCK_USE);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_455031_, p_456693_ -> p_456693_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<DefaultBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_454955_ -> p_454955_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(DefaultBlockInteractionTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(DefaultBlockInteractionTrigger.TriggerInstance::location)
            )
            .apply(p_454955_, DefaultBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(LootContext p_451295_) {
         return this.location.isEmpty() || this.location.get().matches(p_451295_);
      }

      @Override
      public void validate(CriterionValidator p_457371_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_457371_);
         this.location.ifPresent(p_452043_ -> p_457371_.validate(p_452043_, LootContextParamSets.BLOCK_USE, "location"));
      }
   }
}
