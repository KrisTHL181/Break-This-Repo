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

   public PackMetadataGenerator(PackOutput p_254070_) {
      this.output = p_254070_;
   }

   public <T> PackMetadataGenerator add(MetadataSectionType<T> p_252067_, T p_249511_) {
      this.elements
         .put(
            p_252067_.name(),
            () -> ((JsonElement)p_252067_.codec().encodeStart(JsonOps.INSTANCE, p_249511_).getOrThrow(IllegalArgumentException::new)).getAsJsonObject()
         );
      return this;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254137_) {
      JsonObject jsonobject = new JsonObject();
      this.elements.forEach((p_249290_, p_251317_) -> jsonobject.add(p_249290_, p_251317_.get()));
      return DataProvider.saveStable(p_254137_, jsonobject, this.output.getOutputFolder().resolve("pack.mcmeta"));
   }

   @Override
   public final String getName() {
      return "Pack Metadata";
   }

   public static PackMetadataGenerator forFeaturePack(PackOutput p_256281_, Component p_255661_) {
      return new PackMetadataGenerator(p_256281_)
         .add(PackMetadataSection.SERVER_TYPE, new PackMetadataSection(p_255661_, DetectedVersion.BUILT_IN.packVersion(PackType.SERVER_DATA).minorRange()));
   }

   public static PackMetadataGenerator forFeaturePack(PackOutput p_253903_, Component p_254497_, FeatureFlagSet p_253848_) {
      return forFeaturePack(p_253903_, p_254497_).add(FeatureFlagsMetadataSection.TYPE, new FeatureFlagsMetadataSection(p_253848_));
   }
}
