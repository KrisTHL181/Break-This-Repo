package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;

public class DatapackStructureReport implements DataProvider {
   private final PackOutput output;
   private static final DatapackStructureReport.Entry PSEUDO_REGISTRY = new DatapackStructureReport.Entry(true, false, true);
   private static final DatapackStructureReport.Entry STABLE_DYNAMIC_REGISTRY = new DatapackStructureReport.Entry(true, true, true);
   private static final DatapackStructureReport.Entry UNSTABLE_DYNAMIC_REGISTRY = new DatapackStructureReport.Entry(true, true, false);
   private static final DatapackStructureReport.Entry BUILT_IN_REGISTRY = new DatapackStructureReport.Entry(false, true, true);
   private static final Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> MANUAL_ENTRIES = Map.of(
      Registries.RECIPE,
      PSEUDO_REGISTRY,
      Registries.ADVANCEMENT,
      PSEUDO_REGISTRY,
      Registries.LOOT_TABLE,
      STABLE_DYNAMIC_REGISTRY,
      Registries.ITEM_MODIFIER,
      STABLE_DYNAMIC_REGISTRY,
      Registries.PREDICATE,
      STABLE_DYNAMIC_REGISTRY
   );
   private static final Map<String, DatapackStructureReport.CustomPackEntry> NON_REGISTRY_ENTRIES = Map.of(
      "structure",
      new DatapackStructureReport.CustomPackEntry(DatapackStructureReport.Format.STRUCTURE, new DatapackStructureReport.Entry(true, false, true)),
      "function",
      new DatapackStructureReport.CustomPackEntry(DatapackStructureReport.Format.MCFUNCTION, new DatapackStructureReport.Entry(true, true, true))
   );
   static final Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY_CODEC = Identifier.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::identifier);

   public DatapackStructureReport(PackOutput p_366731_) {
      this.output = p_366731_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_365219_) {
      DatapackStructureReport.Report datapackstructurereport$report = new DatapackStructureReport.Report(this.listRegistries(), NON_REGISTRY_ENTRIES);
      Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("datapack.json");
      return DataProvider.saveStable(
         p_365219_, (JsonElement)DatapackStructureReport.Report.CODEC.encodeStart(JsonOps.INSTANCE, datapackstructurereport$report).getOrThrow(), path
      );
   }

   @Override
   public String getName() {
      return "Datapack Structure";
   }

   private void putIfNotPresent(
      Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> p_367377_,
      ResourceKey<? extends Registry<?>> p_360807_,
      DatapackStructureReport.Entry p_367029_
   ) {
      DatapackStructureReport.Entry datapackstructurereport$entry = p_367377_.putIfAbsent(p_360807_, p_367029_);
      if (datapackstructurereport$entry != null) {
         throw new IllegalStateException("Duplicate entry for key " + p_360807_.identifier());
      }
   }

   private Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> listRegistries() {
      Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> map = new HashMap<>();
      BuiltInRegistries.REGISTRY.forEach(p_365187_ -> this.putIfNotPresent(map, p_365187_.key(), BUILT_IN_REGISTRY));
      RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(p_360913_ -> this.putIfNotPresent(map, p_360913_.key(), UNSTABLE_DYNAMIC_REGISTRY));
      RegistryDataLoader.DIMENSION_REGISTRIES.forEach(p_369046_ -> this.putIfNotPresent(map, p_369046_.key(), UNSTABLE_DYNAMIC_REGISTRY));
      MANUAL_ENTRIES.forEach((p_360843_, p_368350_) -> this.putIfNotPresent(map, (ResourceKey<? extends Registry<?>>)p_360843_, p_368350_));
      return map;
   }

   record CustomPackEntry(DatapackStructureReport.Format format, DatapackStructureReport.Entry entry) {
      public static final Codec<DatapackStructureReport.CustomPackEntry> CODEC = RecordCodecBuilder.create(
         p_368584_ -> p_368584_.group(
               DatapackStructureReport.Format.CODEC.fieldOf("format").forGetter(DatapackStructureReport.CustomPackEntry::format),
               DatapackStructureReport.Entry.MAP_CODEC.forGetter(DatapackStructureReport.CustomPackEntry::entry)
            )
            .apply(p_368584_, DatapackStructureReport.CustomPackEntry::new)
      );
   }

   record Entry(boolean elements, boolean tags, boolean stable) {
      public static final MapCodec<DatapackStructureReport.Entry> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_368666_ -> p_368666_.group(
               Codec.BOOL.fieldOf("elements").forGetter(DatapackStructureReport.Entry::elements),
               Codec.BOOL.fieldOf("tags").forGetter(DatapackStructureReport.Entry::tags),
               Codec.BOOL.fieldOf("stable").forGetter(DatapackStructureReport.Entry::stable)
            )
            .apply(p_368666_, DatapackStructureReport.Entry::new)
      );
      public static final Codec<DatapackStructureReport.Entry> CODEC = MAP_CODEC.codec();
   }

   enum Format implements StringRepresentable {
      STRUCTURE("structure"),
      MCFUNCTION("mcfunction");

      public static final Codec<DatapackStructureReport.Format> CODEC = StringRepresentable.fromEnum(DatapackStructureReport.Format::values);
      private final String name;

      Format(final String p_363444_) {
         this.name = p_363444_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   record Report(Map<ResourceKey<? extends Registry<?>>, DatapackStructureReport.Entry> registries, Map<String, DatapackStructureReport.CustomPackEntry> others) {
      public static final Codec<DatapackStructureReport.Report> CODEC = RecordCodecBuilder.create(
         p_369194_ -> p_369194_.group(
               Codec.unboundedMap(DatapackStructureReport.REGISTRY_KEY_CODEC, DatapackStructureReport.Entry.CODEC)
                  .fieldOf("registries")
                  .forGetter(DatapackStructureReport.Report::registries),
               Codec.unboundedMap(Codec.STRING, DatapackStructureReport.CustomPackEntry.CODEC)
                  .fieldOf("others")
                  .forGetter(DatapackStructureReport.Report::others)
            )
            .apply(p_369194_, DatapackStructureReport.Report::new)
      );
   }
}
