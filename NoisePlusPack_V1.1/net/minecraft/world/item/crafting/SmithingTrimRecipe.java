package net.minecraft.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jspecify.annotations.Nullable;

public class SmithingTrimRecipe implements SmithingRecipe {
   final Ingredient template;
   final Ingredient base;
   final Ingredient addition;
   final Holder<TrimPattern> pattern;
   private @Nullable PlacementInfo placementInfo;

   public SmithingTrimRecipe(Ingredient p_391450_, Ingredient p_397854_, Ingredient p_395856_, Holder<TrimPattern> p_394316_) {
      this.template = p_391450_;
      this.base = p_397854_;
      this.addition = p_395856_;
      this.pattern = p_394316_;
   }

   public ItemStack assemble(SmithingRecipeInput p_344440_, HolderLookup.Provider p_330268_) {
      return applyTrim(p_330268_, p_344440_.base(), p_344440_.addition(), this.pattern);
   }

   public static ItemStack applyTrim(HolderLookup.Provider p_369231_, ItemStack p_368958_, ItemStack p_366218_, Holder<TrimPattern> p_393023_) {
      Optional<Holder<TrimMaterial>> optional = TrimMaterials.getFromIngredient(p_369231_, p_366218_);
      if (optional.isPresent()) {
         ArmorTrim armortrim = p_368958_.get(DataComponents.TRIM);
         ArmorTrim armortrim1 = new ArmorTrim(optional.get(), p_393023_);
         if (Objects.equals(armortrim, armortrim1)) {
            return ItemStack.EMPTY;
         }

         ItemStack itemstack = p_368958_.copyWithCount(1);
         itemstack.set(DataComponents.TRIM, armortrim1);
         return itemstack;
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
      return RecipeSerializer.SMITHING_TRIM;
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.create(List.of(this.template, this.base, this.addition));
      }

      return this.placementInfo;
   }

   @Override
   public List<RecipeDisplay> display() {
      SlotDisplay slotdisplay = this.base.display();
      SlotDisplay slotdisplay1 = this.addition.display();
      SlotDisplay slotdisplay2 = this.template.display();
      return List.of(
         new SmithingRecipeDisplay(
            slotdisplay2,
            slotdisplay,
            slotdisplay1,
            new SlotDisplay.SmithingTrimDemoSlotDisplay(slotdisplay, slotdisplay1, this.pattern),
            new SlotDisplay.ItemSlotDisplay(Items.SMITHING_TABLE)
         )
      );
   }

   public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {
      private static final MapCodec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.mapCodec(
         p_390837_ -> p_390837_.group(
               Ingredient.CODEC.fieldOf("template").forGetter(p_390832_ -> p_390832_.template),
               Ingredient.CODEC.fieldOf("base").forGetter(p_390830_ -> p_390830_.base),
               Ingredient.CODEC.fieldOf("addition").forGetter(p_390835_ -> p_390835_.addition),
               TrimPattern.CODEC.fieldOf("pattern").forGetter(p_390833_ -> p_390833_.pattern)
            )
            .apply(p_390837_, SmithingTrimRecipe::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> STREAM_CODEC = StreamCodec.composite(
         Ingredient.CONTENTS_STREAM_CODEC,
         p_390834_ -> p_390834_.template,
         Ingredient.CONTENTS_STREAM_CODEC,
         p_390838_ -> p_390838_.base,
         Ingredient.CONTENTS_STREAM_CODEC,
         p_390831_ -> p_390831_.addition,
         TrimPattern.STREAM_CODEC,
         p_390836_ -> p_390836_.pattern,
         SmithingTrimRecipe::new
      );

      @Override
      public MapCodec<SmithingTrimRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, SmithingTrimRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
