package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.WorldDataConfiguration;
import org.slf4j.Logger;

public class WorldLoader {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <D, R> CompletableFuture<R> load(
      WorldLoader.InitConfig p_214363_,
      WorldLoader.WorldDataSupplier<D> p_214364_,
      WorldLoader.ResultFactory<D, R> p_214365_,
      Executor p_214366_,
      Executor p_214367_
   ) {
      try {
         Pair<WorldDataConfiguration, CloseableResourceManager> pair = p_214363_.packConfig.createResourceManager();
         CloseableResourceManager closeableresourcemanager = (CloseableResourceManager)pair.getSecond();
         LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = RegistryLayer.createRegistryAccess();
         List<Registry.PendingTags<?>> list = TagLoader.loadTagsForExistingRegistries(
            closeableresourcemanager, layeredregistryaccess.getLayer(RegistryLayer.STATIC)
         );
         RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(RegistryLayer.WORLDGEN);
         List<HolderLookup.RegistryLookup<?>> list1 = TagLoader.buildUpdatedLookups(registryaccess$frozen, list);
         RegistryAccess.Frozen registryaccess$frozen1 = RegistryDataLoader.load(closeableresourcemanager, list1, RegistryDataLoader.WORLDGEN_REGISTRIES);
         List<HolderLookup.RegistryLookup<?>> list2 = Stream.concat(list1.stream(), registryaccess$frozen1.listRegistries()).toList();
         RegistryAccess.Frozen registryaccess$frozen2 = RegistryDataLoader.load(closeableresourcemanager, list2, RegistryDataLoader.DIMENSION_REGISTRIES);
         WorldDataConfiguration worlddataconfiguration = (WorldDataConfiguration)pair.getFirst();
         HolderLookup.Provider holderlookup$provider = HolderLookup.Provider.create(list2.stream());
         WorldLoader.DataLoadOutput<D> dataloadoutput = p_214364_.get(
            new WorldLoader.DataLoadContext(closeableresourcemanager, worlddataconfiguration, holderlookup$provider, registryaccess$frozen2)
         );
         LayeredRegistryAccess<RegistryLayer> layeredregistryaccess1 = layeredregistryaccess.replaceFrom(
            RegistryLayer.WORLDGEN, registryaccess$frozen1, dataloadoutput.finalDimensions
         );
         return ReloadableServerResources.loadResources(
               closeableresourcemanager,
               layeredregistryaccess1,
               list,
               worlddataconfiguration.enabledFeatures(),
               p_214363_.commandSelection(),
               p_214363_.functionCompilationPermissions(),
               p_214366_,
               p_214367_
            )
            .whenComplete((p_214370_, p_214371_) -> {
               if (p_214371_ != null) {
                  closeableresourcemanager.close();
               }
            })
            .thenApplyAsync(p_358549_ -> {
               p_358549_.updateStaticRegistryTags();
               return p_214365_.create(closeableresourcemanager, p_358549_, layeredregistryaccess1, dataloadoutput.cookie);
            }, p_214367_);
      } catch (Exception exception) {
         return CompletableFuture.failedFuture(exception);
      }
   }

   public record DataLoadContext(
      ResourceManager resources, WorldDataConfiguration dataConfiguration, HolderLookup.Provider datapackWorldgen, RegistryAccess.Frozen datapackDimensions
   ) {
   }

   public record DataLoadOutput<D>(D cookie, RegistryAccess.Frozen finalDimensions) {
   }

   public record InitConfig(WorldLoader.PackConfig packConfig, Commands.CommandSelection commandSelection, PermissionSet functionCompilationPermissions) {
   }

   public record PackConfig(PackRepository packRepository, WorldDataConfiguration initialDataConfig, boolean safeMode, boolean initMode) {
      public Pair<WorldDataConfiguration, CloseableResourceManager> createResourceManager() {
         WorldDataConfiguration worlddataconfiguration = MinecraftServer.configurePackRepository(
            this.packRepository, this.initialDataConfig, this.initMode, this.safeMode
         );
         List<PackResources> list = this.packRepository.openAllSelected();
         CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, list);
         return Pair.of(worlddataconfiguration, closeableresourcemanager);
      }
   }

   @FunctionalInterface
   public interface ResultFactory<D, R> {
      R create(CloseableResourceManager var1, ReloadableServerResources var2, LayeredRegistryAccess<RegistryLayer> var3, D var4);
   }

   @FunctionalInterface
   public interface WorldDataSupplier<D> {
      WorldLoader.DataLoadOutput<D> get(WorldLoader.DataLoadContext var1);
   }
}
