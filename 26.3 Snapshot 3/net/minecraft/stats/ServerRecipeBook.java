package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.slf4j.Logger;

public class ServerRecipeBook extends RecipeBook {
   public static final String RECIPE_BOOK_TAG = "recipeBook";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerRecipeBook.DisplayResolver displayResolver;
   @VisibleForTesting
   protected final Set<ResourceKey<Recipe<?>>> known = Sets.newIdentityHashSet();
   @VisibleForTesting
   protected final Set<ResourceKey<Recipe<?>>> highlight = Sets.newIdentityHashSet();

   public ServerRecipeBook(final ServerRecipeBook.DisplayResolver displayResolver) {
      this.displayResolver = displayResolver;
   }

   public void add(final ResourceKey<Recipe<?>> id) {
      this.known.add(id);
   }

   public boolean contains(final ResourceKey<Recipe<?>> id) {
      return this.known.contains(id);
   }

   public void remove(final ResourceKey<Recipe<?>> id) {
      this.known.remove(id);
      this.highlight.remove(id);
   }

   public void removeHighlight(final ResourceKey<Recipe<?>> id) {
      this.highlight.remove(id);
   }

   private void addHighlight(final ResourceKey<Recipe<?>> id) {
      this.highlight.add(id);
   }

   public int addRecipes(final Collection<RecipeHolder<?>> recipes, final ServerPlayer player) {
      List<ClientboundRecipeBookAddPacket.Entry> recipesToAdd = new ArrayList<>();

      for (RecipeHolder<?> recipe : recipes) {
         ResourceKey<Recipe<?>> id = recipe.id();
         if (!this.known.contains(id) && !recipe.value().isSpecial()) {
            this.add(id);
            this.addHighlight(id);
            this.displayResolver
               .displaysForRecipe(id, display -> recipesToAdd.add(new ClientboundRecipeBookAddPacket.Entry(display, recipe.value().showNotification(), true)));
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipe);
         }
      }

      if (!recipesToAdd.isEmpty()) {
         player.connection.send(new ClientboundRecipeBookAddPacket(recipesToAdd, false));
      }

      return recipesToAdd.size();
   }

   public int removeRecipes(final Collection<RecipeHolder<?>> recipes, final ServerPlayer player) {
      List<RecipeDisplayId> recipesToRemove = Lists.newArrayList();

      for (RecipeHolder<?> recipe : recipes) {
         ResourceKey<Recipe<?>> id = recipe.id();
         if (this.known.contains(id)) {
            this.remove(id);
            this.displayResolver.displaysForRecipe(id, display -> recipesToRemove.add(display.id()));
         }
      }

      if (!recipesToRemove.isEmpty()) {
         player.connection.send(new ClientboundRecipeBookRemovePacket(recipesToRemove));
      }

      return recipesToRemove.size();
   }

   private void loadRecipes(
      final List<ResourceKey<Recipe<?>>> recipes, final Consumer<ResourceKey<Recipe<?>>> recipeAddingMethod, final Predicate<ResourceKey<Recipe<?>>> validator
   ) {
      for (ResourceKey<Recipe<?>> recipe : recipes) {
         if (!validator.test(recipe)) {
            LOGGER.error("Tried to load unrecognized recipe: {} removed now.", recipe);
         } else {
            recipeAddingMethod.accept(recipe);
         }
      }
   }

   public void sendInitialRecipeBook(final ServerPlayer player) {
      player.connection.send(new ClientboundRecipeBookSettingsPacket(this.getBookSettings().copy()));
      List<ClientboundRecipeBookAddPacket.Entry> recipesToSend = new ArrayList<>(this.known.size());

      for (ResourceKey<Recipe<?>> id : this.known) {
         this.displayResolver.displaysForRecipe(id, r -> recipesToSend.add(new ClientboundRecipeBookAddPacket.Entry(r, false, this.highlight.contains(id))));
      }

      player.connection.send(new ClientboundRecipeBookAddPacket(recipesToSend, true));
   }

   public void copyOverData(final ServerRecipeBook bookToCopy) {
      this.apply(bookToCopy.pack());
   }

   public ServerRecipeBook.Packed pack() {
      return new ServerRecipeBook.Packed(this.bookSettings.copy(), List.copyOf(this.known), List.copyOf(this.highlight));
   }

   private void apply(final ServerRecipeBook.Packed packed) {
      this.known.clear();
      this.highlight.clear();
      this.bookSettings.replaceFrom(packed.settings);
      this.known.addAll(packed.known);
      this.highlight.addAll(packed.highlight);
   }

   public void loadUntrusted(final ServerRecipeBook.Packed packed, final Predicate<ResourceKey<Recipe<?>>> validator) {
      this.bookSettings.replaceFrom(packed.settings);
      this.loadRecipes(packed.known, this.known::add, validator);
      this.loadRecipes(packed.highlight, this.highlight::add, validator);
   }

   @FunctionalInterface
   public interface DisplayResolver {
      void displaysForRecipe(ResourceKey<Recipe<?>> id, Consumer<RecipeDisplayEntry> output);
   }

   public record Packed(RecipeBookSettings settings, List<ResourceKey<Recipe<?>>> known, List<ResourceKey<Recipe<?>>> highlight) {
      public static final Codec<ServerRecipeBook.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               RecipeBookSettings.MAP_CODEC.forGetter(ServerRecipeBook.Packed::settings),
               Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(ServerRecipeBook.Packed::known),
               Recipe.KEY_CODEC.listOf().fieldOf("toBeDisplayed").forGetter(ServerRecipeBook.Packed::highlight)
            )
            .apply(i, ServerRecipeBook.Packed::new)
      );
   }
}
