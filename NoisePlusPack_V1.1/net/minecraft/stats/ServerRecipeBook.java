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
import net.minecraft.advancements.CriteriaTriggers;
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

   public ServerRecipeBook(ServerRecipeBook.DisplayResolver p_361467_) {
      this.displayResolver = p_361467_;
   }

   public void add(ResourceKey<Recipe<?>> p_369732_) {
      this.known.add(p_369732_);
   }

   public boolean contains(ResourceKey<Recipe<?>> p_360909_) {
      return this.known.contains(p_360909_);
   }

   public void remove(ResourceKey<Recipe<?>> p_366423_) {
      this.known.remove(p_366423_);
      this.highlight.remove(p_366423_);
   }

   public void removeHighlight(ResourceKey<Recipe<?>> p_366458_) {
      this.highlight.remove(p_366458_);
   }

   private void addHighlight(ResourceKey<Recipe<?>> p_365655_) {
      this.highlight.add(p_365655_);
   }

   public int addRecipes(Collection<RecipeHolder<?>> p_12792_, ServerPlayer p_12793_) {
      List<ClientboundRecipeBookAddPacket.Entry> list = new ArrayList<>();

      for (RecipeHolder<?> recipeholder : p_12792_) {
         ResourceKey<Recipe<?>> resourcekey = recipeholder.id();
         if (!this.known.contains(resourcekey) && !recipeholder.value().isSpecial()) {
            this.add(resourcekey);
            this.addHighlight(resourcekey);
            this.displayResolver
               .displaysForRecipe(
                  resourcekey, p_363687_ -> list.add(new ClientboundRecipeBookAddPacket.Entry(p_363687_, recipeholder.value().showNotification(), true))
               );
            CriteriaTriggers.RECIPE_UNLOCKED.trigger(p_12793_, recipeholder);
         }
      }

      if (!list.isEmpty()) {
         p_12793_.connection.send(new ClientboundRecipeBookAddPacket(list, false));
      }

      return list.size();
   }

   public int removeRecipes(Collection<RecipeHolder<?>> p_12807_, ServerPlayer p_12808_) {
      List<RecipeDisplayId> list = Lists.newArrayList();

      for (RecipeHolder<?> recipeholder : p_12807_) {
         ResourceKey<Recipe<?>> resourcekey = recipeholder.id();
         if (this.known.contains(resourcekey)) {
            this.remove(resourcekey);
            this.displayResolver.displaysForRecipe(resourcekey, p_364401_ -> list.add(p_364401_.id()));
         }
      }

      if (!list.isEmpty()) {
         p_12808_.connection.send(new ClientboundRecipeBookRemovePacket(list));
      }

      return list.size();
   }

   private void loadRecipes(List<ResourceKey<Recipe<?>>> p_392652_, Consumer<ResourceKey<Recipe<?>>> p_12799_, Predicate<ResourceKey<Recipe<?>>> p_367349_) {
      for (ResourceKey<Recipe<?>> resourcekey : p_392652_) {
         if (!p_367349_.test(resourcekey)) {
            LOGGER.error("Tried to load unrecognized recipe: {} removed now.", resourcekey);
         } else {
            p_12799_.accept(resourcekey);
         }
      }
   }

   public void sendInitialRecipeBook(ServerPlayer p_12790_) {
      p_12790_.connection.send(new ClientboundRecipeBookSettingsPacket(this.getBookSettings().copy()));
      List<ClientboundRecipeBookAddPacket.Entry> list = new ArrayList<>(this.known.size());

      for (ResourceKey<Recipe<?>> resourcekey : this.known) {
         this.displayResolver
            .displaysForRecipe(
               resourcekey, p_369028_ -> list.add(new ClientboundRecipeBookAddPacket.Entry(p_369028_, false, this.highlight.contains(resourcekey)))
            );
      }

      p_12790_.connection.send(new ClientboundRecipeBookAddPacket(list, true));
   }

   public void copyOverData(ServerRecipeBook p_369276_) {
      this.apply(p_369276_.pack());
   }

   public ServerRecipeBook.Packed pack() {
      return new ServerRecipeBook.Packed(this.bookSettings.copy(), List.copyOf(this.known), List.copyOf(this.highlight));
   }

   private void apply(ServerRecipeBook.Packed p_407380_) {
      this.known.clear();
      this.highlight.clear();
      this.bookSettings.replaceFrom(p_407380_.settings);
      this.known.addAll(p_407380_.known);
      this.highlight.addAll(p_407380_.highlight);
   }

   public void loadUntrusted(ServerRecipeBook.Packed p_406545_, Predicate<ResourceKey<Recipe<?>>> p_406947_) {
      this.bookSettings.replaceFrom(p_406545_.settings);
      this.loadRecipes(p_406545_.known, this.known::add, p_406947_);
      this.loadRecipes(p_406545_.highlight, this.highlight::add, p_406947_);
   }

   @FunctionalInterface
   public interface DisplayResolver {
      void displaysForRecipe(ResourceKey<Recipe<?>> var1, Consumer<RecipeDisplayEntry> var2);
   }

   public record Packed(RecipeBookSettings settings, List<ResourceKey<Recipe<?>>> known, List<ResourceKey<Recipe<?>>> highlight) {
      public static final Codec<ServerRecipeBook.Packed> CODEC = RecordCodecBuilder.create(
         p_408587_ -> p_408587_.group(
               RecipeBookSettings.MAP_CODEC.forGetter(ServerRecipeBook.Packed::settings),
               Recipe.KEY_CODEC.listOf().fieldOf("recipes").forGetter(ServerRecipeBook.Packed::known),
               Recipe.KEY_CODEC.listOf().fieldOf("toBeDisplayed").forGetter(ServerRecipeBook.Packed::highlight)
            )
            .apply(p_408587_, ServerRecipeBook.Packed::new)
      );
   }
}
