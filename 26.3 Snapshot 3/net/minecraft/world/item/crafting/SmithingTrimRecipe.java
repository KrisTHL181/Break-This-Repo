package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class SmithingTrimRecipe extends SimpleSmithingRecipe {
   public static final MapCodec<SmithingTrimRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            Ingredient.CODEC.fieldOf("template").forGetter(o -> o.template),
            Ingredient.CODEC.fieldOf("base").forGetter(o -> o.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(o -> o.addition),
            TrimPattern.CODEC.fieldOf("pattern").forGetter(o -> o.pattern)
         )
         .apply(i, SmithingTrimRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.composite(
      Recipe.CommonInfo.STREAM_CODEC,
      o -> o.commonInfo,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.template,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.base,
      Ingredient.CONTENTS_STREAM_CODEC,
      o -> o.addition,
      TrimPattern.STREAM_CODEC,
      o -> o.pattern,
      SmithingTrimRecipe::new
   );
   public static final RecipeSerializer<SmithingTrimRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
   private final Ingredient template;
   private final Ingredient base;
   private final Ingredient addition;
   private final Holder<TrimPattern> pattern;

   public SmithingTrimRecipe(
      final Recipe.CommonInfo commonInfo, final Ingredient template, final Ingredient base, final Ingredient addition, final Holder<TrimPattern> pattern
   ) {
      super(commonInfo);
      this.template = template;
      this.base = base;
      this.addition = addition;
      this.pattern = pattern;
   }

   public ItemStack assemble(final SmithingRecipeInput input) {
      return applyTrim(input.base(), input.addition(), this.pattern);
   }

   public static ItemStack applyTrim(final ItemStack baseItem, final ItemStack materialItem, final Holder<TrimPattern> pattern) {
      Holder<TrimMaterial> material = materialItem.get(DataComponents.PROVIDES_TRIM_MATERIAL);
      if (material != null) {
         ArmorTrim existingTrim = baseItem.get(DataComponents.TRIM);
         ArmorTrim newTrim = new ArmorTrim(material, pattern);
         if (Objects.equals(existingTrim, newTrim)) {
            return ItemStack.EMPTY;
         }

         ItemStack trimmedItem = baseItem.copyWithCount(1);
         trimmedItem.set(DataComponents.TRIM, newTrim);
         return trimmedItem;
      } else {
         return ItemStack.EMPTY;
      }
   }

   @Override
   public Optional<Ingredient> templateIngredient() {
      return Optional.of(this.template);
   }

   @Override
   public Ingredient baseIngredient() {
      return this.base;
   }

   @Override
   public Optional<Ingredient> additionIngredient() {
      return Optional.of(this.addition);
   }

   @Override
   public RecipeSerializer<SmithingTrimRecipe> getSerializer() {
      return SERIALIZER;
   }

   @Override
   protected PlacementInfo createPlacementInfo() {
      return PlacementInfo.create(List.of(this.template, this.base, this.addition));
   }

   @Override
   public List<RecipeDisplay> display() {
      SlotDisplay base = this.base.display();
      SlotDisplay material = this.addition.display();
      SlotDisplay template = this.template.display();
      return List.of(
         new SmithingRecipeDisplay(
            template,
            base,
            material,
            new SlotDisplay.SmithingTrimDemoSlotDisplay(base, material, this.pattern),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
         )
      );
   }
}
