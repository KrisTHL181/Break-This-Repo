package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;

public class ItemListReport implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public ItemListReport(PackOutput p_333960_, CompletableFuture<HolderLookup.Provider> p_331732_) {
      this.output = p_333960_;
      this.registries = p_331732_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_328088_) {
      Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("items.json");
      return this.registries
         .thenCompose(
            p_358205_ -> {
               JsonObject jsonobject = new JsonObject();
               RegistryOps<JsonElement> registryops = p_358205_.createSerializationContext(JsonOps.INSTANCE);
               p_358205_.lookupOrThrow(Registries.ITEM)
                  .listElements()
                  .forEach(
                     p_341012_ -> {
                        JsonObject jsonobject1 = new JsonObject();
                        jsonobject1.add(
                           "components",
                           (JsonElement)DataComponentMap.CODEC
                              .encodeStart(registryops, p_341012_.value().components())
                              .getOrThrow(p_341009_ -> new IllegalStateException("Failed to encode components: " + p_341009_))
                        );
                        jsonobject.add(p_341012_.getRegisteredName(), jsonobject1);
                     }
                  );
               return DataProvider.saveStable(p_328088_, jsonobject, path);
            }
         );
   }

   @Override
   public final String getName() {
      return "Item List";
   }
}
