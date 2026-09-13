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
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
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

   public BuiltInPackSource(PackType p_249137_, VanillaPackResources p_250453_, Identifier p_459603_, DirectoryValidator p_300643_) {
      this.packType = p_249137_;
      this.vanillaPack = p_250453_;
      this.packDir = p_459603_;
      this.validator = p_300643_;
   }

   @Override
   public void loadPacks(Consumer<Pack> p_250708_) {
      Pack pack = this.createVanillaPack(this.vanillaPack);
      if (pack != null) {
         p_250708_.accept(pack);
      }

      this.listBundledPacks(p_250708_);
   }

   protected abstract @Nullable Pack createVanillaPack(PackResources var1);

   protected abstract Component getPackTitle(String var1);

   public VanillaPackResources getVanillaPack() {
      return this.vanillaPack;
   }

   private void listBundledPacks(Consumer<Pack> p_249128_) {
      Map<String, Function<String, Pack>> map = new HashMap<>();
      this.populatePackList(map::put);
      map.forEach((p_250371_, p_250946_) -> {
         Pack pack = p_250946_.apply(p_250371_);
         if (pack != null) {
            p_249128_.accept(pack);
         }
      });
   }

   protected void populatePackList(BiConsumer<String, Function<String, Pack>> p_250341_) {
      this.vanillaPack.listRawPaths(this.packType, this.packDir, p_250248_ -> this.discoverPacksInPath(p_250248_, p_250341_));
   }

   protected void discoverPacksInPath(@Nullable Path p_250013_, BiConsumer<String, Function<String, @Nullable Pack>> p_249898_) {
      if (p_250013_ != null && Files.isDirectory(p_250013_)) {
         try {
            FolderRepositorySource.discoverPacks(
               p_250013_,
               this.validator,
               (p_252012_, p_249772_) -> p_249898_.accept(
                  pathToId(p_252012_), p_250601_ -> this.createBuiltinPack(p_250601_, p_249772_, this.getPackTitle(p_250601_))
               )
            );
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to discover packs in {}", p_250013_, ioexception);
         }
      }
   }

   private static String pathToId(Path p_252048_) {
      return StringUtils.removeEnd(p_252048_.getFileName().toString(), ".zip");
   }

   protected abstract @Nullable Pack createBuiltinPack(String var1, Pack.ResourcesSupplier var2, Component var3);

   protected static Pack.ResourcesSupplier fixedResources(final PackResources p_298206_) {
      return new Pack.ResourcesSupplier() {
         @Override
         public PackResources openPrimary(PackLocationInfo p_333958_) {
            return p_298206_;
         }

         @Override
         public PackResources openFull(PackLocationInfo p_336095_, Pack.Metadata p_328489_) {
            return p_298206_;
         }
      };
   }
}
