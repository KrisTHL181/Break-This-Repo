package net.minecraft.data;

import java.nio.file.Path;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PackOutput {
   private final Path outputFolder;

   public PackOutput(Path p_252039_) {
      this.outputFolder = p_252039_;
   }

   public Path getOutputFolder() {
      return this.outputFolder;
   }

   public Path getOutputFolder(PackOutput.Target p_251669_) {
      return this.getOutputFolder().resolve(p_251669_.directory);
   }

   public PackOutput.PathProvider createPathProvider(PackOutput.Target p_249479_, String p_251050_) {
      return new PackOutput.PathProvider(this, p_249479_, p_251050_);
   }

   public PackOutput.PathProvider createRegistryElementsPathProvider(ResourceKey<? extends Registry<?>> p_344086_) {
      return this.createPathProvider(PackOutput.Target.DATA_PACK, Registries.elementsDirPath(p_344086_));
   }

   public PackOutput.PathProvider createRegistryTagsPathProvider(ResourceKey<? extends Registry<?>> p_345128_) {
      return this.createPathProvider(PackOutput.Target.DATA_PACK, Registries.tagsDirPath(p_345128_));
   }

   public static class PathProvider {
      private final Path root;
      private final String kind;

      PathProvider(PackOutput p_249025_, PackOutput.Target p_251200_, String p_251982_) {
         this.root = p_249025_.getOutputFolder(p_251200_);
         this.kind = p_251982_;
      }

      public Path file(Identifier p_458453_, String p_251208_) {
         return this.root.resolve(p_458453_.getNamespace()).resolve(this.kind).resolve(p_458453_.getPath() + "." + p_251208_);
      }

      public Path json(Identifier p_453775_) {
         return this.root.resolve(p_453775_.getNamespace()).resolve(this.kind).resolve(p_453775_.getPath() + ".json");
      }

      public Path json(ResourceKey<?> p_376925_) {
         return this.root.resolve(p_376925_.identifier().getNamespace()).resolve(this.kind).resolve(p_376925_.identifier().getPath() + ".json");
      }
   }

   public enum Target {
      DATA_PACK("data"),
      RESOURCE_PACK("assets"),
      REPORTS("reports");

      final String directory;

      Target(final String p_251326_) {
         this.directory = p_251326_;
      }
   }
}
