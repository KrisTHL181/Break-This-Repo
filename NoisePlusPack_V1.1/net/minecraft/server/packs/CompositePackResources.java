package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public class CompositePackResources implements PackResources {
   private final PackResources primaryPackResources;
   private final List<PackResources> packResourcesStack;

   public CompositePackResources(PackResources p_301152_, List<PackResources> p_299588_) {
      this.primaryPackResources = p_301152_;
      List<PackResources> list = new ArrayList<>(p_299588_.size() + 1);
      list.addAll(Lists.reverse(p_299588_));
      list.add(p_301152_);
      this.packResourcesStack = List.copyOf(list);
   }

   @Override
   public @Nullable IoSupplier<InputStream> getRootResource(String... p_299314_) {
      return this.primaryPackResources.getRootResource(p_299314_);
   }

   @Override
   public @Nullable IoSupplier<InputStream> getResource(PackType p_299283_, Identifier p_456860_) {
      for (PackResources packresources : this.packResourcesStack) {
         IoSupplier<InputStream> iosupplier = packresources.getResource(p_299283_, p_456860_);
         if (iosupplier != null) {
            return iosupplier;
         }
      }

      return null;
   }

   @Override
   public void listResources(PackType p_299029_, String p_300961_, String p_297881_, PackResources.ResourceOutput p_298322_) {
      Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();

      for (PackResources packresources : this.packResourcesStack) {
         packresources.listResources(p_299029_, p_300961_, p_297881_, map::putIfAbsent);
      }

      map.forEach(p_298322_);
   }

   @Override
   public Set<String> getNamespaces(PackType p_299362_) {
      Set<String> set = new HashSet<>();

      for (PackResources packresources : this.packResourcesStack) {
         set.addAll(packresources.getNamespaces(p_299362_));
      }

      return set;
   }

   @Override
   public <T> @Nullable T getMetadataSection(MetadataSectionType<T> p_378152_) throws IOException {
      return this.primaryPackResources.getMetadataSection(p_378152_);
   }

   @Override
   public PackLocationInfo location() {
      return this.primaryPackResources.location();
   }

   @Override
   public void close() {
      this.packResourcesStack.forEach(PackResources::close);
   }
}
