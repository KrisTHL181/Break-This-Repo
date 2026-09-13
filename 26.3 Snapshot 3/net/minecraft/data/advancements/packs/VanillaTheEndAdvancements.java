package net.minecraft.data.advancements.packs;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.ChangeDimensionTrigger;
import net.minecraft.advancements.triggers.EnterBlockTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.KilledTrigger;
import net.minecraft.advancements.triggers.LevitationTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.SummonedEntityTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

public class VanillaTheEndAdvancements extends AdvancementSubProvider {
   private final HolderGetter<EntityType<?>> entityTypes;
   private final HolderGetter<Structure> structures;

   public VanillaTheEndAdvancements(final BootstrapContext<Advancement> output) {
      super(output);
      this.entityTypes = output.lookup(Registries.ENTITY_TYPE);
      this.structures = output.lookup(Registries.STRUCTURE);
   }

   @Override
   public void generate() {
      AdvancementHolder root = Advancement.Builder.advancement()
         .display(
            Blocks.END_STONE,
            Component.translatable("advancements.end.root.title"),
            Component.translatable("advancements.end.root.description"),
            Identifier.withDefaultNamespace("gui/advancements/backgrounds/end"),
            AdvancementType.TASK,
            false,
            false,
            false
         )
         .addCriterion("entered_end", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(Level.END))
         .save(this.output, "end/root");
      AdvancementHolder killDragon = Advancement.Builder.advancement()
         .parent(root)
         .display(
            Blocks.DRAGON_HEAD,
            Component.translatable("advancements.end.kill_dragon.title"),
            Component.translatable("advancements.end.kill_dragon.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "killed_dragon", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.ENDER_DRAGON))
         )
         .save(this.output, "end/kill_dragon");
      AdvancementHolder enterEndGateway = Advancement.Builder.advancement()
         .parent(killDragon)
         .display(
            Items.ENDER_PEARL,
            Component.translatable("advancements.end.enter_end_gateway.title"),
            Component.translatable("advancements.end.enter_end_gateway.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion("entered_end_gateway", EnterBlockTrigger.TriggerInstance.entersBlock(Blocks.END_GATEWAY))
         .save(this.output, "end/enter_end_gateway");
      Advancement.Builder.advancement()
         .parent(killDragon)
         .display(
            Items.END_CRYSTAL,
            Component.translatable("advancements.end.respawn_dragon.title"),
            Component.translatable("advancements.end.respawn_dragon.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion(
            "summoned_dragon",
            SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypes.ENDER_DRAGON))
         )
         .save(this.output, "end/respawn_dragon");
      AdvancementHolder findEndCity = Advancement.Builder.advancement()
         .parent(enterEndGateway)
         .display(
            Blocks.PURPUR_BLOCK,
            Component.translatable("advancements.end.find_end_city.title"),
            Component.translatable("advancements.end.find_end_city.description"),
            null,
            AdvancementType.TASK,
            true,
            true,
            false
         )
         .addCriterion(
            "in_city", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(this.structures.getOrThrow(BuiltinStructures.END_CITY)))
         )
         .save(this.output, "end/find_end_city");
      Advancement.Builder.advancement()
         .parent(killDragon)
         .display(
            Items.DRAGON_BREATH,
            Component.translatable("advancements.end.dragon_breath.title"),
            Component.translatable("advancements.end.dragon_breath.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("dragon_breath", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DRAGON_BREATH))
         .save(this.output, "end/dragon_breath");
      Advancement.Builder.advancement()
         .parent(findEndCity)
         .display(
            Items.SHULKER_SHELL,
            Component.translatable("advancements.end.levitate.title"),
            Component.translatable("advancements.end.levitate.description"),
            null,
            AdvancementType.CHALLENGE,
            true,
            true,
            false
         )
         .rewards(AdvancementRewards.Builder.experience(50))
         .addCriterion("levitated", LevitationTrigger.TriggerInstance.levitated(DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(50.0))))
         .save(this.output, "end/levitate");
      Advancement.Builder.advancement()
         .parent(findEndCity)
         .display(
            Items.ELYTRA,
            Component.translatable("advancements.end.elytra.title"),
            Component.translatable("advancements.end.elytra.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("elytra", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ELYTRA))
         .save(this.output, "end/elytra");
      Advancement.Builder.advancement()
         .parent(killDragon)
         .display(
            Blocks.DRAGON_EGG,
            Component.translatable("advancements.end.dragon_egg.title"),
            Component.translatable("advancements.end.dragon_egg.description"),
            null,
            AdvancementType.GOAL,
            true,
            true,
            false
         )
         .addCriterion("dragon_egg", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.DRAGON_EGG))
         .save(this.output, "end/dragon_egg");
   }
}
