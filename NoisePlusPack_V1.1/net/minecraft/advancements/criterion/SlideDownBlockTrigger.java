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

public class SlideDownBlockTrigger extends SimpleCriterionTrigger<SlideDownBlockTrigger.TriggerInstance> {
   @Override
   public Codec<SlideDownBlockTrigger.TriggerInstance> codec() {
      return SlideDownBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_459909_, BlockState p_451584_) {
      this.trigger(p_459909_, p_455829_ -> p_455829_.matches(p_451584_));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<SlideDownBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_456761_ -> p_456761_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SlideDownBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(SlideDownBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(SlideDownBlockTrigger.TriggerInstance::state)
               )
               .apply(p_456761_, SlideDownBlockTrigger.TriggerInstance::new)
         )
         .validate(SlideDownBlockTrigger.TriggerInstance::validate);

      private static DataResult<SlideDownBlockTrigger.TriggerInstance> validate(SlideDownBlockTrigger.TriggerInstance p_455671_) {
         return p_455671_.block
            .<DataResult<SlideDownBlockTrigger.TriggerInstance>>flatMap(
               p_458343_ -> p_455671_.state
                  .<String>flatMap(p_460246_ -> p_460246_.checkState(((Block)p_458343_.value()).getStateDefinition()))
                  .map(p_457015_ -> DataResult.error(() -> "Block" + p_458343_ + " has no property " + p_457015_))
            )
            .orElseGet(() -> DataResult.success(p_455671_));
      }

      public static Criterion<SlideDownBlockTrigger.TriggerInstance> slidesDownBlock(Block p_458780_) {
         return CriteriaTriggers.HONEY_BLOCK_SLIDE
            .createCriterion(new SlideDownBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_458780_.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(BlockState p_450320_) {
         return this.block.isPresent() && !p_450320_.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(p_450320_);
      }
   }
}
