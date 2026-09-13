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
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private final List<DataComponentInitializers.PendingComponents<?>> newComponents;

    private ReloadableServerResources(
        final LayeredRegistryAccess<RegistryLayer> fullLayers,
        final HolderLookup.Provider loadingContext,
        final FeatureFlagSet enabledFeatures,
        final Commands.CommandSelection commandSelection,
        final List<Registry.PendingTags<?>> postponedTags,
        final PermissionSet functionCompilationPermissions,
        final List<DataComponentInitializers.PendingComponents<?>> newComponents
    ) {
        this.fullRegistryHolder = new ReloadableServerRegistries.Holder(fullLayers.compositeAccess());
        this.postponedTags = postponedTags;
        this.newComponents = newComponents;
        this.recipes = new RecipeManager(loadingContext);
        this.commands = new Commands(commandSelection, CommandBuildContext.simple(loadingContext, enabledFeatures));
        this.advancements = new ServerAdvancementManager(loadingContext);
        this.functionLibrary = new ServerFunctionLibrary(functionCompilationPermissions, this.commands.getDispatcher());
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
        final ResourceManager resourceManager,
        final LayeredRegistryAccess<RegistryLayer> contextLayers,
        final List<Registry.PendingTags<?>> updatedContextTags,
        final FeatureFlagSet enabledFeatures,
        final Commands.CommandSelection commandSelection,
        final PermissionSet functionCompilationPermissions,
        final Executor backgroundExecutor,
        final Executor mainThreadExecutor
    ) {
        return ReloadableServerRegistries.reload(contextLayers, updatedContextTags, resourceManager, backgroundExecutor)
            .thenCompose(
                fullRegistries -> CompletableFuture.<List<DataComponentInitializers.PendingComponents<?>>>supplyAsync(
                        () -> BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(fullRegistries.lookupWithUpdatedTags()), backgroundExecutor
                    )
                    .thenCompose(
                        pendingComponents -> {
                            ReloadableServerResources result = new ReloadableServerResources(
                                fullRegistries.layers(),
                                fullRegistries.lookupWithUpdatedTags(),
                                enabledFeatures,
                                commandSelection,
                                updatedContextTags,
                                functionCompilationPermissions,
                                (List<DataComponentInitializers.PendingComponents<?>>)pendingComponents
                            );
                            return SimpleReloadInstance.create(
                                    resourceManager,
                                    result.listeners(),
                                    backgroundExecutor,
                                    mainThreadExecutor,
                                    DATA_RELOAD_INITIAL_TASK,
                                    LOGGER.isDebugEnabled()
                                )
                                .done()
                                .thenApply(ignore -> result);
                        }
                    )
            );
    }

    public void updateComponentsAndStaticRegistryTags() {
        this.postponedTags.forEach(Registry.PendingTags::apply);
        this.newComponents.forEach(DataComponentInitializers.PendingComponents::apply);
    }
}