package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record PotionIngredient(Ingredient ingredient, Optional<PotionsPredicate> potions) implements Predicate<ItemStack> {
   public static final MapCodec<PotionIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Ingredient.CODEC.fieldOf("item").forGetter(o -> o.ingredient), PotionsPredicate.CODEC.optionalFieldOf("potion_contents").forGetter(o -> o.potions)
         )
         .apply(i, PotionIngredient::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PotionIngredient> STREAM_CODEC = StreamCodec.composite(
      Ingredient.CONTENTS_STREAM_CODEC, o -> o.ingredient, ByteBufCodecs.optional(PotionsPredicate.STREAM_CODEC), o -> o.potions, PotionIngredient::new
   );

   public boolean test(final ItemStack input) {
      return !this.ingredient.test(input) ? false : this.potions.isEmpty() || this.potions.get().matches(input);
   }

   public static PotionIngredient of(final Item item, final PotionsPredicate potions) {
      return new PotionIngredient(Ingredient.of(item), Optional.of(potions));
   }

   public static PotionIngredient of(final Item item) {
      return new PotionIngredient(Ingredient.of(item), Optional.empty());
   }
}
