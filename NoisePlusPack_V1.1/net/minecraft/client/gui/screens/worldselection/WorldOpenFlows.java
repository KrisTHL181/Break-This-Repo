package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldOpenFlows {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final UUID WORLD_PACK_ID = UUID.fromString("640a6a92-b6cb-48a0-b391-831586500359");
   private final Minecraft minecraft;
   private final LevelStorageSource levelSource;

   public WorldOpenFlows(Minecraft p_233093_, LevelStorageSource p_233094_) {
      this.minecraft = p_233093_;
      this.levelSource = p_233094_;
   }

   public void createFreshLevel(
      String p_233158_, LevelSettings p_233159_, WorldOptions p_249243_, Function<HolderLookup.Provider, WorldDimensions> p_249252_, Screen p_310233_
   ) {
      this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
      LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(p_233158_);
      if (levelstoragesource$levelstorageaccess != null) {
         PackRepository packrepository = ServerPacksSource.createPackRepository(levelstoragesource$levelstorageaccess);
         WorldDataConfiguration worlddataconfiguration = p_233159_.getDataConfiguration();

         try {
            WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(packrepository, worlddataconfiguration, false, false);
            WorldStem worldstem = this.loadWorldDataBlocking(
               worldloader$packconfig,
               p_389327_ -> {
                  WorldDimensions.Complete worlddimensions$complete = p_249252_.apply(p_389327_.datapackWorldgen())
                     .bake(p_389327_.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
                  return new WorldLoader.DataLoadOutput<>(
                     new PrimaryLevelData(p_233159_, p_249243_, worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()),
                     worlddimensions$complete.dimensionsRegistryAccess()
                  );
               },
               WorldStem::new
            );
            this.minecraft.doWorldLoad(levelstoragesource$levelstorageaccess, packrepository, worldstem, true);
         } catch (Exception exception) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load", exception);
            levelstoragesource$levelstorageaccess.safeClose();
            this.minecraft.setScreen(p_310233_);
         }
      }
   }

   private LevelStorageSource.@Nullable LevelStorageAccess createWorldAccess(String p_233156_) {
      try {
         return this.levelSource.validateAndCreateAccess(p_233156_);
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to read level {} data", p_233156_, ioexception);
         SystemToast.onWorldAccessFailure(this.minecraft, p_233156_);
         this.minecraft.setScreen(null);
         return null;
      } catch (ContentValidationException contentvalidationexception) {
         LOGGER.warn("{}", contentvalidationexception.getMessage());
         this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(null)));
         return null;
      }
   }

   public void createLevelFromExistingSettings(
      LevelStorageSource.LevelStorageAccess p_250919_, ReloadableServerResources p_248897_, LayeredRegistryAccess<RegistryLayer> p_250801_, WorldData p_251654_
   ) {
      PackRepository packrepository = ServerPacksSource.createPackRepository(p_250919_);
      CloseableResourceManager closeableresourcemanager = (CloseableResourceManager)new WorldLoader.PackConfig(
            packrepository, p_251654_.getDataConfiguration(), false, false
         )
         .createResourceManager()
         .getSecond();
      this.minecraft.doWorldLoad(p_250919_, packrepository, new WorldStem(closeableresourcemanager, p_248897_, p_250801_, p_251654_), true);
   }

   public WorldStem loadWorldStem(Dynamic<?> p_312184_, boolean p_233124_, PackRepository p_233125_) throws Exception {
      WorldLoader.PackConfig worldloader$packconfig = LevelStorageSource.getPackConfig(p_312184_, p_233125_, p_233124_);
      return this.loadWorldDataBlocking(
         worldloader$packconfig,
         p_389329_ -> {
            Registry<LevelStem> registry = p_389329_.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
            LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
               p_312184_, p_389329_.dataConfiguration(), registry, p_389329_.datapackWorldgen()
            );
            return new WorldLoader.DataLoadOutput<>(leveldataanddimensions.worldData(), leveldataanddimensions.dimensions().dimensionsRegistryAccess());
         },
         WorldStem::new
      );
   }

   public Pair<LevelSettings, WorldCreationContext> recreateWorldData(LevelStorageSource.LevelStorageAccess p_249540_) throws Exception {
      PackRepository packrepository = ServerPacksSource.createPackRepository(p_249540_);
      Dynamic<?> dynamic = p_249540_.getDataTag();
      WorldLoader.PackConfig worldloader$packconfig = LevelStorageSource.getPackConfig(dynamic, packrepository, false);

      @OnlyIn(Dist.CLIENT)
      record Data(LevelSettings levelSettings, WorldOptions options, Registry<LevelStem> existingDimensions) {
      }

      return this.loadWorldDataBlocking(
         worldloader$packconfig,
         p_357766_ -> {
            Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
            LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
               dynamic, p_357766_.dataConfiguration(), registry, p_357766_.datapackWorldgen()
            );
            return new WorldLoader.DataLoadOutput<>(
               new Data(
                  leveldataanddimensions.worldData().getLevelSettings(),
                  leveldataanddimensions.worldData().worldGenOptions(),
                  leveldataanddimensions.dimensions().dimensions()
               ),
               p_357766_.datapackDimensions()
            );
         },
         (p_448107_, p_448108_, p_448109_, p_448110_) -> {
            p_448107_.close();
            InitialWorldCreationOptions initialworldcreationoptions = new InitialWorldCreationOptions(
               WorldCreationUiState.SelectedGameMode.SURVIVAL, GameRuleMap.of(), null
            );
            return Pair.of(
               p_448110_.levelSettings,
               new WorldCreationContext(
                  p_448110_.options,
                  new WorldDimensions(p_448110_.existingDimensions),
                  p_448109_,
                  p_448108_,
                  p_448110_.levelSettings.getDataConfiguration(),
                  initialworldcreationoptions
               )
            );
         }
      );
   }

   private <D, R> R loadWorldDataBlocking(
      WorldLoader.PackConfig p_250997_, WorldLoader.WorldDataSupplier<D> p_251759_, WorldLoader.ResultFactory<D, R> p_249635_
   ) throws Exception {
      WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(
         p_250997_, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER
      );
      CompletableFuture<R> completablefuture = WorldLoader.load(worldloader$initconfig, p_251759_, p_249635_, Util.backgroundExecutor(), this.minecraft);
      this.minecraft.managedBlock(completablefuture::isDone);
      return completablefuture.get();
   }

   private void askForBackup(LevelStorageSource.LevelStorageAccess p_312560_, boolean p_233143_, Runnable p_233144_, Runnable p_312163_) {
      Component component;
      Component component1;
      if (p_233143_) {
         component = Component.translatable("selectWorld.backupQuestion.customized");
         component1 = Component.translatable("selectWorld.backupWarning.customized");
      } else {
         component = Component.translatable("selectWorld.backupQuestion.experimental");
         component1 = Component.translatable("selectWorld.backupWarning.experimental");
      }

      this.minecraft.setScreen(new BackupConfirmScreen(p_312163_, (p_308273_, p_308274_) -> {
         if (p_308273_) {
            EditWorldScreen.makeBackupAndShowToast(p_312560_);
         }

         p_233144_.run();
      }, component, component1, false));
   }

   public static void confirmWorldCreation(Minecraft p_270593_, CreateWorldScreen p_270733_, Lifecycle p_270539_, Runnable p_270158_, boolean p_270709_) {
      BooleanConsumer booleanconsumer = p_233154_ -> {
         if (p_233154_) {
            p_270158_.run();
         } else {
            p_270593_.setScreen(p_270733_);
         }
      };
      if (p_270709_ || p_270539_ == Lifecycle.stable()) {
         p_270158_.run();
      } else if (p_270539_ == Lifecycle.experimental()) {
         p_270593_.setScreen(
            new ConfirmScreen(
               booleanconsumer,
               Component.translatable("selectWorld.warning.experimental.title"),
               Component.translatable("selectWorld.warning.experimental.question")
            )
         );
      } else {
         p_270593_.setScreen(
            new ConfirmScreen(
               booleanconsumer,
               Component.translatable("selectWorld.warning.deprecated.title"),
               Component.translatable("selectWorld.warning.deprecated.question")
            )
         );
      }
   }

   public void openWorld(String p_332907_, Runnable p_332472_) {
      this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
      LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(p_332907_);
      if (levelstoragesource$levelstorageaccess != null) {
         this.openWorldLoadLevelData(levelstoragesource$levelstorageaccess, p_332472_);
      }
   }

   private void openWorldLoadLevelData(LevelStorageSource.LevelStorageAccess p_330142_, Runnable p_335478_) {
      this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));

      Dynamic<?> dynamic;
      LevelSummary levelsummary;
      try {
         dynamic = p_330142_.getDataTag();
         levelsummary = p_330142_.getSummary(dynamic);
      } catch (NbtException | ReportedNbtException | IOException ioexception) {
         this.minecraft.setScreen(new RecoverWorldDataScreen(this.minecraft, p_325454_ -> {
            if (p_325454_) {
               this.openWorldLoadLevelData(p_330142_, p_335478_);
            } else {
               p_330142_.safeClose();
               p_335478_.run();
            }
         }, p_330142_));
         return;
      } catch (OutOfMemoryError outofmemoryerror1) {
         MemoryReserve.release();
         String s = "Ran out of memory trying to read level data of world folder \"" + p_330142_.getLevelId() + "\"";
         LOGGER.error(LogUtils.FATAL_MARKER, s);
         OutOfMemoryError outofmemoryerror = new OutOfMemoryError("Ran out of memory reading level data");
         outofmemoryerror.initCause(outofmemoryerror1);
         CrashReport crashreport = CrashReport.forThrowable(outofmemoryerror, s);
         CrashReportCategory crashreportcategory = crashreport.addCategory("World details");
         crashreportcategory.setDetail("World folder", p_330142_.getLevelId());
         throw new ReportedException(crashreport);
      }

      this.openWorldCheckVersionCompatibility(p_330142_, levelsummary, dynamic, p_335478_);
   }

   private void openWorldCheckVersionCompatibility(
      LevelStorageSource.LevelStorageAccess p_335405_, LevelSummary p_331961_, Dynamic<?> p_333467_, Runnable p_328023_
   ) {
      if (!p_331961_.isCompatible()) {
         p_335405_.safeClose();
         this.minecraft
            .setScreen(
               new AlertScreen(
                  p_328023_,
                  Component.translatable("selectWorld.incompatible.title").withColor(-65536),
                  Component.translatable("selectWorld.incompatible.description", p_331961_.getWorldVersionName())
               )
            );
      } else {
         LevelSummary.BackupStatus levelsummary$backupstatus = p_331961_.backupStatus();
         if (levelsummary$backupstatus.shouldBackup()) {
            String s = "selectWorld.backupQuestion." + levelsummary$backupstatus.getTranslationKey();
            String s1 = "selectWorld.backupWarning." + levelsummary$backupstatus.getTranslationKey();
            MutableComponent mutablecomponent = Component.translatable(s);
            if (levelsummary$backupstatus.isSevere()) {
               mutablecomponent.withColor(-2142128);
            }

            Component component = Component.translatable(s1, p_331961_.getWorldVersionName(), SharedConstants.getCurrentVersion().name());
            this.minecraft.setScreen(new BackupConfirmScreen(() -> {
               p_335405_.safeClose();
               p_328023_.run();
            }, (p_325458_, p_325459_) -> {
               if (p_325458_) {
                  EditWorldScreen.makeBackupAndShowToast(p_335405_);
               }

               this.openWorldLoadLevelStem(p_335405_, p_333467_, false, p_328023_);
            }, mutablecomponent, component, false));
         } else {
            this.openWorldLoadLevelStem(p_335405_, p_333467_, false, p_328023_);
         }
      }
   }

   private void openWorldLoadLevelStem(LevelStorageSource.LevelStorageAccess p_333651_, Dynamic<?> p_332568_, boolean p_334192_, Runnable p_332843_) {
      this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.resource_load")));
      PackRepository packrepository = ServerPacksSource.createPackRepository(p_333651_);

      WorldStem worldstem;
      try {
         worldstem = this.loadWorldStem(p_332568_, p_334192_, packrepository);

         for (LevelStem levelstem : worldstem.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)) {
            levelstem.generator().validate();
         }
      } catch (Exception exception) {
         LOGGER.warn("Failed to load level data or datapacks, can't proceed with server load", exception);
         if (!p_334192_) {
            this.minecraft.setScreen(new DatapackLoadFailureScreen(() -> {
               p_333651_.safeClose();
               p_332843_.run();
            }, () -> this.openWorldLoadLevelStem(p_333651_, p_332568_, true, p_332843_)));
         } else {
            p_333651_.safeClose();
            this.minecraft
               .setScreen(
                  new AlertScreen(
                     p_332843_,
                     Component.translatable("datapackFailure.safeMode.failed.title"),
                     Component.translatable("datapackFailure.safeMode.failed.description"),
                     CommonComponents.GUI_BACK,
                     true
                  )
               );
         }

         return;
      }

      this.openWorldCheckWorldStemCompatibility(p_333651_, worldstem, packrepository, p_332843_);
   }

   private void openWorldCheckWorldStemCompatibility(
      LevelStorageSource.LevelStorageAccess p_329946_, WorldStem p_331923_, PackRepository p_329592_, Runnable p_331882_
   ) {
      WorldData worlddata = p_331923_.worldData();
      boolean flag = worlddata.worldGenOptions().isOldCustomizedWorld();
      boolean flag1 = worlddata.worldGenSettingsLifecycle() != Lifecycle.stable();
      if (!flag && !flag1) {
         this.openWorldLoadBundledResourcePack(p_329946_, p_331923_, p_329592_, p_331882_);
      } else {
         this.askForBackup(p_329946_, flag, () -> this.openWorldLoadBundledResourcePack(p_329946_, p_331923_, p_329592_, p_331882_), () -> {
            p_331923_.close();
            p_329946_.safeClose();
            p_331882_.run();
         });
      }
   }

   private void openWorldLoadBundledResourcePack(
      LevelStorageSource.LevelStorageAccess p_332203_, WorldStem p_333813_, PackRepository p_328830_, Runnable p_331357_
   ) {
      DownloadedPackSource downloadedpacksource = this.minecraft.getDownloadedPackSource();
      this.loadBundledResourcePack(downloadedpacksource, p_332203_).thenApply(p_233177_ -> true).exceptionallyComposeAsync(p_233183_ -> {
         LOGGER.warn("Failed to load pack: ", p_233183_);
         return this.promptBundledPackLoadFailure();
      }, this.minecraft).thenAcceptAsync(p_325451_ -> {
         if (p_325451_) {
            this.openWorldCheckDiskSpace(p_332203_, p_333813_, downloadedpacksource, p_328830_, p_331357_);
         } else {
            downloadedpacksource.popAll();
            p_333813_.close();
            p_332203_.safeClose();
            p_331357_.run();
         }
      }, this.minecraft).exceptionally(p_233175_ -> {
         this.minecraft.delayCrash(CrashReport.forThrowable(p_233175_, "Load world"));
         return null;
      });
   }

   private void openWorldCheckDiskSpace(
      LevelStorageSource.LevelStorageAccess p_332115_, WorldStem p_329606_, DownloadedPackSource p_331698_, PackRepository p_334521_, Runnable p_330770_
   ) {
      if (p_332115_.checkForLowDiskSpace()) {
         this.minecraft
            .setScreen(
               new ConfirmScreen(
                  p_325469_ -> {
                     if (p_325469_) {
                        this.openWorldDoLoad(p_332115_, p_329606_, p_334521_);
                     } else {
                        p_331698_.popAll();
                        p_329606_.close();
                        p_332115_.safeClose();
                        p_330770_.run();
                     }
                  },
                  Component.translatable("selectWorld.warning.lowDiskSpace.title").withStyle(ChatFormatting.RED),
                  Component.translatable("selectWorld.warning.lowDiskSpace.description"),
                  CommonComponents.GUI_CONTINUE,
                  CommonComponents.GUI_BACK
               )
            );
      } else {
         this.openWorldDoLoad(p_332115_, p_329606_, p_334521_);
      }
   }

   private void openWorldDoLoad(LevelStorageSource.LevelStorageAccess p_329495_, WorldStem p_329186_, PackRepository p_331916_) {
      this.minecraft.doWorldLoad(p_329495_, p_331916_, p_329186_, false);
   }

   private CompletableFuture<Void> loadBundledResourcePack(DownloadedPackSource p_312230_, LevelStorageSource.LevelStorageAccess p_310544_) {
      Path path = p_310544_.getLevelPath(LevelResource.MAP_RESOURCE_FILE);
      if (Files.exists(path) && !Files.isDirectory(path)) {
         p_312230_.configureForLocalWorld();
         CompletableFuture<Void> completablefuture = p_312230_.waitForPackFeedback(WORLD_PACK_ID);
         p_312230_.pushLocalPack(WORLD_PACK_ID, path);
         return completablefuture;
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
      CompletableFuture<Boolean> completablefuture = new CompletableFuture<>();
      this.minecraft
         .setScreen(
            new ConfirmScreen(
               completablefuture::complete,
               Component.translatable("multiplayer.texturePrompt.failure.line1"),
               Component.translatable("multiplayer.texturePrompt.failure.line2"),
               CommonComponents.GUI_PROCEED,
               CommonComponents.GUI_CANCEL
            )
         );
      return completablefuture;
   }
}
