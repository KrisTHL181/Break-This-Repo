package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
   private final String prefix;
   private final String extension;

   public FileToIdConverter(String p_248876_, String p_251478_) {
      this.prefix = p_248876_;
      this.extension = p_251478_;
   }

   public static FileToIdConverter json(String p_248754_) {
      return new FileToIdConverter(p_248754_, ".json");
   }

   public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> p_375453_) {
      return json(Registries.elementsDirPath(p_375453_));
   }

   public Identifier idToFile(Identifier p_452775_) {
      return p_452775_.withPath(this.prefix + "/" + p_452775_.getPath() + this.extension);
   }

   public Identifier fileToId(Identifier p_450329_) {
      String s = p_450329_.getPath();
      return p_450329_.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
   }

   public Map<Identifier, Resource> listMatchingResources(ResourceManager p_252045_) {
      return p_252045_.listResources(this.prefix, p_448783_ -> p_448783_.getPath().endsWith(this.extension));
   }

   public Map<Identifier, List<Resource>> listMatchingResourceStacks(ResourceManager p_249881_) {
      return p_249881_.listResourceStacks(this.prefix, p_448784_ -> p_448784_.getPath().endsWith(this.extension));
   }
}
