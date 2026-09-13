package net.minecraft.server.packs.repository;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jspecify.annotations.Nullable;

public class ServerPacksSource extends BuiltInPackSource {
    private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
        Component.translatable("dataPack.vanilla.description"), SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).minorRange()
    );
    private static final FeatureFlagsMetadataSection FEATURE_FLAGS_METADATA_SECTION = new FeatureFlagsMetadataSection(FeatureFlags.DEFAULT_FLAGS);
    private static final ResourceMetadata BUILT_IN_METADATA = ResourceMetadata.of(
        PackMetadataSection.SERVER_TYPE, VERSION_METADATA_SECTION, FeatureFlagsMetadataSection.TYPE, FEATURE_FLAGS_METADATA_SECTION
    );
    private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo(
        "vanilla", Component.translatable("dataPack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO)
    );
    private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.BOTTOM, false);
    private static final PackSelectionConfig FEATURE_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private static final Identifier PACKS_DIR = Identifier.withDefaultNamespace("datapacks");

    public ServerPacksSource(final DirectoryValidator validator) {
        super(PackType.SERVER_DATA, createVanillaPackSource(), PACKS_DIR, validator);
    }

    private static PackLocationInfo createBuiltInPackLocation(final String id, final Component title) {
        return new PackLocationInfo(id, title, PackSource.FEATURE, Optional.of(KnownPack.vanilla(id)));
    }

    @VisibleForTesting
    public static VanillaPackResources createVanillaPackSource() {
        return new VanillaPackResourcesBuilder()
            .setMetadata(BUILT_IN_METADATA)
            .exposeNamespace("minecraft")
            .applyDevelopmentConfig()
            .pushJarResources()
            .build(VANILLA_PACK_INFO);
    }

    @Override
    protected Component getPackTitle(final String id) {
        return Component.literal(id);
    }

    @Override
    protected @Nullable Pack createVanillaPack(final PackResources resources) {
        return Pack.readMetaAndCreate(VANILLA_PACK_INFO, fixedResources(resources), PackType.SERVER_DATA, VANILLA_SELECTION_CONFIG);
    }

    @Override
    protected @Nullable Pack createBuiltinPack(final String id, final Pack.ResourcesSupplier resources, final Component name) {
        return Pack.readMetaAndCreate(createBuiltInPackLocation(id, name), resources, PackType.SERVER_DATA, FEATURE_SELECTION_CONFIG);
    }

    public static PackRepository createPackRepository(final Path datapackDir, final DirectoryValidator validator) {
        return new PackRepository(new ServerPacksSource(validator), new FolderRepositorySource(datapackDir, PackType.SERVER_DATA, PackSource.WORLD, validator));
    }

    public static PackRepository createVanillaTrustedRepository() {
        return new PackRepository(new ServerPacksSource(new DirectoryValidator(path -> true)));
    }

    public static PackRepository createPackRepository(final LevelStorageSource.LevelStorageAccess levelSourceAccess) {
        return createPackRepository(levelSourceAccess.getLevelPath(LevelResource.DATAPACK_DIR), levelSourceAccess.parent().getWorldDirValidator());
    }
}