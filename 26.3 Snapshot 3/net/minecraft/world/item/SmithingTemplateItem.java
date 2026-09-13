package net.minecraft.world.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.TooltipDisplay;

public class SmithingTemplateItem extends Item {
   private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
   private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
   private static final Component INGREDIENTS_TITLE = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.ingredients"))
      )
      .withStyle(TITLE_FORMAT);
   private static final Component APPLIES_TO_TITLE = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.applies_to"))
      )
      .withStyle(TITLE_FORMAT);
   private static final Component SMITHING_TEMPLATE_SUFFIX = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template"))
      )
      .withStyle(TITLE_FORMAT);
   private static final Component ARMOR_TRIM_APPLIES_TO = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.armor_trim.applies_to"))
      )
      .withStyle(DESCRIPTION_FORMAT);
   private static final Component ARMOR_TRIM_INGREDIENTS = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.armor_trim.ingredients"))
      )
      .withStyle(DESCRIPTION_FORMAT);
   private static final Component ARMOR_TRIM_BASE_SLOT_DESCRIPTION = Component.translatable(
      Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.armor_trim.base_slot_description"))
   );
   private static final Component ARMOR_TRIM_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
      Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.armor_trim.additions_slot_description"))
   );
   private static final Component NETHERITE_UPGRADE_APPLIES_TO = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.netherite_upgrade.applies_to"))
      )
      .withStyle(DESCRIPTION_FORMAT);
   private static final Component NETHERITE_UPGRADE_INGREDIENTS = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.netherite_upgrade.ingredients"))
      )
      .withStyle(DESCRIPTION_FORMAT);
   private static final Component NETHERITE_UPGRADE_BASE_SLOT_DESCRIPTION = Component.translatable(
      Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.netherite_upgrade.base_slot_description"))
   );
   private static final Component NETHERITE_UPGRADE_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
      Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.netherite_upgrade.additions_slot_description"))
   );
   private static final Identifier EMPTY_SLOT_HELMET = Identifier.withDefaultNamespace("container/slot/helmet");
   private static final Identifier EMPTY_SLOT_CHESTPLATE = Identifier.withDefaultNamespace("container/slot/chestplate");
   private static final Identifier EMPTY_SLOT_LEGGINGS = Identifier.withDefaultNamespace("container/slot/leggings");
   private static final Identifier EMPTY_SLOT_BOOTS = Identifier.withDefaultNamespace("container/slot/boots");
   private static final Identifier EMPTY_SLOT_HOE = Identifier.withDefaultNamespace("container/slot/hoe");
   private static final Identifier EMPTY_SLOT_AXE = Identifier.withDefaultNamespace("container/slot/axe");
   private static final Identifier EMPTY_SLOT_SWORD = Identifier.withDefaultNamespace("container/slot/sword");
   private static final Identifier EMPTY_SLOT_SHOVEL = Identifier.withDefaultNamespace("container/slot/shovel");
   private static final Identifier EMPTY_SLOT_SPEAR = Identifier.withDefaultNamespace("container/slot/spear");
   private static final Identifier EMPTY_SLOT_PICKAXE = Identifier.withDefaultNamespace("container/slot/pickaxe");
   private static final Identifier EMPTY_SLOT_INGOT = Identifier.withDefaultNamespace("container/slot/ingot");
   private static final Identifier EMPTY_SLOT_REDSTONE_DUST = Identifier.withDefaultNamespace("container/slot/redstone_dust");
   private static final Identifier EMPTY_SLOT_QUARTZ = Identifier.withDefaultNamespace("container/slot/quartz");
   private static final Identifier EMPTY_SLOT_EMERALD = Identifier.withDefaultNamespace("container/slot/emerald");
   private static final Identifier EMPTY_SLOT_DIAMOND = Identifier.withDefaultNamespace("container/slot/diamond");
   private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
   private static final Identifier EMPTY_SLOT_AMETHYST_SHARD = Identifier.withDefaultNamespace("container/slot/amethyst_shard");
   private static final Identifier EMPTY_SLOT_NAUTILUS_ARMOR = Identifier.withDefaultNamespace("container/slot/nautilus_armor");
   private final Component appliesTo;
   private final Component ingredients;
   private final Component baseSlotDescription;
   private final Component additionsSlotDescription;
   private final List<Identifier> baseSlotEmptyIcons;
   private final List<Identifier> additionalSlotEmptyIcons;

   public SmithingTemplateItem(
      final Component appliesTo,
      final Component ingredients,
      final Component baseSlotDescription,
      final Component additionsSlotDescription,
      final List<Identifier> baseSlotEmptyIcons,
      final List<Identifier> additionalSlotEmptyIcons,
      final Item.Properties properties
   ) {
      super(properties);
      this.appliesTo = appliesTo;
      this.ingredients = ingredients;
      this.baseSlotDescription = baseSlotDescription;
      this.additionsSlotDescription = additionsSlotDescription;
      this.baseSlotEmptyIcons = baseSlotEmptyIcons;
      this.additionalSlotEmptyIcons = additionalSlotEmptyIcons;
   }

   public static SmithingTemplateItem createArmorTrimTemplate(final Item.Properties properties) {
      return new SmithingTemplateItem(
         ARMOR_TRIM_APPLIES_TO,
         ARMOR_TRIM_INGREDIENTS,
         ARMOR_TRIM_BASE_SLOT_DESCRIPTION,
         ARMOR_TRIM_ADDITIONS_SLOT_DESCRIPTION,
         createTrimmableArmorIconList(),
         createTrimmableMaterialIconList(),
         properties
      );
   }

   public static SmithingTemplateItem createNetheriteUpgradeTemplate(final Item.Properties properties) {
      return new SmithingTemplateItem(
         NETHERITE_UPGRADE_APPLIES_TO,
         NETHERITE_UPGRADE_INGREDIENTS,
         NETHERITE_UPGRADE_BASE_SLOT_DESCRIPTION,
         NETHERITE_UPGRADE_ADDITIONS_SLOT_DESCRIPTION,
         createNetheriteUpgradeIconList(),
         createNetheriteUpgradeMaterialList(),
         properties
      );
   }

   private static List<Identifier> createTrimmableArmorIconList() {
      return List.of(EMPTY_SLOT_HELMET, EMPTY_SLOT_CHESTPLATE, EMPTY_SLOT_LEGGINGS, EMPTY_SLOT_BOOTS);
   }

   private static List<Identifier> createTrimmableMaterialIconList() {
      return List.of(
         EMPTY_SLOT_INGOT,
         EMPTY_SLOT_REDSTONE_DUST,
         EMPTY_SLOT_LAPIS_LAZULI,
         EMPTY_SLOT_QUARTZ,
         EMPTY_SLOT_DIAMOND,
         EMPTY_SLOT_EMERALD,
         EMPTY_SLOT_AMETHYST_SHARD
      );
   }

   private static List<Identifier> createNetheriteUpgradeIconList() {
      return List.of(
         EMPTY_SLOT_HELMET,
         EMPTY_SLOT_SWORD,
         EMPTY_SLOT_CHESTPLATE,
         EMPTY_SLOT_PICKAXE,
         EMPTY_SLOT_LEGGINGS,
         EMPTY_SLOT_AXE,
         EMPTY_SLOT_BOOTS,
         EMPTY_SLOT_HOE,
         EMPTY_SLOT_SHOVEL,
         EMPTY_SLOT_NAUTILUS_ARMOR,
         EMPTY_SLOT_SPEAR
      );
   }

   private static List<Identifier> createNetheriteUpgradeMaterialList() {
      return List.of(EMPTY_SLOT_INGOT);
   }

   @Override
   public void appendHoverText(
      final ItemStack itemStack,
      final Item.TooltipContext context,
      final TooltipDisplay display,
      final Consumer<Component> builder,
      final TooltipFlag tooltipFlag
   ) {
      builder.accept(SMITHING_TEMPLATE_SUFFIX);
      builder.accept(CommonComponents.EMPTY);
      builder.accept(APPLIES_TO_TITLE);
      builder.accept(CommonComponents.space().append(this.appliesTo));
      builder.accept(INGREDIENTS_TITLE);
      builder.accept(CommonComponents.space().append(this.ingredients));
   }

   public Component getBaseSlotDescription() {
      return this.baseSlotDescription;
   }

   public Component getAdditionSlotDescription() {
      return this.additionsSlotDescription;
   }

   public List<Identifier> getBaseSlotEmptyIcons() {
      return this.baseSlotEmptyIcons;
   }

   public List<Identifier> getAdditionalSlotEmptyIcons() {
      return this.additionalSlotEmptyIcons;
   }
}
