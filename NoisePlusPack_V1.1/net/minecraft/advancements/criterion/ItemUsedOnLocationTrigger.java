package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class ItemUsedOnLocationTrigger extends SimpleCriterionTrigger<ItemUsedOnLocationTrigger.TriggerInstance> {
   @Override
   public Codec<ItemUsedOnLocationTrigger.TriggerInstance> codec() {
      return ItemUsedOnLocationTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_451257_, BlockPos p_459944_, ItemStack p_457762_) {
      ServerLevel serverlevel = p_451257_.level();
      BlockState blockstate = serverlevel.getBlockState(p_459944_);
      LootParams lootparams = new LootParams.Builder(serverlevel)
         .withParameter(LootContextParams.ORIGIN, p_459944_.getCenter())
         .withParameter(LootContextParams.THIS_ENTITY, p_451257_)
         .withParameter(LootContextParams.BLOCK_STATE, blockstate)
         .withParameter(LootContextParams.TOOL, p_457762_)
         .create(LootContextParamSets.ADVANCEMENT_LOCATION);
      LootContext lootcontext = new LootContext.Builder(lootparams).create(Optional.empty());
      this.trigger(p_451257_, p_453896_ -> p_453896_.matches(lootcontext));
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> location)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<ItemUsedOnLocationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_453470_ -> p_453470_.group(
               EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::player),
               ContextAwarePredicate.CODEC.optionalFieldOf("location").forGetter(ItemUsedOnLocationTrigger.TriggerInstance::location)
            )
            .apply(p_453470_, ItemUsedOnLocationTrigger.TriggerInstance::new)
      );

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(Block p_451050_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_451050_).build()
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlock(LootItemCondition.Builder... p_456334_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            Arrays.stream(p_456334_).map(LootItemCondition.Builder::build).toArray(LootItemCondition[]::new)
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static <T extends Comparable<T>> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_450524_, Property<T> p_458102_, String p_454263_
      ) {
         StatePropertiesPredicate.Builder statepropertiespredicate$builder = StatePropertiesPredicate.Builder.properties().hasProperty(p_458102_, p_454263_);
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_450524_).setProperties(statepropertiespredicate$builder).build()
         );
         return CriteriaTriggers.PLACED_BLOCK
            .createCriterion(new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate)));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_453156_, Property<Boolean> p_454164_, boolean p_459407_
      ) {
         return placedBlockWithProperties(p_453156_, p_454164_, String.valueOf(p_459407_));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(Block p_453493_, Property<Integer> p_453742_, int p_450350_) {
         return placedBlockWithProperties(p_453493_, p_453742_, String.valueOf(p_450350_));
      }

      public static <T extends Comparable<T> & StringRepresentable> Criterion<ItemUsedOnLocationTrigger.TriggerInstance> placedBlockWithProperties(
         Block p_458233_, Property<T> p_456180_, T p_458246_
      ) {
         return placedBlockWithProperties(p_458233_, p_456180_, p_458246_.getSerializedName());
      }

      private static ItemUsedOnLocationTrigger.TriggerInstance itemUsedOnLocation(LocationPredicate.Builder p_452084_, ItemPredicate.Builder p_459480_) {
         ContextAwarePredicate contextawarepredicate = ContextAwarePredicate.create(
            LocationCheck.checkLocation(p_452084_).build(), MatchTool.toolMatches(p_459480_).build()
         );
         return new ItemUsedOnLocationTrigger.TriggerInstance(Optional.empty(), Optional.of(contextawarepredicate));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> itemUsedOnBlock(LocationPredicate.Builder p_458491_, ItemPredicate.Builder p_451200_) {
         return CriteriaTriggers.ITEM_USED_ON_BLOCK.createCriterion(itemUsedOnLocation(p_458491_, p_451200_));
      }

      public static Criterion<ItemUsedOnLocationTrigger.TriggerInstance> allayDropItemOnBlock(
         LocationPredicate.Builder p_457869_, ItemPredicate.Builder p_460101_
      ) {
         return CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.createCriterion(itemUsedOnLocation(p_457869_, p_460101_));
      }

      public boolean matches(LootContext p_452112_) {
         return this.location.isEmpty() || this.location.get().matches(p_452112_);
      }

      @Override
      public void validate(CriterionValidator p_453525_) {
         SimpleCriterionTrigger.SimpleInstance.super.validate(p_453525_);
         this.location.ifPresent(p_458341_ -> p_453525_.validate(p_458341_, LootContextParamSets.ADVANCEMENT_LOCATION, "location"));
      }
   }
}
