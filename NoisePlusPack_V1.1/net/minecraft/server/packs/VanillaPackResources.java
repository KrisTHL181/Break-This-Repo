package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FileUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;
   private final BuiltInMetadata metadata;
   private final Set<String> namespaces;
   private final List<Path> rootPaths;
   private final Map<PackType, List<Path>> pathsForType;

   VanillaPackResources(PackLocationInfo p_331390_, BuiltInMetadata p_249743_, Set<String> p_250468_, List<Path> p_248798_, Map<PackType, List<Path>> p_251106_) {
      this.location = p_331390_;
      this.metadata = p_249743_;
      this.namespaces = p_250468_;
      this.rootPaths = p_248798_;
      this.pathsForType = p_251106_;
   }

   @Override
   public @Nullable IoSupplier<InputStream> getRootResource(String... p_250530_) {
      FileUtil.validatePath(p_250530_);
      List<String> list = List.of(p_250530_);

      for (Path path : this.rootPaths) {
         Path path1 = FileUtil.resolvePath(path, list);
         if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
            return IoSupplier.create(path1);
         }
      }

      return null;
   }

   public void listRawPaths(PackType p_252103_, Identifier p_456273_, Consumer<Path> p_251968_) {
      FileUtil.decomposePath(p_456273_.getPath()).ifSuccess(p_449171_ -> {
         String s = p_456273_.getNamespace();

         for (Path path : this.pathsForType.get(p_252103_)) {
            Path path1 = path.resolve(s);
            p_251968_.accept(FileUtil.resolvePath(path1, p_449171_));
         }
      }).ifError(p_326467_ -> LOGGER.error("Invalid path {}: {}", p_456273_, p_326467_.message()));
   }

   @Override
   public void listResources(PackType p_248974_, String p_248703_, String p_250848_, PackResources.ResourceOutput p_249668_) {
      FileUtil.decomposePath(p_250848_).ifSuccess(p_248228_ -> {
         List<Path> list = this.pathsForType.get(p_248974_);
         int i = list.size();
         if (i == 1) {
            getResources(p_249668_, p_248703_, list.get(0), p_248228_);
         } else if (i > 1) {
            Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();

            for (int j = 0; j < i - 1; j++) {
               getResources(map::putIfAbsent, p_248703_, list.get(j), p_248228_);
            }

            Path path = list.get(i - 1);
            if (map.isEmpty()) {
               getResources(p_249668_, p_248703_, path, p_248228_);
            } else {
               getResources(map::putIfAbsent, p_248703_, path, p_248228_);
               map.forEach(p_249668_);
            }
         }
      }).ifError(p_326469_ -> LOGGER.error("Invalid path {}: {}", p_250848_, p_326469_.message()));
   }

   private static void getResources(PackResources.ResourceOutput p_249662_, String p_251249_, Path p_251290_, List<String> p_250451_) {
      Path path = p_251290_.resolve(p_251249_);
      PathPackResources.listPath(p_251249_, path, p_250451_, p_249662_);
   }

   @Override
   public @Nullable IoSupplier<InputStream> getResource(PackType p_250512_, Identifier p_452512_) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(p_452512_.getPath()).mapOrElse(p_248224_ -> {
         String s = p_452512_.getNamespace();

         for (Path path : this.pathsForType.get(p_250512_)) {
            Path path1 = FileUtil.resolvePath(path.resolve(s), p_248224_);
            if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
               return IoSupplier.create(path1);
            }
         }

         return null;
      }, p_326471_ -> {
         LOGGER.error("Invalid path {}: {}", p_452512_, p_326471_.message());
         return null;
      });
   }

   @Override
   public Set<String> getNamespaces(PackType p_10322_) {
      return this.namespaces;
   }

   @Override
   public <T> @Nullable T getMetadataSection(MetadataSectionType<T> p_378082_) {
      IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
      if (iosupplier != null) {
         try (InputStream inputstream = iosupplier.get()) {
            T t = AbstractPackResources.getMetadataFromStream(p_378082_, inputstream, this.location);
            if (t == null) {
               return this.metadata.get(p_378082_);
            }

            return t;
         } catch (IOException var8) {
         }
      }

      return this.metadata.get(p_378082_);
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }

   @Override
   public void close() {
   }

   public ResourceProvider asProvider() {
      return p_449167_ -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, p_449167_))
         .map(p_248221_ -> new Resource(this, (IoSupplier<InputStream>)p_248221_));
   }
}
