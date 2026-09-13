package net.minecraft.data.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.DetectedVersion;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.flag.FeatureFlagSet;

public class PackMetadataGenerator implements DataProvider {
   private final PackOutput output;
   private final Map<String, Supplier<JsonElement>> elements = new HashMap<>();

   public PackMetadataGenerator(final PackOutput output) {
      this.output = output;
   }

   public <T> PackMetadataGenerator add(final MetadataSectionType<T> type, final T value) {
      this.elements
         .put(type.name(), () -> ((JsonElement)type.codec().encodeStart(JsonOps.INSTANCE, value).getOrThrow(IllegalArgumentException::new)).getAsJsonObject());
      return this;
   }

   @Override
   public CompletableFuture<?> run(final CachedOutput cache) {
      JsonObject result = new JsonObject();
      this.elements.forEach((id, data) -> result.add(id, data.get()));
      return DataProvider.saveStable(cache, result, this.output.getOutputFolder().resolve("pack.mcmeta"));
   }

   @Override
   public final String getName() {
      return "Pack Metadata";
   }

   public static PackMetadataGenerator forFeaturePack(final PackOutput output, final Component description) {
      return new PackMetadataGenerator(output)
         .add(PackMetadataSection.SERVER_TYPE, new PackMetadataSection(description, DetectedVersion.BUILT_IN.packVersion(PackType.SERVER_DATA).minorRange()));
   }

   public static PackMetadataGenerator forFeaturePack(final PackOutput output, final Component description, final FeatureFlagSet flags) {
      return forFeaturePack(output, description).add(FeatureFlagsMetadataSection.TYPE, new FeatureFlagsMetadataSection(flags));
   }
}
