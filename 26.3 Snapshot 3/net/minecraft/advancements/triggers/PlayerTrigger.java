package net.minecraft.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class PlayerTrigger extends SimpleCriterionTrigger<PlayerTrigger.TriggerInstance> {
   @Override
   public Codec<PlayerTrigger.TriggerInstance> codec() {
      return PlayerTrigger.TriggerInstance.CODEC;
   }

   public void trigger(final ServerPlayer player) {
      this.trigger(player, t -> true);
   }

   public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<PlayerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         i -> i.group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(PlayerTrigger.TriggerInstance::player))
            .apply(i, PlayerTrigger.TriggerInstance::new)
      );

      public static Criterion<PlayerTrigger.TriggerInstance> located(final LocationPredicate.Builder location) {
         return CriteriaTriggers.LOCATION
            .createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(EntityPredicate.Builder.entity().located(location)))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(final EntityPredicate.Builder player) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(player.build()))));
      }

      public static Criterion<PlayerTrigger.TriggerInstance> located(final Optional<EntityPredicate> player) {
         return CriteriaTriggers.LOCATION.createCriterion(new PlayerTrigger.TriggerInstance(EntityPredicate.wrap(player)));
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
         final HolderGetter<Block> blocks, final HolderGetter<Item> items, final Block stepOnBlock, final Item requiredEquipment
      ) {
         return located(
            EntityPredicate.Builder.entity()
               .equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(items, requiredEquipment)))
               .steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(blocks, stepOnBlock)))
         );
      }
   }
}
