package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
   private final ReloadableServerRegistries.Holder fullRegistryHolder;
   private final Commands commands;
   private final RecipeManager recipes;
   private final ServerAdvancementManager advancements;
   private final ServerFunctionLibrary functionLibrary;
   private final List<Registry.PendingTags<?>> postponedTags;

   private ReloadableServerResources(
      LayeredRegistryAccess<RegistryLayer> p_368059_,
      HolderLookup.Provider p_363207_,
      FeatureFlagSet p_250695_,
      Commands.CommandSelection p_206858_,
      List<Registry.PendingTags<?>> p_364269_,
      PermissionSet p_453006_
   ) {
      this.fullRegistryHolder = new ReloadableServerRegistries.Holder(p_368059_.compositeAccess());
      this.postponedTags = p_364269_;
      this.recipes = new RecipeManager(p_363207_);
      this.commands = new Commands(p_206858_, CommandBuildContext.simple(p_363207_, p_250695_));
      this.advancements = new ServerAdvancementManager(p_363207_);
      this.functionLibrary = new ServerFunctionLibrary(p_453006_, this.commands.getDispatcher());
   }

   public ServerFunctionLibrary getFunctionLibrary() {
      return this.functionLibrary;
   }

   public ReloadableServerRegistries.Holder fullRegistries() {
      return this.fullRegistryHolder;
   }

   public RecipeManager getRecipeManager() {
      return this.recipes;
   }

   public Commands getCommands() {
      return this.commands;
   }

   public ServerAdvancementManager getAdvancements() {
      return this.advancements;
   }

   public List<PreparableReloadListener> listeners() {
      return List.of(this.recipes, this.functionLibrary, this.advancements);
   }

   public static CompletableFuture<ReloadableServerResources> loadResources(
      ResourceManager p_248588_,
      LayeredRegistryAccess<RegistryLayer> p_330376_,
      List<Registry.PendingTags<?>> p_366334_,
      FeatureFlagSet p_250212_,
      Commands.CommandSelection p_249301_,
      PermissionSet p_457868_,
      Executor p_249136_,
      Executor p_249601_
   ) {
      return ReloadableServerRegistries.reload(p_330376_, p_366334_, p_248588_, p_249136_)
         .thenCompose(
            p_448843_ -> {
               ReloadableServerResources reloadableserverresources = new ReloadableServerResources(
                  p_448843_.layers(), p_448843_.lookupWithUpdatedTags(), p_250212_, p_249301_, p_366334_, p_457868_
               );
               return SimpleReloadInstance.create(
                     p_248588_, reloadableserverresources.listeners(), p_249136_, p_249601_, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()
                  )
                  .done()
                  .thenApply(p_214306_ -> reloadableserverresources);
            }
         );
   }

   public void updateStaticRegistryTags() {
      this.postponedTags.forEach(Registry.PendingTags::apply);
   }
}
