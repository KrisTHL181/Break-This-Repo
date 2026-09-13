package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.info.BiomeParametersDumpReport;
import net.minecraft.data.info.BlockListReport;
import net.minecraft.data.info.CommandsReport;
import net.minecraft.data.info.DatapackStructureReport;
import net.minecraft.data.info.PacketReport;
import net.minecraft.data.info.RegistryComponentsReport;
import net.minecraft.data.info.RegistryDumpReport;
import net.minecraft.data.loot.packs.TradeRebalanceLootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.TradeRebalanceRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.DialogTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FeatureTagsProvider;
import net.minecraft.data.tags.FlatLevelGeneratorPresetTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.GameEventTagsProvider;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.data.tags.PotionTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.TimelineTagsProvider;
import net.minecraft.data.tags.TradeRebalanceEnchantmentTagsProvider;
import net.minecraft.data.tags.TradeRebalanceTradeTagsProvider;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.data.tags.VanillaEnchantmentTagsProvider;
import net.minecraft.data.tags.VanillaItemTagsProvider;
import net.minecraft.data.tags.VillagerTradesTagsProvider;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.dataprovider.JsonRpcApiSchema;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.structure.Structure;

public class Main {
    @SuppressForbidden(reason = "System.out needed before bootstrap")
    public static void main(final String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser parser = new OptionParser();
        OptionSpec<Void> helpOption = parser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> serverOption = parser.accepts("server", "Include server generators");
        OptionSpec<Void> devOption = parser.accepts("dev", "Include development tools");
        OptionSpec<Void> reportsOption = parser.accepts("reports", "Include data reports");
        parser.accepts("validate", "Validate inputs");
        OptionSpec<Void> allOption = parser.accepts("all", "Include all generators");
        OptionSpec<String> outputOption = parser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSpec<String> inputOption = parser.accepts("input", "Input folder").withRequiredArg();
        OptionSet optionSet = parser.parse(args);
        if (!optionSet.has(helpOption) && optionSet.hasOptions()) {
            Path output = Paths.get(outputOption.value(optionSet));
            boolean allOptions = optionSet.has(allOption);
            boolean server = allOptions || optionSet.has(serverOption);
            boolean dev = allOptions || optionSet.has(devOption);
            boolean reports = allOptions || optionSet.has(reportsOption);
            Collection<Path> input = optionSet.valuesOf(inputOption).stream().map(x$0 -> Paths.get(x$0)).toList();
            DataGenerator generator = new DataGenerator.Cached(output, SharedConstants.getCurrentVersion(), true);
            addServerDefinitionProviders(generator, server, reports);
            addServerConverters(generator, input, server, dev);
            generator.run();
            Util.shutdownExecutors();
        } else {
            parser.printHelpOn(System.out);
        }
    }

    private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(
        final BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> target, final CompletableFuture<HolderLookup.Provider> registries
    ) {
        return output -> target.apply(output, registries);
    }

    public static void addServerConverters(final DataGenerator generator, final Collection<Path> input, final boolean server, final boolean dev) {
        DataGenerator.PackGenerator commonVanillaPack = generator.getVanillaPack(server);
        commonVanillaPack.addProvider(o -> new SnbtToNbt(o, input).addFilter(new StructureUpdater()));
        DataGenerator.PackGenerator devVanillaPack = generator.getVanillaPack(dev);
        devVanillaPack.addProvider(o -> new NbtToSnbt(o, input));
    }

    public static void addServerDefinitionProviders(final DataGenerator generator, final boolean server, final boolean reports) {
        CompletableFuture<HolderLookup.Provider> vanillaRegistries = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        DataGenerator.PackGenerator serverVanillaPack = generator.getVanillaPack(server);
        serverVanillaPack.addProvider(bindRegistries(RegistriesDatapackGenerator::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(VanillaAdvancementProvider::create, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(VanillaLootTableProvider::create, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(VanillaRecipeProvider.Runner::new, vanillaRegistries));
        TagsProvider<Block> vanillaBlockTagsProvider = serverVanillaPack.addProvider(bindRegistries(VanillaBlockTagsProvider::new, vanillaRegistries));
        TagsProvider<Item> vanillaItemTagsProvider = serverVanillaPack.addProvider(bindRegistries(VanillaItemTagsProvider::new, vanillaRegistries));
        TagsProvider<Biome> vanillaBiomeTagsProvider = serverVanillaPack.addProvider(bindRegistries(BiomeTagsProvider::new, vanillaRegistries));
        TagsProvider<BannerPattern> vanillaBannerPatternTagsProvider = serverVanillaPack.addProvider(
            bindRegistries(BannerPatternTagsProvider::new, vanillaRegistries)
        );
        TagsProvider<Structure> vanillaStructureTagsProvider = serverVanillaPack.addProvider(bindRegistries(StructureTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(DamageTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(DialogTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(EntityTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(FlatLevelGeneratorPresetTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(FluidTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(GameEventTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(InstrumentTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(PaintingVariantTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(PoiTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(WorldPresetTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(VanillaEnchantmentTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(TimelineTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(PotionTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(VillagerTradesTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(FeatureTagsProvider::new, vanillaRegistries));
        serverVanillaPack = generator.getVanillaPack(reports);
        serverVanillaPack.addProvider(bindRegistries(BiomeParametersDumpReport::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(RegistryComponentsReport::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(BlockListReport::new, vanillaRegistries));
        serverVanillaPack.addProvider(bindRegistries(CommandsReport::new, vanillaRegistries));
        serverVanillaPack.addProvider(RegistryDumpReport::new);
        serverVanillaPack.addProvider(PacketReport::new);
        serverVanillaPack.addProvider(DatapackStructureReport::new);
        serverVanillaPack.addProvider(JsonRpcApiSchema::new);
        CompletableFuture<RegistrySetBuilder.PatchedRegistries> tradeRebalanceRegistries = TradeRebalanceRegistries.createLookup(vanillaRegistries);
        CompletableFuture<HolderLookup.Provider> patchedRegistrySet = tradeRebalanceRegistries.thenApply(RegistrySetBuilder.PatchedRegistries::patches);
        DataGenerator.PackGenerator tradeRebalancePack = generator.getBuiltinDatapack(server, "trade_rebalance");
        tradeRebalancePack.addProvider(bindRegistries(RegistriesDatapackGenerator::new, patchedRegistrySet));
        tradeRebalancePack.addProvider(
            o -> PackMetadataGenerator.forFeaturePack(
                o, Component.translatable("dataPack.trade_rebalance.description"), FeatureFlagSet.of(FeatureFlags.TRADE_REBALANCE)
            )
        );
        CompletableFuture<HolderLookup.Provider> patchedRegistries = tradeRebalanceRegistries.thenApply(RegistrySetBuilder.PatchedRegistries::full);
        tradeRebalancePack.addProvider(bindRegistries(TradeRebalanceLootTableProvider::create, patchedRegistries));
        tradeRebalancePack.addProvider(bindRegistries(TradeRebalanceEnchantmentTagsProvider::new, patchedRegistries));
        tradeRebalancePack.addProvider(bindRegistries(TradeRebalanceTradeTagsProvider::new, patchedRegistries));
        serverVanillaPack = generator.getBuiltinDatapack(server, "redstone_experiments");
        serverVanillaPack.addProvider(
            o -> PackMetadataGenerator.forFeaturePack(
                o, Component.translatable("dataPack.redstone_experiments.description"), FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS)
            )
        );
        serverVanillaPack = generator.getBuiltinDatapack(server, "minecart_improvements");
        serverVanillaPack.addProvider(
            o -> PackMetadataGenerator.forFeaturePack(
                o, Component.translatable("dataPack.minecart_improvements.description"), FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
            )
        );
    }
}