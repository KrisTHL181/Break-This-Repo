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

   public ItemStackComponentizationFix(Schema p_331666_) {
      super(p_331666_, true);
   }

   private static void fixItemStack(ItemStackComponentizationFix.ItemStackData p_335726_, Dynamic<?> p_329007_) {
      int i = p_335726_.removeTag("HideFlags").asInt(0);
      p_335726_.moveTagToComponent("Damage", "minecraft:damage", p_329007_.createInt(0));
      p_335726_.moveTagToComponent("RepairCost", "minecraft:repair_cost", p_329007_.createInt(0));
      p_335726_.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
      p_335726_.removeTag("BlockStateTag")
         .result()
         .ifPresent(p_333568_ -> p_335726_.setComponent("minecraft:block_state", fixBlockStateTag((Dynamic<?>)p_333568_)));
      p_335726_.moveTagToComponent("EntityTag", "minecraft:entity_data");
      p_335726_.fixSubTag("BlockEntityTag", false, p_330163_ -> {
         String s = NamespacedSchema.ensureNamespaced(p_330163_.get("id").asString(""));
         p_330163_ = fixBlockEntityTag(p_335726_, p_330163_, s);
         Dynamic<?> dynamic2 = p_330163_.remove("id");
         return dynamic2.equals(p_330163_.emptyMap()) ? dynamic2 : p_330163_;
      });
      p_335726_.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
      if (p_335726_.removeTag("Unbreakable").asBoolean(false)) {
         Dynamic<?> dynamic = p_329007_.emptyMap();
         if ((i & 4) != 0) {
            dynamic = dynamic.set("show_in_tooltip", p_329007_.createBoolean(false));
         }

         p_335726_.setComponent("minecraft:unbreakable", dynamic);
      }

      fixEnchantments(p_335726_, p_329007_, "Enchantments", "minecraft:enchantments", (i & 1) != 0);
      if (p_335726_.is("minecraft:enchanted_book")) {
         fixEnchantments(p_335726_, p_329007_, "StoredEnchantments", "minecraft:stored_enchantments", (i & 32) != 0);
      }

      p_335726_.fixSubTag("display", false, p_332023_ -> fixDisplay(p_335726_, p_332023_, i));
      fixAdventureModeChecks(p_335726_, p_329007_, i);
      fixAttributeModifiers(p_335726_, p_329007_, i);
      Optional<? extends Dynamic<?>> optional = p_335726_.removeTag("Trim").result();
      if (optional.isPresent()) {
         Dynamic<?> dynamic1 = (Dynamic<?>)optional.get();
         if ((i & 128) != 0) {
            dynamic1 = dynamic1.set("show_in_tooltip", dynamic1.createBoolean(false));
         }

         p_335726_.setComponent("minecraft:trim", dynamic1);
      }

      if ((i & 32) != 0) {
         p_335726_.setComponent("minecraft:hide_additional_tooltip", p_329007_.emptyMap());
      }

      if (p_335726_.is("minecraft:crossbow")) {
         p_335726_.removeTag("Charged");
         p_335726_.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", p_329007_.createList(Stream.empty()));
      }

      if (p_335726_.is("minecraft:bundle")) {
         p_335726_.moveTagToComponent("Items", "minecraft:bundle_contents", p_329007_.createList(Stream.empty()));
      }

      if (p_335726_.is("minecraft:filled_map")) {
         p_335726_.moveTagToComponent("map", "minecraft:map_id");
         Map<? extends Dynamic<?>, ? extends Dynamic<?>> map = p_335726_.removeTag("Decorations")
            .asStream()
            .map(ItemStackComponentizationFix::fixMapDecoration)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (p_328286_, p_333385_) -> p_328286_));
         if (!map.isEmpty()) {
            p_335726_.setComponent("minecraft:map_decorations", p_329007_.createMap(map));
         }
      }

      if (p_335726_.is(POTION_HOLDER_IDS)) {
         fixPotionContents(p_335726_, p_329007_);
      }

      if (p_335726_.is("minecraft:writable_book")) {
         fixWritableBook(p_335726_, p_329007_);
      }

      if (p_335726_.is("minecraft:written_book")) {
         fixWrittenBook(p_335726_, p_329007_);
      }

      if (p_335726_.is("minecraft:suspicious_stew")) {
         p_335726_.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
      }

      if (p_335726_.is("minecraft:debug_stick")) {
         p_335726_.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
      }

      if (p_335726_.is(BUCKETED_MOB_IDS)) {
         fixBucketedMobData(p_335726_, p_329007_);
      }

      if (p_335726_.is("minecraft:goat_horn")) {
         p_335726_.moveTagToComponent("instrument", "minecraft:instrument");
      }

      if (p_335726_.is("minecraft:knowledge_book")) {
         p_335726_.moveTagToComponent("Recipes", "minecraft:recipes");
      }

      if (p_335726_.is("minecraft:compass")) {
         fixLodestoneTracker(p_335726_, p_329007_);
      }

      if (p_335726_.is("minecraft:firework_rocket")) {
         fixFireworkRocket(p_335726_);
      }

      if (p_335726_.is("minecraft:firework_star")) {
         fixFireworkStar(p_335726_);
      }

      if (p_335726_.is("minecraft:player_head")) {
         p_335726_.removeTag("SkullOwner").result().ifPresent(p_328052_ -> p_335726_.setComponent("minecraft:profile", fixProfile((Dynamic<?>)p_328052_)));
      }
   }

   private static Dynamic<?> fixBlockStateTag(Dynamic<?> p_329060_) {
      return (Dynamic<?>)DataFixUtils.orElse(p_329060_.asMapOpt().result().map(p_331937_ -> p_331937_.collect(Collectors.toMap(Pair::getFirst, p_330078_ -> {
         String s = ((Dynamic)p_330078_.getFirst()).asString("");
         Dynamic<?> dynamic = (Dynamic<?>)p_330078_.getSecond();
         if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(s)) {
            Optional<Boolean> optional = dynamic.asBoolean().result();
            if (optional.isPresent()) {
               return dynamic.createString(String.valueOf(optional.get()));
            }
         }

         Optional<Number> optional1 = dynamic.asNumber().result();
         return optional1.isPresent() ? dynamic.createString(optional1.get().toString()) : dynamic;
      }))).map(p_329060_::createMap), p_329060_);
   }

   private static Dynamic<?> fixDisplay(ItemStackComponentizationFix.ItemStackData p_333136_, Dynamic<?> p_329974_, int p_330088_) {
      p_329974_.get("Name")
         .result()
         .filter(LegacyComponentDataFixUtils::isStrictlyValidJson)
         .ifPresent(p_421550_ -> p_333136_.setComponent("minecraft:custom_name", (Dynamic<?>)p_421550_));
      OptionalDynamic<?> optionaldynamic = p_329974_.get("Lore");
      if (optionaldynamic.result().isPresent()) {
         p_333136_.setComponent(
            "minecraft:lore", p_329974_.createList(p_329974_.get("Lore").asStream().filter(LegacyComponentDataFixUtils::isStrictlyValidJson))
         );
      }

      Optional<Integer> optional = p_329974_.get("color").asNumber().result().map(Number::intValue);
      boolean flag = (p_330088_ & 64) != 0;
      if (optional.isPresent() || flag) {
         Dynamic<?> dynamic = p_329974_.emptyMap().set("rgb", p_329974_.createInt(optional.orElse(10511680)));
         if (flag) {
            dynamic = dynamic.set("show_in_tooltip", p_329974_.createBoolean(false));
         }

         p_333136_.setComponent("minecraft:dyed_color", dynamic);
      }

      Optional<String> optional1 = p_329974_.get("LocName").asString().result();
      if (optional1.isPresent()) {
         p_333136_.setComponent("minecraft:item_name", LegacyComponentDataFixUtils.createTranslatableComponent(p_329974_.getOps(), optional1.get()));
      }

      if (p_333136_.is("minecraft:filled_map")) {
         p_333136_.setComponent("minecraft:map_color", p_329974_.get("MapColor"));
         p_329974_ = p_329974_.remove("MapColor");
      }

      return p_329974_.remove("Name").remove("Lore").remove("color").remove("LocName");
   }

   private static <T> Dynamic<T> fixBlockEntityTag(ItemStackComponentizationFix.ItemStackData p_334120_, Dynamic<T> p_332622_, String p_334133_) {
      p_334120_.setComponent("minecraft:lock", p_332622_.get("Lock"));
      p_332622_ = p_332622_.remove("Lock");
      Optional<Dynamic<T>> optional = p_332622_.get("LootTable").result();
      if (optional.isPresent()) {
         Dynamic<T> dynamic = p_332622_.emptyMap().set("loot_table", optional.get());
         long i = p_332622_.get("LootTableSeed").asLong(0L);
         if (i != 0L) {
            dynamic = dynamic.set("seed", p_332622_.createLong(i));
         }

         p_334120_.setComponent("minecraft:container_loot", dynamic);
         p_332622_ = p_332622_.remove("LootTable").remove("LootTableSeed");
      }
      return switch (p_334133_) {
         case "minecraft:skull" -> {
            p_334120_.setComponent("minecraft:note_block_sound", p_332622_.get("note_block_sound"));
            yield p_332622_.remove("note_block_sound");
         }
         case "minecraft:decorated_pot" -> {
            p_334120_.setComponent("minecraft:pot_decorations", p_332622_.get("sherds"));
            Optional<Dynamic<T>> optional2 = p_332622_.get("item").result();
            if (optional2.isPresent()) {
               p_334120_.setComponent(
                  "minecraft:container", p_332622_.createList(Stream.of(p_332622_.emptyMap().set("slot", p_332622_.createInt(0)).set("item", optional2.get())))
               );
            }

            yield p_332622_.remove("sherds").remove("item");
         }
         case "minecraft:banner" -> {
            p_334120_.setComponent("minecraft:banner_patterns", p_332622_.get("patterns"));
            Optional<Number> optional1 = p_332622_.get("Base").asNumber().result();
            if (optional1.isPresent()) {
               p_334120_.setComponent("minecraft:base_color", p_332622_.createString(ExtraDataFixUtils.dyeColorIdToName(optional1.get().intValue())));
            }

            yield p_332622_.remove("patterns").remove("Base");
         }
         case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
            List<Dynamic<T>> list = p_332622_.get("Items")
               .asList(
                  p_334204_ -> p_334204_.emptyMap()
                     .set("slot", p_334204_.createInt(p_334204_.get("Slot").asByte((byte)0) & 255))
                     .set("item", p_334204_.remove("Slot"))
               );
            if (!list.isEmpty()) {
               p_334120_.setComponent("minecraft:container", p_332622_.createList(list.stream()));
            }

            yield p_332622_.remove("Items");
         }
         case "minecraft:beehive" -> {
            p_334120_.setComponent("minecraft:bees", p_332622_.get("bees"));
            yield p_332622_.remove("bees");
         }
         default -> p_332622_;
      };
   }

   private static void fixEnchantments(
      ItemStackComponentizationFix.ItemStackData p_332552_, Dynamic<?> p_328849_, String p_333260_, String p_334340_, boolean p_329498_
   ) {
      OptionalDynamic<?> optionaldynamic = p_332552_.removeTag(p_333260_);
      List<Pair<String, Integer>> list = optionaldynamic.asList(Function.identity())
         .stream()
         .flatMap(p_331550_ -> parseEnchantment((Dynamic<?>)p_331550_).stream())
         .filter(p_421548_ -> (Integer)p_421548_.getSecond() > 0)
         .toList();
      if (!list.isEmpty() || p_329498_) {
         Dynamic<?> dynamic = p_328849_.emptyMap();
         Dynamic<?> dynamic1 = p_328849_.emptyMap();

         for (Pair<String, Integer> pair : list) {
            dynamic1 = dynamic1.set((String)pair.getFirst(), p_328849_.createInt((Integer)pair.getSecond()));
         }

         dynamic = dynamic.set("levels", dynamic1);
         if (p_329498_) {
            dynamic = dynamic.set("show_in_tooltip", p_328849_.createBoolean(false));
         }

         p_332552_.setComponent(p_334340_, dynamic);
      }

      if (optionaldynamic.result().isPresent() && list.isEmpty()) {
         p_332552_.setComponent("minecraft:enchantment_glint_override", p_328849_.createBoolean(true));
      }
   }

   private static Optional<Pair<String, Integer>> parseEnchantment(Dynamic<?> p_328387_) {
      return p_328387_.get("id")
         .asString()
         .apply2stable((p_335475_, p_333076_) -> Pair.of(p_335475_, Mth.clamp(p_333076_.intValue(), 0, 255)), p_328387_.get("lvl").asNumber())
         .result();
   }

   private static void fixAdventureModeChecks(ItemStackComponentizationFix.ItemStackData p_328938_, Dynamic<?> p_336252_, int p_331810_) {
      fixBlockStatePredicates(p_328938_, p_336252_, "CanDestroy", "minecraft:can_break", (p_331810_ & 8) != 0);
      fixBlockStatePredicates(p_328938_, p_336252_, "CanPlaceOn", "minecraft:can_place_on", (p_331810_ & 16) != 0);
   }

   private static void fixBlockStatePredicates(
      ItemStackComponentizationFix.ItemStackData p_331433_, Dynamic<?> p_332377_, String p_332474_, String p_333138_, boolean p_334219_
   ) {
      Optional<? extends Dynamic<?>> optional = p_331433_.removeTag(p_332474_).result();
      if (!optional.isEmpty()) {
         Dynamic<?> dynamic = p_332377_.emptyMap()
            .set(
               "predicates",
               p_332377_.createList(
                  optional.get()
                     .asStream()
                     .map(
                        p_336172_ -> (Dynamic)DataFixUtils.orElse(
                           p_336172_.asString().map(p_330317_ -> fixBlockStatePredicate((Dynamic<?>)p_336172_, p_330317_)).result(), p_336172_
                        )
                     )
               )
            );
         if (p_334219_) {
            dynamic = dynamic.set("show_in_tooltip", p_332377_.createBoolean(false));
         }

         p_331433_.setComponent(p_333138_, dynamic);
      }
   }

   private static Dynamic<?> fixBlockStatePredicate(Dynamic<?> p_330250_, String p_335482_) {
      int i = p_335482_.indexOf(91);
      int j = p_335482_.indexOf(123);
      int k = p_335482_.length();
      if (i != -1) {
         k = i;
      }

      if (j != -1) {
         k = Math.min(k, j);
      }

      String s = p_335482_.substring(0, k);
      Dynamic<?> dynamic = p_330250_.emptyMap().set("blocks", p_330250_.createString(s.trim()));
      int l = p_335482_.indexOf(93);
      if (i != -1 && l != -1) {
         Dynamic<?> dynamic1 = p_330250_.emptyMap();

         for (String s1 : PROPERTY_SPLITTER.split(p_335482_.substring(i + 1, l))) {
            int i1 = s1.indexOf(61);
            if (i1 != -1) {
               String s2 = s1.substring(0, i1).trim();
               String s3 = s1.substring(i1 + 1).trim();
               dynamic1 = dynamic1.set(s2, p_330250_.createString(s3));
            }
         }

         dynamic = dynamic.set("state", dynamic1);
      }

      int j1 = p_335482_.indexOf(125);
      if (j != -1 && j1 != -1) {
         dynamic = dynamic.set("nbt", p_330250_.createString(p_335482_.substring(j, j1 + 1)));
      }

      return dynamic;
   }

   private static void fixAttributeModifiers(ItemStackComponentizationFix.ItemStackData p_329869_, Dynamic<?> p_332943_, int p_330062_) {
      OptionalDynamic<?> optionaldynamic = p_329869_.removeTag("AttributeModifiers");
      if (!optionaldynamic.result().isEmpty()) {
         boolean flag = (p_330062_ & 2) != 0;
         List<? extends Dynamic<?>> list = optionaldynamic.asList(ItemStackComponentizationFix::fixAttributeModifier);
         Dynamic<?> dynamic = p_332943_.emptyMap().set("modifiers", p_332943_.createList(list.stream()));
         if (flag) {
            dynamic = dynamic.set("show_in_tooltip", p_332943_.createBoolean(false));
         }

         p_329869_.setComponent("minecraft:attribute_modifiers", dynamic);
      }
   }

   private static Dynamic<?> fixAttributeModifier(Dynamic<?> p_330280_) {
      Dynamic<?> dynamic = p_330280_.emptyMap()
         .set("name", p_330280_.createString(""))
         .set("amount", p_330280_.createDouble(0.0))
         .set("operation", p_330280_.createString("add_value"));
      dynamic = Dynamic.copyField(p_330280_, "AttributeName", dynamic, "type");
      dynamic = Dynamic.copyField(p_330280_, "Slot", dynamic, "slot");
      dynamic = Dynamic.copyField(p_330280_, "UUID", dynamic, "uuid");
      dynamic = Dynamic.copyField(p_330280_, "Name", dynamic, "name");
      dynamic = Dynamic.copyField(p_330280_, "Amount", dynamic, "amount");
      return Dynamic.copyAndFixField(p_330280_, "Operation", dynamic, "operation", p_334772_ -> {
         return p_334772_.createString(switch (p_334772_.asInt(0)) {
            case 1 -> "add_multiplied_base";
            case 2 -> "add_multiplied_total";
            default -> "add_value";
         });
      });
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(Dynamic<?> p_329859_) {
      Dynamic<?> dynamic = (Dynamic<?>)DataFixUtils.orElseGet(p_329859_.get("id").result(), () -> p_329859_.createString(""));
      Dynamic<?> dynamic1 = p_329859_.emptyMap()
         .set("type", p_329859_.createString(fixMapDecorationType(p_329859_.get("type").asInt(0))))
         .set("x", p_329859_.createDouble(p_329859_.get("x").asDouble(0.0)))
         .set("z", p_329859_.createDouble(p_329859_.get("z").asDouble(0.0)))
         .set("rotation", p_329859_.createFloat((float)p_329859_.get("rot").asDouble(0.0)));
      return Pair.of(dynamic, dynamic1);
   }

   private static String fixMapDecorationType(int p_328497_) {
      return switch (p_328497_) {
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

   private static void fixPotionContents(ItemStackComponentizationFix.ItemStackData p_329173_, Dynamic<?> p_331866_) {
      Dynamic<?> dynamic = p_331866_.emptyMap();
      Optional<String> optional = p_329173_.removeTag("Potion").asString().result().filter(p_334871_ -> !p_334871_.equals("minecraft:empty"));
      if (optional.isPresent()) {
         dynamic = dynamic.set("potion", p_331866_.createString(optional.get()));
      }

      dynamic = p_329173_.moveTagInto("CustomPotionColor", dynamic, "custom_color");
      dynamic = p_329173_.moveTagInto("custom_potion_effects", dynamic, "custom_effects");
      if (!dynamic.equals(p_331866_.emptyMap())) {
         p_329173_.setComponent("minecraft:potion_contents", dynamic);
      }
   }

   private static void fixWritableBook(ItemStackComponentizationFix.ItemStackData p_332414_, Dynamic<?> p_329764_) {
      Dynamic<?> dynamic = fixBookPages(p_332414_, p_329764_);
      if (dynamic != null) {
         p_332414_.setComponent("minecraft:writable_book_content", p_329764_.emptyMap().set("pages", dynamic));
      }
   }

   private static void fixWrittenBook(ItemStackComponentizationFix.ItemStackData p_333609_, Dynamic<?> p_330312_) {
      Dynamic<?> dynamic = fixBookPages(p_333609_, p_330312_);
      String s = p_333609_.removeTag("title").asString("");
      Optional<String> optional = p_333609_.removeTag("filtered_title").asString().result();
      Dynamic<?> dynamic1 = p_330312_.emptyMap();
      dynamic1 = dynamic1.set("title", createFilteredText(p_330312_, s, optional));
      dynamic1 = p_333609_.moveTagInto("author", dynamic1, "author");
      dynamic1 = p_333609_.moveTagInto("resolved", dynamic1, "resolved");
      dynamic1 = p_333609_.moveTagInto("generation", dynamic1, "generation");
      if (dynamic != null) {
         dynamic1 = dynamic1.set("pages", dynamic);
      }

      p_333609_.setComponent("minecraft:written_book_content", dynamic1);
   }

   private static @Nullable Dynamic<?> fixBookPages(ItemStackComponentizationFix.ItemStackData p_335763_, Dynamic<?> p_328532_) {
      List<String> list = p_335763_.removeTag("pages").asList(p_331615_ -> p_331615_.asString(""));
      Map<String, String> map = p_335763_.removeTag("filtered_pages").asMap(p_335169_ -> p_335169_.asString("0"), p_329927_ -> p_329927_.asString(""));
      if (list.isEmpty()) {
         return null;
      }

      List<Dynamic<?>> list1 = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         String s = list.get(i);
         String s1 = map.get(String.valueOf(i));
         list1.add(createFilteredText(p_328532_, s, Optional.ofNullable(s1)));
      }

      return p_328532_.createList(list1.stream());
   }

   private static Dynamic<?> createFilteredText(Dynamic<?> p_330363_, String p_328510_, Optional<String> p_328222_) {
      Dynamic<?> dynamic = p_330363_.emptyMap().set("raw", p_330363_.createString(p_328510_));
      if (p_328222_.isPresent()) {
         dynamic = dynamic.set("filtered", p_330363_.createString(p_328222_.get()));
      }

      return dynamic;
   }

   private static void fixBucketedMobData(ItemStackComponentizationFix.ItemStackData p_328428_, Dynamic<?> p_327719_) {
      Dynamic<?> dynamic = p_327719_.emptyMap();

      for (String s : BUCKETED_MOB_TAGS) {
         dynamic = p_328428_.moveTagInto(s, dynamic, s);
      }

      if (!dynamic.equals(p_327719_.emptyMap())) {
         p_328428_.setComponent("minecraft:bucket_entity_data", dynamic);
      }
   }

   private static void fixLodestoneTracker(ItemStackComponentizationFix.ItemStackData p_330486_, Dynamic<?> p_329466_) {
      Optional<? extends Dynamic<?>> optional = p_330486_.removeTag("LodestonePos").result();
      Optional<? extends Dynamic<?>> optional1 = p_330486_.removeTag("LodestoneDimension").result();
      if (!optional.isEmpty() || !optional1.isEmpty()) {
         boolean flag = p_330486_.removeTag("LodestoneTracked").asBoolean(true);
         Dynamic<?> dynamic = p_329466_.emptyMap();
         if (optional.isPresent() && optional1.isPresent()) {
            dynamic = dynamic.set("target", p_329466_.emptyMap().set("pos", optional.get()).set("dimension", optional1.get()));
         }

         if (!flag) {
            dynamic = dynamic.set("tracked", p_329466_.createBoolean(false));
         }

         p_330486_.setComponent("minecraft:lodestone_tracker", dynamic);
      }
   }

   private static void fixFireworkStar(ItemStackComponentizationFix.ItemStackData p_334817_) {
      p_334817_.fixSubTag("Explosion", true, p_334469_ -> {
         p_334817_.setComponent("minecraft:firework_explosion", fixFireworkExplosion(p_334469_));
         return p_334469_.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
      });
   }

   private static void fixFireworkRocket(ItemStackComponentizationFix.ItemStackData p_329939_) {
      p_329939_.fixSubTag(
         "Fireworks",
         true,
         p_335612_ -> {
            Stream<? extends Dynamic<?>> stream = p_335612_.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
            int i = p_335612_.get("Flight").asInt(0);
            p_329939_.setComponent(
               "minecraft:fireworks",
               p_335612_.emptyMap().set("explosions", p_335612_.createList(stream)).set("flight_duration", p_335612_.createByte((byte)i))
            );
            return p_335612_.remove("Explosions").remove("Flight");
         }
      );
   }

   private static Dynamic<?> fixFireworkExplosion(Dynamic<?> p_327955_) {
      p_327955_ = p_327955_.set("shape", p_327955_.createString(switch (p_327955_.get("Type").asInt(0)) {
         case 1 -> "large_ball";
         case 2 -> "star";
         case 3 -> "creeper";
         case 4 -> "burst";
         default -> "small_ball";
      })).remove("Type");
      p_327955_ = p_327955_.renameField("Colors", "colors");
      p_327955_ = p_327955_.renameField("FadeColors", "fade_colors");
      p_327955_ = p_327955_.renameField("Trail", "has_trail");
      return p_327955_.renameField("Flicker", "has_twinkle");
   }

   public static Dynamic<?> fixProfile(Dynamic<?> p_331244_) {
      Optional<String> optional = p_331244_.asString().result();
      if (optional.isPresent()) {
         return isValidPlayerName(optional.get()) ? p_331244_.emptyMap().set("name", p_331244_.createString(optional.get())) : p_331244_.emptyMap();
      }

      String s = p_331244_.get("Name").asString("");
      Optional<? extends Dynamic<?>> optional1 = p_331244_.get("Id").result();
      Dynamic<?> dynamic = fixProfileProperties(p_331244_.get("Properties"));
      Dynamic<?> dynamic1 = p_331244_.emptyMap();
      if (isValidPlayerName(s)) {
         dynamic1 = dynamic1.set("name", p_331244_.createString(s));
      }

      if (optional1.isPresent()) {
         dynamic1 = dynamic1.set("id", optional1.get());
      }

      if (dynamic != null) {
         dynamic1 = dynamic1.set("properties", dynamic);
      }

      return dynamic1;
   }

   private static boolean isValidPlayerName(String p_332205_) {
      return p_332205_.length() > 16 ? false : p_332205_.chars().filter(p_331992_ -> p_331992_ <= 32 || p_331992_ >= 127).findAny().isEmpty();
   }

   private static @Nullable Dynamic<?> fixProfileProperties(OptionalDynamic<?> p_329629_) {
      Map<String, List<Pair<String, Optional<String>>>> map = p_329629_.asMap(p_330311_ -> p_330311_.asString(""), p_331808_ -> p_331808_.asList(p_329294_ -> {
         String s = p_329294_.get("Value").asString("");
         Optional<String> optional = p_329294_.get("Signature").asString().result();
         return Pair.of(s, optional);
      }));
      return map.isEmpty()
         ? null
         : p_329629_.createList(
            map.entrySet()
               .stream()
               .flatMap(
                  p_334576_ -> p_334576_.getValue()
                     .stream()
                     .map(
                        p_335577_ -> {
                           Dynamic<?> dynamic = p_329629_.emptyMap()
                              .set("name", p_329629_.createString(p_334576_.getKey()))
                              .set("value", p_329629_.createString((String)p_335577_.getFirst()));
                           Optional<String> optional = (Optional<String>)p_335577_.getSecond();
                           return optional.isPresent() ? dynamic.set("signature", p_329629_.createString(optional.get())) : dynamic;
                        }
                     )
               )
         );
   }

   protected TypeRewriteRule makeRule() {
      return this.writeFixAndRead(
         "ItemStack componentization",
         this.getInputSchema().getType(References.ITEM_STACK),
         this.getOutputSchema().getType(References.ITEM_STACK),
         p_332668_ -> {
            Optional<? extends Dynamic<?>> optional = ItemStackComponentizationFix.ItemStackData.read(p_332668_).map(p_329220_ -> {
               fixItemStack(p_329220_, p_329220_.tag);
               return p_329220_.write();
            });
            return (Dynamic)DataFixUtils.orElse(optional, p_332668_);
         }
      );
   }

   static class ItemStackData {
      private final String item;
      private final int count;
      private Dynamic<?> components;
      private final Dynamic<?> remainder;
      Dynamic<?> tag;

      private ItemStackData(String p_330519_, int p_328981_, Dynamic<?> p_329116_) {
         this.item = NamespacedSchema.ensureNamespaced(p_330519_);
         this.count = p_328981_;
         this.components = p_329116_.emptyMap();
         this.tag = p_329116_.get("tag").orElseEmptyMap();
         this.remainder = p_329116_.remove("tag");
      }

      public static Optional<ItemStackComponentizationFix.ItemStackData> read(Dynamic<?> p_327873_) {
         return p_327873_.get("id")
            .asString()
            .apply2stable(
               (p_336038_, p_334919_) -> new ItemStackComponentizationFix.ItemStackData(p_336038_, p_334919_.intValue(), p_327873_.remove("id").remove("Count")),
               p_327873_.get("Count").asNumber()
            )
            .result();
      }

      public OptionalDynamic<?> removeTag(String p_327670_) {
         OptionalDynamic<?> optionaldynamic = this.tag.get(p_327670_);
         this.tag = this.tag.remove(p_327670_);
         return optionaldynamic;
      }

      public void setComponent(String p_334273_, Dynamic<?> p_334484_) {
         this.components = this.components.set(p_334273_, p_334484_);
      }

      public void setComponent(String p_333518_, OptionalDynamic<?> p_335463_) {
         p_335463_.result().ifPresent(p_334065_ -> this.components = this.components.set(p_333518_, p_334065_));
      }

      public Dynamic<?> moveTagInto(String p_333380_, Dynamic<?> p_332781_, String p_332207_) {
         Optional<? extends Dynamic<?>> optional = this.removeTag(p_333380_).result();
         return optional.isPresent() ? p_332781_.set(p_332207_, optional.get()) : p_332781_;
      }

      public void moveTagToComponent(String p_330725_, String p_334538_, Dynamic<?> p_328097_) {
         Optional<? extends Dynamic<?>> optional = this.removeTag(p_330725_).result();
         if (optional.isPresent() && !optional.get().equals(p_328097_)) {
            this.setComponent(p_334538_, (Dynamic<?>)optional.get());
         }
      }

      public void moveTagToComponent(String p_334513_, String p_335629_) {
         this.removeTag(p_334513_).result().ifPresent(p_331999_ -> this.setComponent(p_335629_, (Dynamic<?>)p_331999_));
      }

      public void fixSubTag(String p_334224_, boolean p_331760_, UnaryOperator<Dynamic<?>> p_335156_) {
         OptionalDynamic<?> optionaldynamic = this.tag.get(p_334224_);
         if (!p_331760_ || !optionaldynamic.result().isEmpty()) {
            Dynamic<?> dynamic = optionaldynamic.orElseEmptyMap();
            dynamic = p_335156_.apply(dynamic);
            if (dynamic.equals(dynamic.emptyMap())) {
               this.tag = this.tag.remove(p_334224_);
            } else {
               this.tag = this.tag.set(p_334224_, dynamic);
            }
         }
      }

      public Dynamic<?> write() {
         Dynamic<?> dynamic = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
         if (!this.tag.equals(this.tag.emptyMap())) {
            this.components = this.components.set("minecraft:custom_data", this.tag);
         }

         if (!this.components.equals(this.tag.emptyMap())) {
            dynamic = dynamic.set("components", this.components);
         }

         return mergeRemainder(dynamic, this.remainder);
      }

      private static <T> Dynamic<T> mergeRemainder(Dynamic<T> p_331283_, Dynamic<?> p_335645_) {
         DynamicOps<T> dynamicops = p_331283_.getOps();
         return dynamicops.getMap(p_331283_.getValue())
            .flatMap(p_335224_ -> dynamicops.mergeToMap(p_335645_.convert(dynamicops).getValue(), p_335224_))
            .map(p_334956_ -> new Dynamic(dynamicops, p_334956_))
            .result()
            .orElse(p_331283_);
      }

      public boolean is(String p_328447_) {
         return this.item.equals(p_328447_);
      }

      public boolean is(Set<String> p_332697_) {
         return p_332697_.contains(this.item);
      }

      public boolean hasComponent(String p_329543_) {
         return this.components.get(p_329543_).result().isPresent();
      }
   }
}
