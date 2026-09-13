package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class AnyBlockInteractionTrigger extends SimpleCriterionTrigger<AnyBlockInteractionTrigger.TriggerInstance> {
   @Override
   public Codec<AnyBlockInteractionTrigger.TriggerInstance> codec() {
      return AnyBlockInteractionTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player, final BlockPos pos, final ItemInstance tool) {
      ServerLevel level = player.level();
      BlockState state = level.getBlockState(pos);
      LootParams params = new LootParams.Builder(level)
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
         .withParameter(LootContextParams.THIS_ENTITY, player)
         .withParameter(LootContextParams.BLOCK_STATE, state)
         .withParameter(LootContextParams.TOOL, tool)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext context = new LootContext.Builder(params).create(Optional.empty());
      this.trigger(player, t -> t.matches(context));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<AnyBlockInteractionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(AnyBlockInteractionTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(AnyBlockInteractionTrigger.TriggerInstance::location)
            )
            .apply(i, AnyBlockInteractionTrigger.TriggerInstance::new)
      );

      public boolean matches(final LootContext locationContext) {
         return this.location.isEmpty() || this.location.get().matches(locationContext);
      }

      @Override
      public void validate(final ValidationContextSource validator) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
         Validatable.validate(validator.context(LootContextParamSets.ADVANCEMENT_LOCATION), "location", this.location);
      }
   }
}
