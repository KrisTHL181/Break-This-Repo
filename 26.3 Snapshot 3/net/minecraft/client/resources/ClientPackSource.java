package net.minecraft.client.resources;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jspecify.annotations.Nullable;

public class ClientPackSource extends BuiltInPackSource {
   private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
      Component.translatable("resourcePack.vanilla.description"), SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).minorRange()
   );
   private static final ResourceMetadata BUILT_IN_METADATA = ResourceMetadata.of(PackMetadataSection.CLIENT_TYPE, VERSION_METADATA_SECTION);
   public static final String HIGH_CONTRAST_PACK = "high_contrast";
   private static final Map<String, Component> SPECIAL_PACK_NAMES = Map.of(
      "programmer_art", Component.translatable("resourcePack.programmer_art.name"), "high_contrast", Component.translatable("resourcePack.high_contrast.name")
   );
   private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo(
      "vanilla", Component.translatable("resourcePack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO)
   );
   private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
   private static final PackSelectionConfig BUILT_IN_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   private static final Identifier PACKS_DIR = Identifier.withDefaultNamespace("resourcepacks");
   private final @Nullable Path externalAssetDir;

   public ClientPackSource(final Path externalAssetSource, final DirectoryValidator validator) {
      super(PackType.CLIENT_RESOURCES, createVanillaPackSource(externalAssetSource), PACKS_DIR, validator);
      this.externalAssetDir = this.findExplodedAssetPacks(externalAssetSource);
   }

   private static PackLocationInfo createBuiltInPackLocation(final String id, final Component title) {
      return new PackLocationInfo(id, title, PackSource.BUILT_IN, Optional.of(KnownPack.vanilla(id)));
   }

   private @Nullable Path findExplodedAssetPacks(final Path externalAssetSource) {
      if (SharedConstants.IS_RUNNING_IN_IDE && externalAssetSource.getFileSystem() == FileSystems.getDefault()) {
         Path devAssetDir = externalAssetSource.getParent().resolve("resourcepacks");
         if (Files.isDirectory(devAssetDir)) {
            return devAssetDir;
         }
      }

      return null;
   }

   private static VanillaPackResources createVanillaPackSource(final Path externalAssetRoot) {
      return new VanillaPackResourcesBuilder()
         .setMetadata(BUILT_IN_METADATA)
         .exposeNamespace("minecraft", "realms")
         .applyDevelopmentConfig()
         .pushJarResources()
         .pushLayer()
         .pushAssetPath(PackType.CLIENT_RESOURCES, externalAssetRoot)
         .build(VANILLA_PACK_INFO);
   }

   @Override
   protected Component getPackTitle(final String id) {
      Component title = SPECIAL_PACK_NAMES.get(id);
      return title != null ? title : Component.literal(id);
   }

   @Override
   protected @Nullable Pack createVanillaPack(final Pack.ResourcesSupplier resources) {
      return Pack.readMetaAndCreate(VANILLA_PACK_INFO, resources, PackType.CLIENT_RESOURCES, VANILLA_SELECTION_CONFIG);
   }

   @Override
   protected @Nullable Pack createBuiltinPack(final String id, final Pack.ResourcesSupplier resources, final Component name) {
      return Pack.readMetaAndCreate(createBuiltInPackLocation(id, name), resources, PackType.CLIENT_RESOURCES, BUILT_IN_SELECTION_CONFIG);
   }

   @Override
   protected void populatePackList(final BiConsumer<String, Function<String, Pack>> discoveredPacks) {
      super.populatePackList(discoveredPacks);
      if (this.externalAssetDir != null) {
         this.discoverPacksInPath(this.externalAssetDir, discoveredPacks);
      }
   }
}
