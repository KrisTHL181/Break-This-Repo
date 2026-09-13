package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BuiltInPackSource implements RepositorySource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String VANILLA_ID = "vanilla";
   public static final String TESTS_ID = "tests";
   public static final KnownPack CORE_PACK_INFO = KnownPack.vanilla("core");
   private final PackType packType;
   private final VanillaPackResources vanillaPack;
   private final Identifier packDir;
   private final DirectoryValidator validator;

   public BuiltInPackSource(final PackType packType, final VanillaPackResources vanillaPack, final Identifier packDir, final DirectoryValidator validator) {
      this.packType = packType;
      this.vanillaPack = vanillaPack;
      this.packDir = packDir;
      this.validator = validator;
   }

   @Override
   public void loadPacks(final Consumer<Pack> result) {
      Pack vanilla = this.createVanillaPack(this.vanillaPack.asResourcesSupplier());
      if (vanilla != null) {
         result.accept(vanilla);
      }

      this.listBundledPacks(result);
   }

   protected abstract @Nullable Pack createVanillaPack(final Pack.ResourcesSupplier resources);

   protected abstract Component getPackTitle(String id);

   public VanillaPackResources getVanillaPack() {
      return this.vanillaPack;
   }

   private void listBundledPacks(final Consumer<Pack> packConsumer) {
      Map<String, Function<String, Pack>> discoveredPacks = new HashMap<>();
      this.populatePackList(discoveredPacks::put);
      discoveredPacks.forEach((id, packSupplier) -> {
         Pack pack = packSupplier.apply(id);
         if (pack != null) {
            packConsumer.accept(pack);
         }
      });
   }

   protected void populatePackList(final BiConsumer<String, Function<String, Pack>> discoveredPacks) {
      this.vanillaPack.listRawPaths(this.packType, this.packDir, path -> this.discoverPacksInPath(path, discoveredPacks));
   }

   protected void discoverPacksInPath(final @Nullable Path targetDir, final BiConsumer<String, Function<String, @Nullable Pack>> discoveredPacks) {
      if (targetDir != null && Files.isDirectory(targetDir)) {
         try {
            FolderRepositorySource.discoverPacks(
               targetDir,
               this.validator,
               (path, resources) -> discoveredPacks.accept(pathToId(path), id -> this.createBuiltinPack(id, resources, this.getPackTitle(id)))
            );
         } catch (IOException e) {
            LOGGER.warn("Failed to discover packs in {}", targetDir, e);
         }
      }
   }

   private static String pathToId(final Path path) {
      return StringUtils.removeEnd(path.getFileName().toString(), ".zip");
   }

   protected abstract @Nullable Pack createBuiltinPack(final String id, final Pack.ResourcesSupplier resources, final Component name);
}
