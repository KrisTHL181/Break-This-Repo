package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ShapedRecipe implements CraftingRecipe {
   final ShapedRecipePattern pattern;
   final ItemStack result;
   final String group;
   final CraftingBookCategory category;
   final boolean showNotification;
   private @Nullable PlacementInfo placementInfo;

   public ShapedRecipe(String p_250221_, CraftingBookCategory p_250716_, ShapedRecipePattern p_312200_, ItemStack p_248581_, boolean p_310619_) {
      this.group = p_250221_;
      this.category = p_250716_;
      this.pattern = p_312200_;
      this.result = p_248581_;
      this.showNotification = p_310619_;
   }

   public ShapedRecipe(String p_272759_, CraftingBookCategory p_273506_, ShapedRecipePattern p_310709_, ItemStack p_272852_) {
      this(p_272759_, p_273506_, p_310709_, p_272852_, true);
   }

   @Override
   public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
      return RecipeSerializer.SHAPED_RECIPE;
   }

   @Override
   public String group() {
      return this.group;
   }

   @Override
   public CraftingBookCategory category() {
      return this.category;
   }

   @VisibleForTesting
   public List<Optional<Ingredient>> getIngredients() {
      return this.pattern.ingredients();
   }

   @Override
   public PlacementInfo placementInfo() {
      if (this.placementInfo == null) {
         this.placementInfo = PlacementInfo.createFromOptionals(this.pattern.ingredients());
      }

      return this.placementInfo;
   }

   @Override
   public boolean showNotification() {
      return this.showNotification;
   }

   public boolean matches(CraftingInput p_345171_, Level p_44177_) {
      return this.pattern.matches(p_345171_);
   }

   public ItemStack assemble(CraftingInput p_345083_, HolderLookup.Provider p_333236_) {
      return this.result.copy();
   }

   public int getWidth() {
      return this.pattern.width();
   }

   public int getHeight() {
      return this.pattern.height();
   }

   @Override
   public List<RecipeDisplay> display() {
      return List.of(
         new ShapedCraftingRecipeDisplay(
            this.pattern.width(),
            this.pattern.height(),
            this.pattern.ingredients().stream().map(p_359851_ -> p_359851_.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
            new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
         )
      );
   }

   public static class Serializer implements RecipeSerializer<ShapedRecipe> {
      public static final MapCodec<ShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(
         p_327208_ -> p_327208_.group(
               Codec.STRING.optionalFieldOf("group", "").forGetter(p_309251_ -> p_309251_.group),
               CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(p_309253_ -> p_309253_.category),
               ShapedRecipePattern.MAP_CODEC.forGetter(p_309254_ -> p_309254_.pattern),
               ItemStack.STRICT_CODEC.fieldOf("result").forGetter(p_309252_ -> p_309252_.result),
               Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(p_309255_ -> p_309255_.showNotification)
            )
            .apply(p_327208_, ShapedRecipe::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> STREAM_CODEC = StreamCodec.of(
         ShapedRecipe.Serializer::toNetwork, ShapedRecipe.Serializer::fromNetwork
      );

      @Override
      public MapCodec<ShapedRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, ShapedRecipe> streamCodec() {
         return STREAM_CODEC;
      }

      private static ShapedRecipe fromNetwork(RegistryFriendlyByteBuf p_335571_) {
         String s = p_335571_.readUtf();
         CraftingBookCategory craftingbookcategory = p_335571_.readEnum(CraftingBookCategory.class);
         ShapedRecipePattern shapedrecipepattern = ShapedRecipePattern.STREAM_CODEC.decode(p_335571_);
         ItemStack itemstack = ItemStack.STREAM_CODEC.decode(p_335571_);
         boolean flag = p_335571_.readBoolean();
         return new ShapedRecipe(s, craftingbookcategory, shapedrecipepattern, itemstack, flag);
      }

      private static void toNetwork(RegistryFriendlyByteBuf p_336365_, ShapedRecipe p_330934_) {
         p_336365_.writeUtf(p_330934_.group);
         p_336365_.writeEnum(p_330934_.category);
         ShapedRecipePattern.STREAM_CODEC.encode(p_336365_, p_330934_.pattern);
         ItemStack.STREAM_CODEC.encode(p_336365_, p_330934_.result);
         p_336365_.writeBoolean(p_330934_.showNotification);
      }
   }
}
