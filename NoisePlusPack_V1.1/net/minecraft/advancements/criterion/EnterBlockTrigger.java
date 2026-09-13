package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   @Override
   public Codec<EnterBlockTrigger.TriggerInstance> codec() {
      return EnterBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_457190_, BlockState p_455452_) {
      this.trigger(p_457190_, p_450141_ -> p_450141_.matches(p_455452_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<EnterBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_460514_ -> p_460514_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EnterBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(EnterBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(EnterBlockTrigger.TriggerInstance::state)
               )
               .apply(p_460514_, EnterBlockTrigger.TriggerInstance::new)
         )
         .validate(EnterBlockTrigger.TriggerInstance::validate);

      private static DataResult<EnterBlockTrigger.TriggerInstance> validate(EnterBlockTrigger.TriggerInstance p_454415_) {
         return p_454415_.block
            .<DataResult<EnterBlockTrigger.TriggerInstance>>flatMap(
               p_452986_ -> p_454415_.state
                  .<String>flatMap(p_456278_ -> p_456278_.checkState(((Block)p_452986_.value()).getStateDefinition()))
                  .map(p_460784_ -> DataResult.error(() -> "Block" + p_452986_ + " has no property " + p_460784_))
            )
            .orElseGet(() -> DataResult.success(p_454415_));
      }

      public static Criterion<EnterBlockTrigger.TriggerInstance> entersBlock(Block p_458725_) {
         return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_458725_.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(BlockState p_455358_) {
         return this.block.isPresent() && !p_455358_.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(p_455358_);
      }
   }
}
