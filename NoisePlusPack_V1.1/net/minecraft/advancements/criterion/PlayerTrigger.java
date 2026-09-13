package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger extends SimpleCriterionTrigger<PlayerTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerTrigger.TriggerInstance> codec() {
      return PlayerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer p_458108_) {
      this.trigger(p_458108_, p_452817_ -> true);
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_455690_ -> p_455690_.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerTrigger.TriggerInstance::player))
            .apply(p_455690_, PlayerTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerTrigger.TriggerInstance> located(LocationPredicate.Builder p_457210_) {
         return CriteriaTriggers.LOCATION
            .createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().located(p_457210_)))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(EntityPredicate.Builder p_451400_) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(p_451400_.build()))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(Optional<EntityPredicate> p_458188_) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(EntityPredicate.wrap(p_458188_)));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> sleptInBed() {
         return CriteriaTriggers.SLEPT_IN_BED.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> raidWon() {
         return CriteriaTriggers.RAID_WIN.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> avoidVibration() {
         return CriteriaTriggers.AVOID_VIBRATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> tick() {
         return CriteriaTriggers.TICK.createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> walkOnBlockWithEquipment(
         HolderGetter<Block> p_459352_, HolderGetter<Item> p_457579_, Block p_451959_, Item p_460087_
      ) {
         return located(
            EntityPredicate.Builder.entity()
               .equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(p_457579_, p_460087_)))
               .steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(p_459352_, p_451959_)))
         );
      }
   }
}
