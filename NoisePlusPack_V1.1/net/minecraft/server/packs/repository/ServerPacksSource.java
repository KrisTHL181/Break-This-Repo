package net.minecraft.server.packs.repository;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.BuiltInMetadata;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
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
   private static final BuiltInMetadata BUILT_IN_METADATA = BuiltInMetadata.of(
      PackMetadataSection.SERVER_TYPE, VERSION_METADATA_SECTION, FeatureFlagsMetadataSection.TYPE, FEATURE_FLAGS_METADATA_SECTION
   );
   private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo(
      "vanilla", Component.translatable("dataPack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO)
   );
   private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.BOTTOM, false);
   private static final PackSelectionConfig FEATURE_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   private static final Identifier PACKS_DIR = Identifier.withDefaultNamespace("datapacks");

   public ServerPacksSource(DirectoryValidator p_300750_) {
      super(PackType.SERVER_DATA, createVanillaPackSource(), PACKS_DIR, p_300750_);
   }

   private static PackLocationInfo createBuiltInPackLocation(String p_330867_, Component p_330785_) {
      return new PackLocationInfo(p_330867_, p_330785_, PackSource.FEATURE, Optional.of(KnownPack.vanilla(p_330867_)));
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
   protected Component getPackTitle(String p_249692_) {
      return Component.literal(p_249692_);
   }

   @Override
   protected @Nullable Pack createVanillaPack(PackResources p_250283_) {
      return Pack.readMetaAndCreate(VANILLA_PACK_INFO, fixedResources(p_250283_), PackType.SERVER_DATA, VANILLA_SELECTION_CONFIG);
   }

   @Override
   protected @Nullable Pack createBuiltinPack(String p_250596_, Pack.ResourcesSupplier p_249625_, Component p_249043_) {
      return Pack.readMetaAndCreate(createBuiltInPackLocation(p_250596_, p_249043_), p_249625_, PackType.SERVER_DATA, FEATURE_SELECTION_CONFIG);
   }

   public static PackRepository createPackRepository(Path p_251569_, DirectoryValidator p_300268_) {
      return new PackRepository(new ServerPacksSource(p_300268_), new FolderRepositorySource(p_251569_, PackType.SERVER_DATA, PackSource.WORLD, p_300268_));
   }

   public static PackRepository createVanillaTrustedRepository() {
      return new PackRepository(new ServerPacksSource(new DirectoryValidator(p_296600_ -> true)));
   }

   public static PackRepository createPackRepository(LevelStorageSource.LevelStorageAccess p_250213_) {
      return createPackRepository(p_250213_.getLevelPath(LevelResource.DATAPACK_DIR), p_250213_.parent().getWorldDirValidator());
   }
}
