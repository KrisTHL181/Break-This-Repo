package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.BuiltInBlockModels;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.LoadedBlockModels;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ModelManager implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private Map<Identifier, ItemModel> bakedItemStackModels = Map.of();
    private Map<Identifier, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasManager atlasManager;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private ModelBakery.MissingModels missingModels;
    private @Nullable BlockStateModelSet blockStateModelSet;
    private @Nullable BlockModelSet blockModelSet;
    private @Nullable FluidStateModelSet fluidStateModelSet;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();

    public ModelManager(final BlockColors blockColors, final AtlasManager atlasManager, final PlayerSkinRenderCache playerSkinRenderCache) {
        this.blockColors = blockColors;
        this.atlasManager = atlasManager;
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    public ItemModel getItemModel(final Identifier id) {
        return this.bakedItemStackModels.getOrDefault(id, this.missingModels.item());
    }

    public ClientItem.Properties getItemProperties(final Identifier id) {
        return this.itemProperties.getOrDefault(id, ClientItem.Properties.DEFAULT);
    }

    public BlockStateModelSet getBlockStateModelSet() {
        return Objects.requireNonNull(this.blockStateModelSet, "Block models not yet initialized");
    }

    public BlockModelSet getBlockModelSet() {
        return Objects.requireNonNull(this.blockModelSet, "Block models not yet initialized");
    }

    public FluidStateModelSet getFluidStateModelSet() {
        return Objects.requireNonNull(this.fluidStateModelSet, "Fluid models not yet initialized");
    }

    @Override
    public final CompletableFuture<Void> reload(
        final PreparableReloadListener.SharedState currentReload,
        final Executor taskExecutor,
        final PreparableReloadListener.PreparationBarrier preparationBarrier,
        final Executor reloadExecutor
    ) {
        ResourceManager manager = currentReload.resourceManager();
        CompletableFuture<EntityModelSet> entityModelSet = CompletableFuture.supplyAsync(EntityModelSet::vanilla, taskExecutor);
        CompletableFuture<Map<Identifier, UnbakedModel>> modelCache = loadBlockModels(manager, taskExecutor);
        CompletableFuture<BlockStateModelLoader.LoadedModels> blockStateModels = BlockStateModelLoader.loadBlockStates(manager, taskExecutor);
        CompletableFuture<Map<BlockState, BlockModel.Unbaked>> blockModelContents = CompletableFuture.supplyAsync(
            () -> BuiltInBlockModels.createBlockModels(this.blockColors), taskExecutor
        );
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> itemStackModels = ClientItemInfoLoader.scheduleLoad(manager, taskExecutor);
        CompletableFuture<ModelManager.ResolvedModels> modelDiscovery = CompletableFuture.allOf(modelCache, blockStateModels, itemStackModels)
            .thenApplyAsync(var3 -> discoverModelDependencies(modelCache.join(), blockStateModels.join(), itemStackModels.join()), taskExecutor);
        CompletableFuture<Object2IntMap<BlockState>> modelGroups = blockStateModels.thenApplyAsync(
            models -> buildModelGroups(this.blockColors, models), taskExecutor
        );
        AtlasManager.PendingStitchResults pendingStitches = currentReload.get(AtlasManager.PENDING_STITCH);
        CompletableFuture<SpriteLoader.Preparations> pendingBlockAtlasSprites = pendingStitches.get(AtlasIds.BLOCKS);
        CompletableFuture<SpriteLoader.Preparations> pendingItemAtlasSprites = pendingStitches.get(AtlasIds.ITEMS);
        CompletableFuture<LoadedBlockModels> blockModels = CompletableFuture.allOf(blockModelContents, entityModelSet)
            .thenApply(var3 -> new LoadedBlockModels(blockModelContents.join(), entityModelSet.join(), this.atlasManager, this.playerSkinRenderCache));
        return CompletableFuture.allOf(
                pendingBlockAtlasSprites,
                pendingItemAtlasSprites,
                modelDiscovery,
                modelGroups,
                blockStateModels,
                itemStackModels,
                entityModelSet,
                blockModels,
                modelCache
            )
            .thenComposeAsync(
                var11x -> {
                    SpriteLoader.Preparations blockAtlasSprites = pendingBlockAtlasSprites.join();
                    SpriteLoader.Preparations itemAtlasSprites = pendingItemAtlasSprites.join();
                    ModelManager.ResolvedModels resolvedModels = modelDiscovery.join();
                    Object2IntMap<BlockState> groups = modelGroups.join();
                    Set<Identifier> unreferencedModels = Sets.difference(modelCache.join().keySet(), resolvedModels.models.keySet());
                    if (!unreferencedModels.isEmpty()) {
                        LOGGER.debug(
                            "Unreferenced models: \n{}",
                            unreferencedModels.stream().sorted().map(modelId -> "\t" + modelId + "\n").collect(Collectors.joining())
                        );
                    }

                    ModelBakery bakery = new ModelBakery(
                        entityModelSet.join(),
                        this.atlasManager,
                        this.playerSkinRenderCache,
                        blockStateModels.join().models(),
                        itemStackModels.join().contents(),
                        resolvedModels.models(),
                        resolvedModels.missing()
                    );
                    return loadModels(blockAtlasSprites, itemAtlasSprites, bakery, blockModels.join(), groups, entityModelSet.join(), taskExecutor);
                },
                taskExecutor
            )
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(this::apply, reloadExecutor);
    }

    private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(final ResourceManager manager, final Executor executor) {
        return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(manager), executor)
            .thenCompose(
                resources -> {
                    List<CompletableFuture<Pair<Identifier, CuboidModel>>> result = new ArrayList<>(resources.size());

                    for (Entry<Identifier, Resource> resource : resources.entrySet()) {
                        result.add(CompletableFuture.supplyAsync(() -> {
                            Identifier modelId = MODEL_LISTER.fileToId(resource.getKey());

                            try (Reader reader = resource.getValue().openAsReader()) {
                                return Pair.of(modelId, CuboidModel.fromStream(reader));
                            } catch (Exception e) {
                                LOGGER.error("Failed to load model {}", resource.getKey(), e);
                                return null;
                            }
                        }, executor));
                    }

                    return Util.sequence(result)
                        .thenApply(pairs -> pairs.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
                }
            );
    }

    private static ModelManager.ResolvedModels discoverModelDependencies(
        final Map<Identifier, UnbakedModel> allModels,
        final BlockStateModelLoader.LoadedModels blockStateModels,
        final ClientItemInfoLoader.LoadedClientInfos itemInfos
    ) {
        try (Zone ignored = Profiler.get().zone("dependencies")) {
            ModelDiscovery result = new ModelDiscovery(allModels, MissingCuboidModel.missingModel());
            result.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
            blockStateModels.models().values().forEach(result::addRoot);
            itemInfos.contents().values().forEach(info -> result.addRoot(info.model()));
            return new ModelManager.ResolvedModels(result.missingModel(), result.resolve());
        }
    }

    private static CompletableFuture<ModelManager.ReloadState> loadModels(
        final SpriteLoader.Preparations blockAtlas,
        final SpriteLoader.Preparations itemAtlas,
        final ModelBakery bakery,
        final LoadedBlockModels blockModels,
        final Object2IntMap<BlockState> modelGroups,
        final EntityModelSet entityModelSet,
        final Executor taskExecutor
    ) {
        MaterialBaker blockItemMaterialBaker = new ModelManager.CombinedBlockItemMaterialBaker(blockAtlas, itemAtlas);
        MaterialBaker blockOnlyMaterialBaker = new ModelManager.BlockOnlyMaterialBaker(blockAtlas);
        CompletableFuture<ModelBakery.BakingResult> bakedStateResults = bakery.bakeModels(blockItemMaterialBaker, taskExecutor);
        CompletableFuture<Map<BlockState, BlockModel>> bakedModelsFuture = bakedStateResults.thenCompose(
            bakingResult -> blockModels.bake(bakingResult::getBlockStateModel, bakingResult.missingModels().block(), taskExecutor)
        );
        return bakedStateResults.thenCombine(
            bakedModelsFuture,
            (bakingResult, bakedModels) -> {
                blockItemMaterialBaker.logMissingTextures();
                Map<Fluid, FluidModel> fluidModels = FluidStateModelSet.bake(blockOnlyMaterialBaker);
                blockOnlyMaterialBaker.logMissingTextures();
                Map<BlockState, BlockStateModel> modelByStateCache = createBlockStateToModelDispatch(
                    bakingResult.blockStateModels(), bakingResult.missingModels().block()
                );
                return new ModelManager.ReloadState(
                    bakingResult, modelGroups, modelByStateCache, (Map<BlockState, BlockModel>)bakedModels, fluidModels, entityModelSet
                );
            }
        );
    }

    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(
        final Map<BlockState, BlockStateModel> bakedModels, final BlockStateModel missingModel
    ) {
        try (Zone ignored = Profiler.get().zone("block state dispatch")) {
            Map<BlockState, BlockStateModel> modelByStateCache = new IdentityHashMap<>(bakedModels);

            for (Block block : BuiltInRegistries.BLOCK) {
                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    if (bakedModels.putIfAbsent(state, missingModel) == null) {
                        LOGGER.warn("Missing model for variant: '{}'", state);
                    }
                });
            }

            return modelByStateCache;
        }
    }

    private static Object2IntMap<BlockState> buildModelGroups(final BlockColors blockColors, final BlockStateModelLoader.LoadedModels blockStateModels) {
        try (Zone ignored = Profiler.get().zone("block groups")) {
            return ModelGroupCollector.build(blockColors, blockStateModels);
        }
    }

    private void apply(final ModelManager.ReloadState preparations) {
        ModelBakery.BakingResult bakedModels = preparations.bakedModels;
        this.bakedItemStackModels = bakedModels.itemStackModels();
        this.itemProperties = bakedModels.itemProperties();
        this.modelGroups = preparations.modelGroups;
        this.missingModels = bakedModels.missingModels();
        this.blockStateModelSet = new BlockStateModelSet(preparations.blockStateModels, this.missingModels.block());
        this.blockModelSet = new BlockModelSet(this.blockStateModelSet, preparations.blockModels, this.blockColors);
        this.fluidStateModelSet = new FluidStateModelSet(preparations.fluidModels, this.missingModels.fluid());
        this.entityModelSet = preparations.entityModelSet;
    }

    public boolean requiresRender(final BlockState oldState, final BlockState newState) {
        if (oldState == newState) {
            return false;
        }

        int oldModelGroup = this.modelGroups.getInt(oldState);
        if (oldModelGroup != -1) {
            int newModelGroup = this.modelGroups.getInt(newState);
            if (oldModelGroup == newModelGroup) {
                FluidState oldFluidState = oldState.getFluidState();
                FluidState newFluidState = newState.getFluidState();
                return oldFluidState != newFluidState;
            }
        }

        return true;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    @OnlyIn(Dist.CLIENT)
    private static class BlockOnlyMaterialBaker extends MaterialBaker {
        private final SpriteLoader.Preparations blockAtlas;

        public BlockOnlyMaterialBaker(final SpriteLoader.Preparations blockAtlas) {
            super(blockAtlas.missing());
            this.blockAtlas = blockAtlas;
        }

        @Override
        protected Material.@Nullable Baked bake(final Material material) {
            return bakeForAtlas(material, this.blockAtlas);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class CombinedBlockItemMaterialBaker extends MaterialBaker {
        private final SpriteLoader.Preparations blockAtlas;
        private final SpriteLoader.Preparations itemAtlas;

        public CombinedBlockItemMaterialBaker(final SpriteLoader.Preparations blockAtlas, final SpriteLoader.Preparations itemAtlas) {
            super(blockAtlas.missing());
            this.blockAtlas = blockAtlas;
            this.itemAtlas = itemAtlas;
        }

        @Override
        protected Material.@Nullable Baked bake(final Material material) {
            Material.Baked itemMaterial = bakeForAtlas(material, this.itemAtlas);
            return itemMaterial != null ? itemMaterial : bakeForAtlas(material, this.blockAtlas);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record ReloadState(
        ModelBakery.BakingResult bakedModels,
        Object2IntMap<BlockState> modelGroups,
        Map<BlockState, BlockStateModel> blockStateModels,
        Map<BlockState, BlockModel> blockModels,
        Map<Fluid, FluidModel> fluidModels,
        EntityModelSet entityModelSet
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    private record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
    }
}