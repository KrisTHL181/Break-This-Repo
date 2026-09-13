package net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RegistryDataCollector {
   private RegistryDataCollector.@Nullable ContentsCollector contentsCollector;
   private RegistryDataCollector.@Nullable TagCollector tagCollector;

   public void appendContents(ResourceKey<? extends Registry<?>> p_331647_, List<RegistrySynchronization.PackedRegistryEntry> p_327881_) {
      if (this.contentsCollector == null) {
         this.contentsCollector = new RegistryDataCollector.ContentsCollector();
      }

      this.contentsCollector.append(p_331647_, p_327881_);
   }

   public void appendTags(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> p_329188_) {
      if (this.tagCollector == null) {
         this.tagCollector = new RegistryDataCollector.TagCollector();
      }

      p_329188_.forEach(this.tagCollector::append);
   }

   private static <T> Registry.PendingTags<T> resolveRegistryTags(
      RegistryAccess.Frozen p_360841_, ResourceKey<? extends Registry<? extends T>> p_363100_, TagNetworkSerialization.NetworkPayload p_362008_
   ) {
      Registry<T> registry = p_360841_.lookupOrThrow(p_363100_);
      return registry.prepareTagReload(p_362008_.resolve(registry));
   }

   private RegistryAccess loadNewElementsAndTags(ResourceProvider p_367565_, RegistryDataCollector.ContentsCollector p_362016_, boolean p_364598_) {
      LayeredRegistryAccess<ClientRegistryLayer> layeredregistryaccess = ClientRegistryLayer.createRegistryAccess();
      RegistryAccess.Frozen registryaccess$frozen = layeredregistryaccess.getAccessForLoading(ClientRegistryLayer.REMOTE);
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> map = new HashMap<>();
      p_362016_.elements
         .forEach(
            (p_365837_, p_368362_) -> map.put(
               (ResourceKey<? extends Registry<?>>)p_365837_,
               new RegistryDataLoader.NetworkedRegistryData(
                  (List<RegistrySynchronization.PackedRegistryEntry>)p_368362_, TagNetworkSerialization.NetworkPayload.EMPTY
               )
            )
         );
      List<Registry.PendingTags<?>> list = new ArrayList<>();
      if (this.tagCollector != null) {
         this.tagCollector.forEach((p_369903_, p_361286_) -> {
            if (!p_361286_.isEmpty()) {
               if (RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_369903_)) {
                  map.compute((ResourceKey<? extends Registry<?>>)p_369903_, (p_364673_, p_362709_) -> {
                     List<RegistrySynchronization.PackedRegistryEntry> list2 = p_362709_ != null ? p_362709_.elements() : List.of();
                     return new RegistryDataLoader.NetworkedRegistryData(list2, p_361286_);
                  });
               } else if (!p_364598_) {
                  list.add(resolveRegistryTags(registryaccess$frozen, (ResourceKey<? extends Registry<?>>)p_369903_, p_361286_));
               }
            }
         });
      }

      List<HolderLookup.RegistryLookup<?>> list1 = TagLoader.buildUpdatedLookups(registryaccess$frozen, list);

      RegistryAccess.Frozen registryaccess$frozen1;
      try {
         registryaccess$frozen1 = RegistryDataLoader.load(map, p_367565_, list1, RegistryDataLoader.SYNCHRONIZED_REGISTRIES).freeze();
      } catch (Exception exception) {
         CrashReport crashreport = CrashReport.forThrowable(exception, "Network Registry Load");
         addCrashDetails(crashreport, map, list);
         throw new ReportedException(crashreport);
      }

      RegistryAccess registryaccess = layeredregistryaccess.replaceFrom(ClientRegistryLayer.REMOTE, registryaccess$frozen1).compositeAccess();
      list.forEach(Registry.PendingTags::apply);
      return registryaccess;
   }

   private static void addCrashDetails(
      CrashReport p_361122_,
      Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> p_366915_,
      List<Registry.PendingTags<?>> p_369882_
   ) {
      CrashReportCategory crashreportcategory = p_361122_.addCategory("Received Elements and Tags");
      crashreportcategory.setDetail(
         "Dynamic Registries",
         () -> p_366915_.entrySet()
            .stream()
            .sorted(Comparator.comparing(p_448121_ -> p_448121_.getKey().identifier()))
            .map(
               p_448124_ -> String.format(
                  Locale.ROOT,
                  "\n\t\t%s: elements=%d tags=%d",
                  p_448124_.getKey().identifier(),
                  p_448124_.getValue().elements().size(),
                  p_448124_.getValue().tags().size()
               )
            )
            .collect(Collectors.joining())
      );
      crashreportcategory.setDetail(
         "Static Registries",
         () -> p_369882_.stream()
            .sorted(Comparator.comparing(p_448123_ -> p_448123_.key().identifier()))
            .map(p_448122_ -> String.format(Locale.ROOT, "\n\t\t%s: tags=%d", p_448122_.key().identifier(), p_448122_.size()))
            .collect(Collectors.joining())
      );
   }

   private void loadOnlyTags(RegistryDataCollector.TagCollector p_362263_, RegistryAccess.Frozen p_361181_, boolean p_369477_) {
      p_362263_.forEach((p_370187_, p_363143_) -> {
         if (p_369477_ || RegistrySynchronization.isNetworkable((ResourceKey<? extends Registry<?>>)p_370187_)) {
            resolveRegistryTags(p_361181_, (ResourceKey<? extends Registry<?>>)p_370187_, p_363143_).apply();
         }
      });
   }

   public RegistryAccess.Frozen collectGameRegistries(ResourceProvider p_333941_, RegistryAccess.Frozen p_368312_, boolean p_328462_) {
      RegistryAccess registryaccess;
      if (this.contentsCollector != null) {
         registryaccess = this.loadNewElementsAndTags(p_333941_, this.contentsCollector, p_328462_);
      } else {
         if (this.tagCollector != null) {
            this.loadOnlyTags(this.tagCollector, p_368312_, !p_328462_);
         }

         registryaccess = p_368312_;
      }

      return registryaccess.freeze();
   }

   @OnlyIn(Dist.CLIENT)
   static class ContentsCollector {
      final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> elements = new HashMap<>();

      public void append(ResourceKey<? extends Registry<?>> p_331127_, List<RegistrySynchronization.PackedRegistryEntry> p_331340_) {
         this.elements.computeIfAbsent(p_331127_, p_332834_ -> new ArrayList<>()).addAll(p_331340_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class TagCollector {
      private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = new HashMap<>();

      public void append(ResourceKey<? extends Registry<?>> p_367185_, TagNetworkSerialization.NetworkPayload p_369486_) {
         this.tags.put(p_367185_, p_369486_);
      }

      public void forEach(BiConsumer<? super ResourceKey<? extends Registry<?>>, ? super TagNetworkSerialization.NetworkPayload> p_365146_) {
         this.tags.forEach(p_365146_);
      }
   }
}
