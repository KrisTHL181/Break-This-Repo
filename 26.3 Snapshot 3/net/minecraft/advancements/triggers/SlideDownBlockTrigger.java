package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
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

   public void trigger(final ServerPlayer player, final BlockState state) {
      this.trigger(player, t -> t.matches(state));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      // ===== 修改：RecordCodecBuilder.create 显式类型参数，validate 改为 lambda =====
      public static final Codec<SlideDownBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.<TriggerInstance>create(
            i -> i.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SlideDownBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(SlideDownBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(SlideDownBlockTrigger.TriggerInstance::state)
               )
               .apply(i, SlideDownBlockTrigger.TriggerInstance::new)
         )
         .validate((SlideDownBlockTrigger.TriggerInstance t) -> SlideDownBlockTrigger.TriggerInstance.validate(t));

      private static DataResult<SlideDownBlockTrigger.TriggerInstance> validate(final SlideDownBlockTrigger.TriggerInstance trigger) {
         return trigger.block
            .<DataResult<SlideDownBlockTrigger.TriggerInstance>>flatMap(
               block -> trigger.state
                  .<String>flatMap(state -> state.checkState(((Block)block.value()).getStateDefinition()))
                  .map(property -> DataResult.error(() -> "Block" + block + " has no property " + property))
            )
            .orElseGet(() -> DataResult.success(trigger));
      }

      public static Criterion<SlideDownBlockTrigger.TriggerInstance> slidesDownBlock(final Block block) {
         return CriteriaTriggers.HONEY_BLOCK_SLIDE
            .createCriterion(new SlideDownBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(block.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(final BlockState state) {
         return this.block.isPresent() && !state.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(state);
      }
   }
}