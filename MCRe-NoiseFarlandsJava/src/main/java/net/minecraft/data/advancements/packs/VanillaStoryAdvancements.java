package net.minecraft.data.advancements.packs;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.DamagePredicate;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.advancements.triggers.ChangeDimensionTrigger;
import net.minecraft.advancements.triggers.CuredZombieVillagerTrigger;
import net.minecraft.advancements.triggers.EnchantedItemTrigger;
import net.minecraft.advancements.triggers.EntityHurtPlayerTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

public class VanillaStoryAdvancements implements AdvancementSubProvider {
    @Override
    public void generate(final HolderLookup.Provider registries, final Consumer<AdvancementHolder> output) {
        HolderGetter<Item> items = registries.lookupOrThrow(Registries.ITEM);
        AdvancementHolder root = Advancement.Builder.advancement()
            .display(
                Blocks.GRASS_BLOCK,
                Component.translatable("advancements.story.root.title"),
                Component.translatable("advancements.story.root.description"),
                Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"),
                AdvancementType.TASK,
                false,
                false,
                false
            )
            .addCriterion("crafting_table", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.CRAFTING_TABLE))
            .save(output, "story/root");
        AdvancementHolder mineStone = Advancement.Builder.advancement()
            .parent(root)
            .display(
                Items.WOODEN_PICKAXE,
                Component.translatable("advancements.story.mine_stone.title"),
                Component.translatable("advancements.story.mine_stone.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("get_stone", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, ItemTags.STONE_TOOL_MATERIALS)))
            .save(output, "story/mine_stone");
        AdvancementHolder upgradeTools = Advancement.Builder.advancement()
            .parent(mineStone)
            .display(
                Items.STONE_PICKAXE,
                Component.translatable("advancements.story.upgrade_tools.title"),
                Component.translatable("advancements.story.upgrade_tools.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("stone_pickaxe", InventoryChangeTrigger.TriggerInstance.hasItems(Items.STONE_PICKAXE))
            .save(output, "story/upgrade_tools");
        AdvancementHolder smeltIron = Advancement.Builder.advancement()
            .parent(upgradeTools)
            .display(
                Items.IRON_INGOT,
                Component.translatable("advancements.story.smelt_iron.title"),
                Component.translatable("advancements.story.smelt_iron.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("iron", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
            .save(output, "story/smelt_iron");
        AdvancementHolder ironTools = Advancement.Builder.advancement()
            .parent(smeltIron)
            .display(
                Items.IRON_PICKAXE,
                Component.translatable("advancements.story.iron_tools.title"),
                Component.translatable("advancements.story.iron_tools.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("iron_pickaxe", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_PICKAXE))
            .save(output, "story/iron_tools");
        AdvancementHolder mineDiamond = Advancement.Builder.advancement()
            .parent(ironTools)
            .display(
                Items.DIAMOND,
                Component.translatable("advancements.story.mine_diamond.title"),
                Component.translatable("advancements.story.mine_diamond.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("diamond", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND))
            .save(output, "story/mine_diamond");
        AdvancementHolder lavaBucket = Advancement.Builder.advancement()
            .parent(smeltIron)
            .display(
                Items.LAVA_BUCKET,
                Component.translatable("advancements.story.lava_bucket.title"),
                Component.translatable("advancements.story.lava_bucket.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("lava_bucket", InventoryChangeTrigger.TriggerInstance.hasItems(Items.LAVA_BUCKET))
            .save(output, "story/lava_bucket");
        AdvancementHolder obtainArmor = Advancement.Builder.advancement()
            .parent(smeltIron)
            .display(
                Items.IRON_CHESTPLATE,
                Component.translatable("advancements.story.obtain_armor.title"),
                Component.translatable("advancements.story.obtain_armor.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .requirements(AdvancementRequirements.Strategy.OR)
            .addCriterion("iron_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_HELMET))
            .addCriterion("iron_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_CHESTPLATE))
            .addCriterion("iron_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_LEGGINGS))
            .addCriterion("iron_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_BOOTS))
            .save(output, "story/obtain_armor");
        Advancement.Builder.advancement()
            .parent(mineDiamond)
            .display(
                Items.ENCHANTED_BOOK,
                Component.translatable("advancements.story.enchant_item.title"),
                Component.translatable("advancements.story.enchant_item.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("enchanted_item", EnchantedItemTrigger.TriggerInstance.enchantedItem())
            .save(output, "story/enchant_item");
        AdvancementHolder formObsidian = Advancement.Builder.advancement()
            .parent(lavaBucket)
            .display(
                Blocks.OBSIDIAN,
                Component.translatable("advancements.story.form_obsidian.title"),
                Component.translatable("advancements.story.form_obsidian.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("obsidian", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.OBSIDIAN))
            .save(output, "story/form_obsidian");
        Advancement.Builder.advancement()
            .parent(obtainArmor)
            .display(
                Items.SHIELD,
                Component.translatable("advancements.story.deflect_arrow.title"),
                Component.translatable("advancements.story.deflect_arrow.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion(
                "deflected_projectile",
                EntityHurtPlayerTrigger.TriggerInstance.entityHurtPlayer(
                    DamagePredicate.Builder.damageInstance()
                        .type(DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE)))
                        .blocked(true)
                )
            )
            .save(output, "story/deflect_arrow");
        Advancement.Builder.advancement()
            .parent(mineDiamond)
            .display(
                Items.DIAMOND_CHESTPLATE,
                Component.translatable("advancements.story.shiny_gear.title"),
                Component.translatable("advancements.story.shiny_gear.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .requirements(AdvancementRequirements.Strategy.OR)
            .addCriterion("diamond_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_HELMET))
            .addCriterion("diamond_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_CHESTPLATE))
            .addCriterion("diamond_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_LEGGINGS))
            .addCriterion("diamond_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_BOOTS))
            .save(output, "story/shiny_gear");
        AdvancementHolder enterTheNether = Advancement.Builder.advancement()
            .parent(formObsidian)
            .display(
                Items.FLINT_AND_STEEL,
                Component.translatable("advancements.story.enter_the_nether.title"),
                Component.translatable("advancements.story.enter_the_nether.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("entered_nether", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(Level.NETHER))
            .save(output, "story/enter_the_nether");
        Advancement.Builder.advancement()
            .parent(enterTheNether)
            .display(
                Items.GOLDEN_APPLE,
                Component.translatable("advancements.story.cure_zombie_villager.title"),
                Component.translatable("advancements.story.cure_zombie_villager.description"),
                null,
                AdvancementType.GOAL,
                true,
                true,
                false
            )
            .addCriterion("cured_zombie", CuredZombieVillagerTrigger.TriggerInstance.curedZombieVillager())
            .save(output, "story/cure_zombie_villager");
        AdvancementHolder followEnderEye = Advancement.Builder.advancement()
            .parent(enterTheNether)
            .display(
                Items.ENDER_EYE,
                Component.translatable("advancements.story.follow_ender_eye.title"),
                Component.translatable("advancements.story.follow_ender_eye.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion(
                "in_stronghold",
                PlayerTrigger.TriggerInstance.located(
                    LocationPredicate.Builder.inStructure(registries.lookupOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.STRONGHOLD))
                )
            )
            .save(output, "story/follow_ender_eye");
        Advancement.Builder.advancement()
            .parent(followEnderEye)
            .display(
                Blocks.END_STONE,
                Component.translatable("advancements.story.enter_the_end.title"),
                Component.translatable("advancements.story.enter_the_end.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .addCriterion("entered_end", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(Level.END))
            .save(output, "story/enter_the_end");
    }
}