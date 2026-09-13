package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.phys.Vec3;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   @Override
   public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
      return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
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
      public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
            )
            .apply(i, ItemUsedOnLocationTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(final Block block) {
         ContextAwarePredicate location = ContextAwarePredicate.create(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).build());
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(location)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(final LootItemCondition.Builder... conditions) {
         ContextAwarePredicate location = ContextAwarePredicate.create(
            Arrays.stream(conditions).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
         );
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(location)));
      }

      public static <T extends Comparable<T>> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         final Block block, final Property<T> property, final String propertyValue
      ) {
         StatePropertiesPredicate.Builder predicateBuilder = StatePropertiesPredicate.Builder.properties().hasProperty(property, propertyValue);
         ContextAwarePredicate location = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(predicateBuilder).build()
         );
         return CriteriaTriggers.PLACED_BLOCK.createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(location)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         final Block block, final Property<Boolean> property, final boolean propertyValue
      ) {
         return placedBlockWithProperties(block, property, String.valueOf(propertyValue));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         final Block block, final Property<Integer> property, final int propertyValue
      ) {
         return placedBlockWithProperties(block, property, String.valueOf(propertyValue));
      }

      public static <T extends Comparable<T> & StringRepresentable> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         final Block block, final Property<T> properties, final T propertyValue
      ) {
         return placedBlockWithProperties(block, properties, propertyValue.getSerializedName());
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(final LocationPredicate.Builder location, final ItemPredicate.Builder item) {
         ContextAwarePredicate predicate = ContextAwarePredicate.create(LocationCheck.checkLocation(location).build(), MatchTool.toolMatches(item).build());
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(predicate));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(
         final LocationPredicate.Builder location, final ItemPredicate.Builder item
      ) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(location, item));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(
         final LocationPredicate.Builder location, final ItemPredicate.Builder item
      ) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(location, item));
      }

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
