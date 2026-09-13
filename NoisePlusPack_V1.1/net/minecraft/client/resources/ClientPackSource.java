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
import net.minecraft.server.packs.BuiltInMetadata;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ClientPackSource extends BuiltInPackSource {
   private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
      Component.translatable("resourcePack.vanilla.description"), SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES).minorRange()
   );
   private static final BuiltInMetadata BUILT_IN_METADATA = BuiltInMetadata.of(PackMetadataSection.CLIENT_TYPE, VERSION_METADATA_SECTION);
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

   public ClientPackSource(Path p_249324_, DirectoryValidator p_299963_) {
      super(PackType.CLIENT_RESOURCES, createVanillaPackSource(p_249324_), PACKS_DIR, p_299963_);
      this.externalAssetDir = this.findExplodedAssetPacks(p_249324_);
   }

   private static PackLocationInfo createBuiltInPackLocation(String p_331520_, Component p_335955_) {
      return new PackLocationInfo(p_331520_, p_335955_, PackSource.BUILT_IN, Optional.of(KnownPack.vanilla(p_331520_)));
   }

   private @Nullable Path findExplodedAssetPacks(Path p_251339_) {
      if (SharedConstants.IS_RUNNING_IN_IDE && p_251339_.getFileSystem() == FileSystems.getDefault()) {
         Path path = p_251339_.getParent().resolve("resourcepacks");
         if (Files.isDirectory(path)) {
            return path;
         }
      }

      return null;
   }

   private static VanillaPackResources createVanillaPackSource(Path p_250749_) {
      VanillaPackResourcesBuilder vanillapackresourcesbuilder = new VanillaPackResourcesBuilder()
         .setMetadata(BUILT_IN_METADATA)
         .exposeNamespace("minecraft", "realms");
      return vanillapackresourcesbuilder.applyDevelopmentConfig()
         .pushJarResources()
         .pushAssetPath(PackType.CLIENT_RESOURCES, p_250749_)
         .build(VANILLA_PACK_INFO);
   }

   @Override
   protected Component getPackTitle(String p_250421_) {
      Component component = SPECIAL_PACK_NAMES.get(p_250421_);
      return component != null ? component : Component.literal(p_250421_);
   }

   @Override
   protected @Nullable Pack createVanillaPack(PackResources p_250048_) {
      return Pack.readMetaAndCreate(VANILLA_PACK_INFO, fixedResources(p_250048_), PackType.CLIENT_RESOURCES, VANILLA_SELECTION_CONFIG);
   }

   @Override
   protected @Nullable Pack createBuiltinPack(String p_250992_, Pack.ResourcesSupplier p_250814_, Component p_249835_) {
      return Pack.readMetaAndCreate(createBuiltInPackLocation(p_250992_, p_249835_), p_250814_, PackType.CLIENT_RESOURCES, BUILT_IN_SELECTION_CONFIG);
   }

   @Override
   protected void populatePackList(BiConsumer<String, Function<String, Pack>> p_249851_) {
      super.populatePackList(p_249851_);
      if (this.externalAssetDir != null) {
         this.discoverPacksInPath(this.externalAssetDir, p_249851_);
      }
   }
}
