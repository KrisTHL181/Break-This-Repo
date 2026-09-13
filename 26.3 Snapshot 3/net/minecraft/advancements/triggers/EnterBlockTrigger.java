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

public class EnterBlockTrigger extends SimpleCriterionTrigger<EnterBlockTrigger.TriggerInstance> {
   @Override
   public Codec<EnterBlockTrigger.TriggerInstance> codec() {
      return EnterBlockTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final BlockState state) {
      this.trigger(player, t -> t.matches(state));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<StatePropertiesPredicate> state)
      implements SimpleCriterionTrigger.SimpleInstance {
      // ===== 修改：RecordCodecBuilder.create 显式类型参数，validate 改为 lambda =====
      public static final Codec<EnterBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.<EnterBlockTrigger.TriggerInstance>create(
            i -> i.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(EnterBlockTrigger.TriggerInstance::player),
                  BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(EnterBlockTrigger.TriggerInstance::block),
                  StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(EnterBlockTrigger.TriggerInstance::state)
               )
               .apply(i, EnterBlockTrigger.TriggerInstance::new)
         )
         .validate((EnterBlockTrigger.TriggerInstance t) -> EnterBlockTrigger.TriggerInstance.validate(t));

      private static DataResult<EnterBlockTrigger.TriggerInstance> validate(final EnterBlockTrigger.TriggerInstance trigger) {
         return trigger.block
            .<DataResult<EnterBlockTrigger.TriggerInstance>>flatMap(
               block -> trigger.state
                  .<String>flatMap(state -> state.checkState(((Block)block.value()).getStateDefinition()))
                  .map(property -> DataResult.error(() -> "Block" + block + " has no property " + property))
            )
            .orElseGet(() -> DataResult.success(trigger));
      }

      public static Criterion<EnterBlockTrigger.TriggerInstance> entersBlock(final Block block) {
         return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(block.builtInRegistryHolder()), Optional.empty()));
      }

      public boolean matches(final BlockState state) {
         return this.block.isPresent() && !state.is(this.block.get()) ? false : !this.state.isPresent() || this.state.get().matches(state);
      }
   }
}