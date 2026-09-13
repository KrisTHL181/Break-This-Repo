package net.minecraft.client.multiplayer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SessionSearchTrees {
   private static final SessionSearchTrees.Key RECIPE_COLLECTIONS = new SessionSearchTrees.Key();
   private static final SessionSearchTrees.Key CREATIVE_NAMES = new SessionSearchTrees.Key();
   private static final SessionSearchTrees.Key CREATIVE_TAGS = new SessionSearchTrees.Key();
   private CompletableFuture<SearchTree<ItemStack>> creativeByNameSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private CompletableFuture<SearchTree<ItemStack>> creativeByTagSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private CompletableFuture<SearchTree<RecipeCollection>> recipeSearch = CompletableFuture.completedFuture(SearchTree.empty());
   private final Map<SessionSearchTrees.Key, Runnable> reloaders = new IdentityHashMap<>();

   private void register(SessionSearchTrees.Key p_342736_, Runnable p_343077_) {
      p_343077_.run();
      this.reloaders.put(p_342736_, p_343077_);
   }

   public void rebuildAfterLanguageChange() {
      for (Runnable runnable : this.reloaders.values()) {
         runnable.run();
      }
   }

   private static Stream<String> getTooltipLines(Stream<ItemStack> p_344293_, Item.TooltipContext p_343228_, TooltipFlag p_342315_) {
      return p_344293_.<Component>flatMap(p_343071_ -> p_343071_.getTooltipLines(p_343228_, null, p_342315_).stream())
         .map(p_344266_ -> ChatFormatting.stripFormatting(p_344266_.getString()).trim())
         .filter(p_345189_ -> !p_345189_.isEmpty());
   }

   public void updateRecipes(ClientRecipeBook p_343609_, Level p_362281_) {
      this.register(
         RECIPE_COLLECTIONS,
         () -> {
            List<RecipeCollection> list = p_343609_.getCollections();
            RegistryAccess registryaccess = p_362281_.registryAccess();
            Registry<Item> registry = registryaccess.lookupOrThrow(Registries.ITEM);
            Item.TooltipContext item$tooltipcontext = Item.TooltipContext.of(registryaccess);
            ContextMap contextmap = SlotDisplayContext.fromLevel(p_362281_);
            TooltipFlag tooltipflag = TooltipFlag.Default.NORMAL;
            CompletableFuture<?> completablefuture = this.recipeSearch;
            this.recipeSearch = CompletableFuture.supplyAsync(
               () -> new FullTextSearchTree<>(
                  p_357799_ -> getTooltipLines(
                     p_357799_.getRecipes().stream().flatMap(p_357810_ -> p_357810_.resultItems(contextmap).stream()), item$tooltipcontext, tooltipflag
                  ),
                  p_357813_ -> p_357813_.getRecipes()
                     .stream()
                     .flatMap(p_357803_ -> p_357803_.resultItems(contextmap).stream())
                     .map(p_448126_ -> registry.getKey(p_448126_.getItem())),
                  list
               ),
               Util.backgroundExecutor()
            );
            completablefuture.cancel(true);
         }
      );
   }

   public SearchTree<RecipeCollection> recipes() {
      return this.recipeSearch.join();
   }

   public void updateCreativeTags(List<ItemStack> p_344581_) {
      this.register(
         CREATIVE_TAGS,
         () -> {
            CompletableFuture<?> completablefuture = this.creativeByTagSearch;
            this.creativeByTagSearch = CompletableFuture.supplyAsync(
               () -> new IdSearchTree<>(p_342206_ -> p_342206_.getTags().map(TagKey::location), p_344581_), Util.backgroundExecutor()
            );
            completablefuture.cancel(true);
         }
      );
   }

   public SearchTree<ItemStack> creativeTagSearch() {
      return this.creativeByTagSearch.join();
   }

   public void updateCreativeTooltips(HolderLookup.Provider p_343364_, List<ItemStack> p_342500_) {
      this.register(
         CREATIVE_NAMES,
         () -> {
            Item.TooltipContext item$tooltipcontext = Item.TooltipContext.of(p_343364_);
            TooltipFlag tooltipflag = TooltipFlag.Default.NORMAL.asCreative();
            CompletableFuture<?> completablefuture = this.creativeByNameSearch;
            this.creativeByNameSearch = CompletableFuture.supplyAsync(
               () -> new FullTextSearchTree<>(
                  p_345254_ -> getTooltipLines(Stream.of(p_345254_), item$tooltipcontext, tooltipflag),
                  p_344415_ -> p_344415_.getItemHolder().unwrapKey().map(ResourceKey::identifier).stream(),
                  p_342500_
               ),
               Util.backgroundExecutor()
            );
            completablefuture.cancel(true);
         }
      );
   }

   public SearchTree<ItemStack> creativeNameSearch() {
      return this.creativeByNameSearch.join();
   }

   @OnlyIn(Dist.CLIENT)
   static class Key {
   }
}
