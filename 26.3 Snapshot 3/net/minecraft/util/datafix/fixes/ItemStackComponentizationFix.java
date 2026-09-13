package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import org.jspecify.annotations.Nullable;

public class ItemStackComponentizationFix extends DataFix {
   private static final int HIDE_ENCHANTMENTS = 1;
   private static final int HIDE_MODIFIERS = 2;
   private static final int HIDE_UNBREAKABLE = 4;
   private static final int HIDE_CAN_DESTROY = 8;
   private static final int HIDE_CAN_PLACE = 16;
   private static final int HIDE_ADDITIONAL = 32;
   private static final int HIDE_DYE = 64;
   private static final int HIDE_UPGRADES = 128;
   private static final Set<String> POTION_HOLDER_IDS = Set.of(
      "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
   );
   private static final Set<String> BUCKETED_MOB_IDS = Set.of(
      "minecraft:pufferfish_bucket",
      "minecraft:salmon_bucket",
      "minecraft:cod_bucket",
      "minecraft:tropical_fish_bucket",
      "minecraft:axolotl_bucket",
      "minecraft:tadpole_bucket"
   );
   private static final List<String> BUCKETED_MOB_TAGS = List.of(
      "NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
   );
   private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
      "attached",
      "bottom",
      "conditional",
      "disarmed",
      "drag",
      "enabled",
      "extended",
      "eye",
      "falling",
      "hanging",
      "has_bottle_0",
      "has_bottle_1",
      "has_bottle_2",
      "has_record",
      "has_book",
      "inverted",
      "in_wall",
      "lit",
      "locked",
      "occupied",
      "open",
      "persistent",
      "powered",
      "short",
      "signal_fire",
      "snowy",
      "triggered",
      "unstable",
      "waterlogged",
      "berries",
      "bloom",
      "shrieking",
      "can_summon",
      "up",
      "down",
      "north",
      "east",
      "south",
      "west",
      "slot_0_occupied",
      "slot_1_occupied",
      "slot_2_occupied",
      "slot_3_occupied",
      "slot_4_occupied",
      "slot_5_occupied",
      "cracked",
      "crafting"
   );
   private static final Splitter PROPERTY_SPLITTER = Splitter.on(',');

   public ItemStackComponentizationFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   private static void fixItemStack(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      int hideFlags = itemStack.removeTag("HideFlags").asInt(0);
      itemStack.moveTagToComponent("Damage", "minecraft:damage", dynamic.createInt(0));
      itemStack.moveTagToComponent("RepairCost", "minecraft:repair_cost", dynamic.createInt(0));
      itemStack.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
      itemStack.removeTag("BlockStateTag")
         .result()
         .ifPresent(blockStateTag -> itemStack.setComponent("minecraft:block_state", fixBlockStateTag((Dynamic<?>)blockStateTag)));
      itemStack.moveTagToComponent("EntityTag", "minecraft:entity_data");
      itemStack.fixSubTag("BlockEntityTag", false, blockEntityTag -> {
         String id = NamespacedSchema.ensureNamespaced(blockEntityTag.get("id").asString(""));
         blockEntityTag = fixBlockEntityTag(itemStack, blockEntityTag, id);
         Dynamic<?> withoutId = blockEntityTag.remove("id");
         return withoutId.equals(blockEntityTag.emptyMap()) ? withoutId : blockEntityTag;
      });
      itemStack.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
      if (itemStack.removeTag("Unbreakable").asBoolean(false)) {
         Dynamic<?> component = dynamic.emptyMap();
         if ((hideFlags & 4) != 0) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
         }

         itemStack.setComponent("minecraft:unbreakable", component);
      }

      fixEnchantments(itemStack, dynamic, "Enchantments", "minecraft:enchantments", (hideFlags & 1) != 0);
      if (itemStack.is("minecraft:enchanted_book")) {
         fixEnchantments(itemStack, dynamic, "StoredEnchantments", "minecraft:stored_enchantments", (hideFlags & 32) != 0);
      }

      itemStack.fixSubTag("display", false, display -> fixDisplay(itemStack, display, hideFlags));
      fixAdventureModeChecks(itemStack, dynamic, hideFlags);
      fixAttributeModifiers(itemStack, dynamic, hideFlags);
      Optional<? extends Dynamic<?>> trim = itemStack.removeTag("Trim").result();
      if (trim.isPresent()) {
         Dynamic<?> fixedTrim = (Dynamic<?>)trim.get();
         if ((hideFlags & 128) != 0) {
            fixedTrim = fixedTrim.set("show_in_tooltip", fixedTrim.createBoolean(false));
         }

         itemStack.setComponent("minecraft:trim", fixedTrim);
      }

      if ((hideFlags & 32) != 0) {
         itemStack.setComponent("minecraft:hide_additional_tooltip", dynamic.emptyMap());
      }

      if (itemStack.is("minecraft:crossbow")) {
         itemStack.removeTag("Charged");
         itemStack.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", dynamic.createList(Stream.empty()));
      }

      if (itemStack.is("minecraft:bundle")) {
         itemStack.moveTagToComponent("Items", "minecraft:bundle_contents", dynamic.createList(Stream.empty()));
      }

      if (itemStack.is("minecraft:filled_map")) {
         itemStack.moveTagToComponent("map", "minecraft:map_id");
         Map<? extends Dynamic<?>, ? extends Dynamic<?>> decorations = itemStack.removeTag("Decorations")
            .asStream()
            .map(ItemStackComponentizationFix::fixMapDecoration)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (first, second) -> first));
         if (!decorations.isEmpty()) {
            itemStack.setComponent("minecraft:map_decorations", dynamic.createMap(decorations));
         }
      }

      if (itemStack.is(POTION_HOLDER_IDS)) {
         fixPotionContents(itemStack, dynamic);
      }

      if (itemStack.is("minecraft:writable_book")) {
         fixWritableBook(itemStack, dynamic);
      }

      if (itemStack.is("minecraft:written_book")) {
         fixWrittenBook(itemStack, dynamic);
      }

      if (itemStack.is("minecraft:suspicious_stew")) {
         itemStack.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
      }

      if (itemStack.is("minecraft:debug_stick")) {
         itemStack.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
      }

      if (itemStack.is(BUCKETED_MOB_IDS)) {
         fixBucketedMobData(itemStack, dynamic);
      }

      if (itemStack.is("minecraft:goat_horn")) {
         itemStack.moveTagToComponent("instrument", "minecraft:instrument");
      }

      if (itemStack.is("minecraft:knowledge_book")) {
         itemStack.moveTagToComponent("Recipes", "minecraft:recipes");
      }

      if (itemStack.is("minecraft:compass")) {
         fixLodestoneTracker(itemStack, dynamic);
      }

      if (itemStack.is("minecraft:firework_rocket")) {
         fixFireworkRocket(itemStack);
      }

      if (itemStack.is("minecraft:firework_star")) {
         fixFireworkStar(itemStack);
      }

      if (itemStack.is("minecraft:player_head")) {
         itemStack.removeTag("SkullOwner").result().ifPresent(skullOwner -> itemStack.setComponent("minecraft:profile", fixProfile((Dynamic<?>)skullOwner)));
      }
   }

   private static Dynamic<?> fixBlockStateTag(final Dynamic<?> blockStateTag) {
      return (Dynamic<?>)DataFixUtils.orElse(blockStateTag.asMapOpt().result().map(entries -> entries.collect(Collectors.toMap(Pair::getFirst, entry -> {
         String key = ((Dynamic)entry.getFirst()).asString("");
         Dynamic<?> value = (Dynamic<?>)entry.getSecond();
         if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(key)) {
            Optional<Boolean> bool = value.asBoolean().result();
            if (bool.isPresent()) {
               return value.createString(String.valueOf(bool.get()));
            }
         }

         Optional<Number> number = value.asNumber().result();
         return number.isPresent() ? value.createString(number.get().toString()) : value;
      }))).map(blockStateTag::createMap), blockStateTag);
   }

   private static Dynamic<?> fixDisplay(final ItemStackComponentizationFix.ItemStackData itemStack, Dynamic<?> display, final int hideFlags) {
      display.get("Name")
         .result()
         .filter(LegacyComponentDataFixUtils::isStrictlyValidJson)
         .ifPresent(name -> itemStack.setComponent("minecraft:custom_name", (Dynamic<?>)name));
      OptionalDynamic<?> lore = display.get("Lore");
      if (lore.result().isPresent()) {
         itemStack.setComponent("minecraft:lore", display.createList(display.get("Lore").asStream().filter(LegacyComponentDataFixUtils::isStrictlyValidJson)));
      }

      Optional<Integer> color = display.get("color").asNumber().result().map(Number::intValue);
      boolean hideDye = (hideFlags & 64) != 0;
      if (color.isPresent() || hideDye) {
         Dynamic<?> dyedColor = display.emptyMap().set("rgb", display.createInt(color.orElse(10511680)));
         if (hideDye) {
            dyedColor = dyedColor.set("show_in_tooltip", display.createBoolean(false));
         }

         itemStack.setComponent("minecraft:dyed_color", dyedColor);
      }

      Optional<String> locName = display.get("LocName").asString().result();
      if (locName.isPresent()) {
         itemStack.setComponent("minecraft:item_name", LegacyComponentDataFixUtils.createTranslatableComponent(display.getOps(), locName.get()));
      }

      if (itemStack.is("minecraft:filled_map")) {
         itemStack.setComponent("minecraft:map_color", display.get("MapColor"));
         display = display.remove("MapColor");
      }

      return display.remove("Name").remove("Lore").remove("color").remove("LocName");
   }

   private static <T> Dynamic<T> fixBlockEntityTag(final ItemStackComponentizationFix.ItemStackData itemStack, Dynamic<T> blockEntity, final String id) {
      itemStack.setComponent("minecraft:lock", blockEntity.get("Lock"));
      blockEntity = blockEntity.remove("Lock");
      Optional<Dynamic<T>> lootTable = blockEntity.get("LootTable").result();
      if (lootTable.isPresent()) {
         Dynamic<T> containerLoot = blockEntity.emptyMap().set("loot_table", lootTable.get());
         long seed = blockEntity.get("LootTableSeed").asLong(0L);
         if (seed != 0L) {
            containerLoot = containerLoot.set("seed", blockEntity.createLong(seed));
         }

         itemStack.setComponent("minecraft:container_loot", containerLoot);
         blockEntity = blockEntity.remove("LootTable").remove("LootTableSeed");
      }
      return switch (id) {
         case "minecraft:skull" -> {
            itemStack.setComponent("minecraft:note_block_sound", blockEntity.get("note_block_sound"));
            yield blockEntity.remove("note_block_sound");
         }
         case "minecraft:decorated_pot" -> {
            itemStack.setComponent("minecraft:pot_decorations", blockEntity.get("sherds"));
            Optional<Dynamic<T>> item = blockEntity.get("item").result();
            if (item.isPresent()) {
               itemStack.setComponent(
                  "minecraft:container",
                  blockEntity.createList(Stream.of(blockEntity.emptyMap().set("slot", blockEntity.createInt(0)).set("item", item.get())))
               );
            }

            yield blockEntity.remove("sherds").remove("item");
         }
         case "minecraft:banner" -> {
            itemStack.setComponent("minecraft:banner_patterns", blockEntity.get("patterns"));
            Optional<Number> base = blockEntity.get("Base").asNumber().result();
            if (base.isPresent()) {
               itemStack.setComponent("minecraft:base_color", blockEntity.createString(ExtraDataFixUtils.dyeColorIdToName(base.get().intValue())));
            }

            yield blockEntity.remove("patterns").remove("Base");
         }
         case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
            List<Dynamic<T>> items = blockEntity.get("Items")
               .asList(
                  dynamic -> dynamic.emptyMap().set("slot", dynamic.createInt(dynamic.get("Slot").asByte((byte)0) & 255)).set("item", dynamic.remove("Slot"))
               );
            if (!items.isEmpty()) {
               itemStack.setComponent("minecraft:container", blockEntity.createList(items.stream()));
            }

            yield blockEntity.remove("Items");
         }
         case "minecraft:beehive" -> {
            itemStack.setComponent("minecraft:bees", blockEntity.get("bees"));
            yield blockEntity.remove("bees");
         }
         default -> blockEntity;
      };
   }

   private static void fixEnchantments(
      final ItemStackComponentizationFix.ItemStackData itemStack,
      final Dynamic<?> dynamic,
      final String key,
      final String componentType,
      final boolean hideInTooltip
   ) {
      OptionalDynamic<?> rawEnchantments = itemStack.removeTag(key);
      List<Pair<String, Integer>> enchantments = rawEnchantments.asList(Function.identity())
         .stream()
         .flatMap(enchantmentx -> parseEnchantment(enchantmentx).stream())
         .filter(enchantmentx -> (Integer)enchantmentx.getSecond() > 0)
         .toList();
      if (!enchantments.isEmpty() || hideInTooltip) {
         Dynamic<?> component = dynamic.emptyMap();
         Dynamic<?> levels = dynamic.emptyMap();

         for (Pair<String, Integer> enchantment : enchantments) {
            levels = levels.set((String)enchantment.getFirst(), dynamic.createInt((Integer)enchantment.getSecond()));
         }

         component = component.set("levels", levels);
         if (hideInTooltip) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
         }

         itemStack.setComponent(componentType, component);
      }

      if (rawEnchantments.result().isPresent() && enchantments.isEmpty()) {
         itemStack.setComponent("minecraft:enchantment_glint_override", dynamic.createBoolean(true));
      }
   }

   private static Optional<Pair<String, Integer>> parseEnchantment(final Dynamic<?> entry) {
      return entry.get("id").asString().apply2stable((id, level) -> Pair.of(id, Mth.clamp(level.intValue(), 0, 255)), entry.get("lvl").asNumber()).result();
   }

   private static void fixAdventureModeChecks(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic, final int hideFlags) {
      fixBlockStatePredicates(itemStack, dynamic, "CanDestroy", "minecraft:can_break", (hideFlags & 8) != 0);
      fixBlockStatePredicates(itemStack, dynamic, "CanPlaceOn", "minecraft:can_place_on", (hideFlags & 16) != 0);
   }

   private static void fixBlockStatePredicates(
      final ItemStackComponentizationFix.ItemStackData itemStack,
      final Dynamic<?> dynamic,
      final String tag,
      final String componentId,
      final boolean hideInTooltip
   ) {
      Optional<? extends Dynamic<?>> oldPredicate = itemStack.removeTag(tag).result();
      if (!oldPredicate.isEmpty()) {
         Dynamic<?> component = dynamic.emptyMap()
            .set(
               "predicates",
               dynamic.createList(
                  oldPredicate.get()
                     .asStream()
                     .map(
                        value -> (Dynamic)DataFixUtils.orElse(value.asString().map(string -> fixBlockStatePredicate((Dynamic<?>)value, string)).result(), value)
                     )
               )
            );
         if (hideInTooltip) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
         }

         itemStack.setComponent(componentId, component);
      }
   }

   private static Dynamic<?> fixBlockStatePredicate(final Dynamic<?> dynamic, final String string) {
      int startProperties = string.indexOf(91);
      int startNbt = string.indexOf(123);
      int blockNameEnd = string.length();
      if (startProperties != -1) {
         blockNameEnd = startProperties;
      }

      if (startNbt != -1) {
         blockNameEnd = Math.min(blockNameEnd, startNbt);
      }

      String blockOrTagName = string.substring(0, blockNameEnd);
      Dynamic<?> predicate = dynamic.emptyMap().set("blocks", dynamic.createString(blockOrTagName.trim()));
      int endProperties = string.indexOf(93);
      if (startProperties != -1 && endProperties != -1) {
         Dynamic<?> properties = dynamic.emptyMap();

         for (String property : PROPERTY_SPLITTER.split(string.substring(startProperties + 1, endProperties))) {
            int assignment = property.indexOf(61);
            if (assignment != -1) {
               String key = property.substring(0, assignment).trim();
               String value = property.substring(assignment + 1).trim();
               properties = properties.set(key, dynamic.createString(value));
            }
         }

         predicate = predicate.set("state", properties);
      }

      int endNbt = string.indexOf(125);
      if (startNbt != -1 && endNbt != -1) {
         predicate = predicate.set("nbt", dynamic.createString(string.substring(startNbt, endNbt + 1)));
      }

      return predicate;
   }

   private static void fixAttributeModifiers(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic, final int hideFlags) {
      OptionalDynamic<?> attributeModifiersField = itemStack.removeTag("AttributeModifiers");
      if (!attributeModifiersField.result().isEmpty()) {
         boolean hideInTooltip = (hideFlags & 2) != 0;
         List<? extends Dynamic<?>> attributeModifiers = attributeModifiersField.asList(ItemStackComponentizationFix::fixAttributeModifier);
         Dynamic<?> component = dynamic.emptyMap().set("modifiers", dynamic.createList(attributeModifiers.stream()));
         if (hideInTooltip) {
            component = component.set("show_in_tooltip", dynamic.createBoolean(false));
         }

         itemStack.setComponent("minecraft:attribute_modifiers", component);
      }
   }

   private static Dynamic<?> fixAttributeModifier(final Dynamic<?> input) {
      Dynamic<?> result = input.emptyMap()
         .set("name", input.createString(""))
         .set("amount", input.createDouble(0.0))
         .set("operation", input.createString("add_value"));
      result = Dynamic.copyField(input, "AttributeName", result, "type");
      result = Dynamic.copyField(input, "Slot", result, "slot");
      result = Dynamic.copyField(input, "UUID", result, "uuid");
      result = Dynamic.copyField(input, "Name", result, "name");
      result = Dynamic.copyField(input, "Amount", result, "amount");
      return Dynamic.copyAndFixField(input, "Operation", result, "operation", operation -> {
         return operation.createString(switch (operation.asInt(0)) {
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
            default -> "add_value";
         });
      });
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(final Dynamic<?> decoration) {
      Dynamic<?> id = (Dynamic<?>)DataFixUtils.orElseGet(decoration.get("id").result(), () -> decoration.createString(""));
      Dynamic<?> value = decoration.emptyMap()
         .set("type", decoration.createString(fixMapDecorationType(decoration.get("type").asInt(0))))
         .set("x", decoration.createDouble(decoration.get("x").asDouble(0.0)))
         .set("z", decoration.createDouble(decoration.get("z").asDouble(0.0)))
         .set("rotation", decoration.createFloat((float)decoration.get("rot").asDouble(0.0)));
      return Pair.of(id, value);
   }

   private static String fixMapDecorationType(final int id) {
      return switch (id) {
         case 1 -> "frame";
         case 2 -> "red_marker";
         case 3 -> "blue_marker";
         case 4 -> "target_x";
         case 5 -> "target_point";
         case 6 -> "player_off_map";
         case 7 -> "player_off_limits";
         case 8 -> "mansion";
         case 9 -> "monument";
         case 10 -> "banner_white";
         case 11 -> "banner_orange";
         case 12 -> "banner_magenta";
         case 13 -> "banner_light_blue";
         case 14 -> "banner_yellow";
         case 15 -> "banner_lime";
         case 16 -> "banner_pink";
         case 17 -> "banner_gray";
         case 18 -> "banner_light_gray";
         case 19 -> "banner_cyan";
         case 20 -> "banner_purple";
         case 21 -> "banner_blue";
         case 22 -> "banner_brown";
         case 23 -> "banner_green";
         case 24 -> "banner_red";
         case 25 -> "banner_black";
         case 26 -> "red_x";
         case 27 -> "village_desert";
         case 28 -> "village_plains";
         case 29 -> "village_savanna";
         case 30 -> "village_snowy";
         case 31 -> "village_taiga";
         case 32 -> "jungle_temple";
         case 33 -> "swamp_hut";
         default -> "player";
      };
   }

   private static void fixPotionContents(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      Dynamic<?> component = dynamic.emptyMap();
      Optional<String> potion = itemStack.removeTag("Potion").asString().result().filter(id -> !id.equals("minecraft:empty"));
      if (potion.isPresent()) {
         component = component.set("potion", dynamic.createString(potion.get()));
      }

      component = itemStack.moveTagInto("CustomPotionColor", component, "custom_color");
      component = itemStack.moveTagInto("custom_potion_effects", component, "custom_effects");
      if (!component.equals(dynamic.emptyMap())) {
         itemStack.setComponent("minecraft:potion_contents", component);
      }
   }

   private static void fixWritableBook(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      Dynamic<?> pages = fixBookPages(itemStack, dynamic);
      if (pages != null) {
         itemStack.setComponent("minecraft:writable_book_content", dynamic.emptyMap().set("pages", pages));
      }
   }

   private static void fixWrittenBook(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      Dynamic<?> pages = fixBookPages(itemStack, dynamic);
      String title = itemStack.removeTag("title").asString("");
      Optional<String> filteredTitle = itemStack.removeTag("filtered_title").asString().result();
      Dynamic<?> component = dynamic.emptyMap();
      component = component.set("title", createFilteredText(dynamic, title, filteredTitle));
      component = itemStack.moveTagInto("author", component, "author");
      component = itemStack.moveTagInto("resolved", component, "resolved");
      component = itemStack.moveTagInto("generation", component, "generation");
      if (pages != null) {
         component = component.set("pages", pages);
      }

      itemStack.setComponent("minecraft:written_book_content", component);
   }

   private static @Nullable Dynamic<?> fixBookPages(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      List<String> pages = itemStack.removeTag("pages").asList(pagex -> pagex.asString(""));
      Map<String, String> filteredPages = itemStack.removeTag("filtered_pages").asMap(key -> key.asString("0"), pagex -> pagex.asString(""));
      if (pages.isEmpty()) {
         return null;
      }

      List<Dynamic<?>> fixedPages = new ArrayList<>(pages.size());

      for (int i = 0; i < pages.size(); i++) {
         String page = pages.get(i);
         String filteredPage = filteredPages.get(String.valueOf(i));
         fixedPages.add(createFilteredText(dynamic, page, Optional.ofNullable(filteredPage)));
      }

      return dynamic.createList(fixedPages.stream());
   }

   private static Dynamic<?> createFilteredText(final Dynamic<?> dynamic, final String text, final Optional<String> filtered) {
      Dynamic<?> fixedPage = dynamic.emptyMap().set("raw", dynamic.createString(text));
      if (filtered.isPresent()) {
         fixedPage = fixedPage.set("filtered", dynamic.createString(filtered.get()));
      }

      return fixedPage;
   }

   private static void fixBucketedMobData(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      Dynamic<?> data = dynamic.emptyMap();

      for (String key : BUCKETED_MOB_TAGS) {
         data = itemStack.moveTagInto(key, data, key);
      }

      if (!data.equals(dynamic.emptyMap())) {
         itemStack.setComponent("minecraft:bucket_entity_data", data);
      }
   }

   private static void fixLodestoneTracker(final ItemStackComponentizationFix.ItemStackData itemStack, final Dynamic<?> dynamic) {
      Optional<? extends Dynamic<?>> lodestonePos = itemStack.removeTag("LodestonePos").result();
      Optional<? extends Dynamic<?>> lodestoneDimension = itemStack.removeTag("LodestoneDimension").result();
      if (!lodestonePos.isEmpty() || !lodestoneDimension.isEmpty()) {
         boolean lodestoneTracked = itemStack.removeTag("LodestoneTracked").asBoolean(true);
         Dynamic<?> component = dynamic.emptyMap();
         if (lodestonePos.isPresent() && lodestoneDimension.isPresent()) {
            component = component.set("target", dynamic.emptyMap().set("pos", lodestonePos.get()).set("dimension", lodestoneDimension.get()));
         }

         if (!lodestoneTracked) {
            component = component.set("tracked", dynamic.createBoolean(false));
         }

         itemStack.setComponent("minecraft:lodestone_tracker", component);
      }
   }

   private static void fixFireworkStar(final ItemStackComponentizationFix.ItemStackData itemStack) {
      itemStack.fixSubTag("Explosion", true, explosion -> {
         itemStack.setComponent("minecraft:firework_explosion", fixFireworkExplosion(explosion));
         return explosion.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
      });
   }

   private static void fixFireworkRocket(final ItemStackComponentizationFix.ItemStackData itemStack) {
      itemStack.fixSubTag(
         "Fireworks",
         true,
         fireworks -> {
            Stream<? extends Dynamic<?>> explosions = fireworks.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
            int flight = fireworks.get("Flight").asInt(0);
            itemStack.setComponent(
               "minecraft:fireworks",
               fireworks.emptyMap().set("explosions", fireworks.createList(explosions)).set("flight_duration", fireworks.createByte((byte)flight))
            );
            return fireworks.remove("Explosions").remove("Flight");
         }
      );
   }

   private static Dynamic<?> fixFireworkExplosion(Dynamic<?> explosion) {
      explosion = explosion.set("shape", explosion.createString(switch (explosion.get("Type").asInt(0)) {
         case 1 -> "large_ball";
         case 2 -> "star";
         case 3 -> "creeper";
         case 4 -> "burst";
         default -> "small_ball";
      })).remove("Type");
      explosion = explosion.renameField("Colors", "colors");
      explosion = explosion.renameField("FadeColors", "fade_colors");
      explosion = explosion.renameField("Trail", "has_trail");
      return explosion.renameField("Flicker", "has_twinkle");
   }

   public static Dynamic<?> fixProfile(final Dynamic<?> dynamic) {
      Optional<String> simpleName = dynamic.asString().result();
      if (simpleName.isPresent()) {
         return isValidPlayerName(simpleName.get()) ? dynamic.emptyMap().set("name", dynamic.createString(simpleName.get())) : dynamic.emptyMap();
      }

      String name = dynamic.get("Name").asString("");
      Optional<? extends Dynamic<?>> id = dynamic.get("Id").result();
      Dynamic<?> properties = fixProfileProperties(dynamic.get("Properties"));
      Dynamic<?> profile = dynamic.emptyMap();
      if (isValidPlayerName(name)) {
         profile = profile.set("name", dynamic.createString(name));
      }

      if (id.isPresent()) {
         profile = profile.set("id", id.get());
      }

      if (properties != null) {
         profile = profile.set("properties", properties);
      }

      return profile;
   }

   private static boolean isValidPlayerName(final String name) {
      return name.length() > 16 ? false : name.chars().filter(c -> c <= 32 || c >= 127).findAny().isEmpty();
   }

   private static @Nullable Dynamic<?> fixProfileProperties(final OptionalDynamic<?> dynamic) {
      Map<String, List<Pair<String, Optional<String>>>> properties = dynamic.asMap(key -> key.asString(""), list -> list.asList(property -> {
         String value = property.get("Value").asString("");
         Optional<String> signature = property.get("Signature").asString().result();
         return Pair.of(value, signature);
      }));
      return properties.isEmpty() ? null : dynamic.createList(properties.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(pair -> {
         Dynamic<?> property = dynamic.emptyMap().set("name", dynamic.createString(entry.getKey())).set("value", dynamic.createString((String)pair.getFirst()));
         Optional<String> signature = (Optional<String>)pair.getSecond();
         return signature.isPresent() ? property.set("signature", dynamic.createString(signature.get())) : property;
      })));
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ItemStack componentization",
         this.getInputSchema().getType(References.ITEM_STACK),
         this.getOutputSchema().getType(References.ITEM_STACK),
         dynamic -> {
            Optional<? extends Dynamic<?>> fixedItemStack = ItemStackComponentizationFix.ItemStackData.read(dynamic).map(itemStack -> {
               fixItemStack(itemStack, itemStack.tag);
               return itemStack.write();
            });
            return (Dynamic)DataFixUtils.orElse(fixedItemStack, dynamic);
         }
      );
   }

   private static class ItemStackData {
      private final String item;
      private final int count;
      private Dynamic<?> components;
      private final Dynamic<?> remainder;
      private Dynamic<?> tag;

      private ItemStackData(final String item, final int count, final Dynamic<?> remainder) {
         this.item = NamespacedSchema.ensureNamespaced(item);
         this.count = count;
         this.components = remainder.emptyMap();
         this.tag = remainder.get("tag").orElseEmptyMap();
         this.remainder = remainder.remove("tag");
      }

      public static Optional<ItemStackComponentizationFix.ItemStackData> read(final Dynamic<?> dynamic) {
         return dynamic.get("id")
            .asString()
            .apply2stable(
               (item, count) -> new ItemStackComponentizationFix.ItemStackData(item, count.intValue(), dynamic.remove("id").remove("Count")),
               dynamic.get("Count").asNumber()
            )
            .result();
      }

      public OptionalDynamic<?> removeTag(final String key) {
         OptionalDynamic<?> value = this.tag.get(key);
         this.tag = this.tag.remove(key);
         return value;
      }

      public void setComponent(final String type, final Dynamic<?> value) {
         this.components = this.components.set(type, value);
      }

      public void setComponent(final String type, final OptionalDynamic<?> optionalValue) {
         optionalValue.result().ifPresent(value -> this.components = this.components.set(type, value));
      }

      public Dynamic<?> moveTagInto(final String fromKey, final Dynamic<?> target, final String toKey) {
         Optional<? extends Dynamic<?>> value = this.removeTag(fromKey).result();
         return value.isPresent() ? target.set(toKey, value.get()) : target;
      }

      public void moveTagToComponent(final String key, final String type, final Dynamic<?> defaultValue) {
         Optional<? extends Dynamic<?>> value = this.removeTag(key).result();
         if (value.isPresent() && !value.get().equals(defaultValue)) {
            this.setComponent(type, (Dynamic<?>)value.get());
         }
      }

      public void moveTagToComponent(final String key, final String type) {
         this.removeTag(key).result().ifPresent(value -> this.setComponent(type, (Dynamic<?>)value));
      }

      public void fixSubTag(final String key, final boolean dontFixWhenFieldIsMissing, final UnaryOperator<Dynamic<?>> function) {
         OptionalDynamic<?> value = this.tag.get(key);
         if (!dontFixWhenFieldIsMissing || !value.result().isEmpty()) {
            Dynamic<?> map = value.orElseEmptyMap();
            map = function.apply(map);
            if (map.equals(map.emptyMap())) {
               this.tag = this.tag.remove(key);
            } else {
               this.tag = this.tag.set(key, map);
            }
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> result = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
         if (!this.tag.equals(this.tag.emptyMap())) {
            this.components = this.components.set("minecraft:custom_data", this.tag);
         }

         if (!this.components.equals(this.tag.emptyMap())) {
            result = result.set("components", this.components);
         }

         return mergeRemainder(result, this.remainder);
      }

      private static <T> Dynamic<T> mergeRemainder(final Dynamic<T> itemStack, final Dynamic<?> remainder) {
         DynamicOps<T> ops = itemStack.getOps();
         return ops.getMap(itemStack.getValue())
            .flatMap(itemStackMap -> ops.mergeToMap(remainder.convert(ops).getValue(), itemStackMap))
            .map(merged -> new Dynamic(ops, merged))
            .result()
            .orElse(itemStack);
      }

      public boolean is(final String id) {
         return this.item.equals(id);
      }

      public boolean is(final Set<String> ids) {
         return ids.contains(this.item);
      }

      public boolean hasComponent(final String id) {
         return this.components.get(id).result().isPresent();
      }
   }
}
