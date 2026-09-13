package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class DataComponentPredicates {
   public static final DataComponentPredicate.Type<DamagePredicate> DAMAGE = register("damage", DamagePredicate.CODEC);
   public static final DataComponentPredicate.Type<EnchantmentsPredicate.Enchantments> ENCHANTMENTS = register(
      "enchantments", EnchantmentsPredicate.Enchantments.CODEC
   );
   public static final DataComponentPredicate.Type<EnchantmentsPredicate.StoredEnchantments> STORED_ENCHANTMENTS = register(
      "stored_enchantments", EnchantmentsPredicate.StoredEnchantments.CODEC
   );
   public static final DataComponentPredicate.Type<PotionsPredicate> POTIONS = register("potion_contents", PotionsPredicate.CODEC);
   public static final DataComponentPredicate.Type<CustomDataPredicate> CUSTOM_DATA = register("custom_data", CustomDataPredicate.CODEC);
   public static final DataComponentPredicate.Type<ContainerPredicate> CONTAINER = register("container", ContainerPredicate.CODEC);
   public static final DataComponentPredicate.Type<BundlePredicate> BUNDLE_CONTENTS = register("bundle_contents", BundlePredicate.CODEC);
   public static final DataComponentPredicate.Type<FireworkExplosionPredicate> FIREWORK_EXPLOSION = register(
      "firework_explosion", FireworkExplosionPredicate.CODEC
   );
   public static final DataComponentPredicate.Type<FireworksPredicate> FIREWORKS = register("fireworks", FireworksPredicate.CODEC);
   public static final DataComponentPredicate.Type<WritableBookPredicate> WRITABLE_BOOK = register("writable_book_content", WritableBookPredicate.CODEC);
   public static final DataComponentPredicate.Type<WrittenBookPredicate> WRITTEN_BOOK = register("written_book_content", WrittenBookPredicate.CODEC);
   public static final DataComponentPredicate.Type<AttributeModifiersPredicate> ATTRIBUTE_MODIFIERS = register(
      "attribute_modifiers", AttributeModifiersPredicate.CODEC
   );
   public static final DataComponentPredicate.Type<TrimPredicate> ARMOR_TRIM = register("trim", TrimPredicate.CODEC);
   public static final DataComponentPredicate.Type<JukeboxPlayablePredicate> JUKEBOX_PLAYABLE = register("jukebox_playable", JukeboxPlayablePredicate.CODEC);
   public static final DataComponentPredicate.Type<VillagerTypePredicate> VILLAGER_VARIANT = register("villager/variant", VillagerTypePredicate.CODEC);

   private static <T extends DataComponentPredicate> DataComponentPredicate.Type<T> register(final String id, final Codec<T> codec) {
      return Registry.register(BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE, id, new DataComponentPredicate.ConcreteType<>(codec));
   }

   public static DataComponentPredicate.Type<?> bootstrap(final Registry<DataComponentPredicate.Type<?>> registry) {
      return DAMAGE;
   }
}
