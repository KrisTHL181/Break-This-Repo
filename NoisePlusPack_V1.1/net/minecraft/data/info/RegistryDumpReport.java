package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public class RegistryDumpReport implements DataProvider {
   private final PackOutput output;

   public RegistryDumpReport(PackOutput p_249862_) {
      this.output = p_249862_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_253743_) {
      JsonObject jsonobject = new JsonObject();
      BuiltInRegistries.REGISTRY
         .listElements()
         .forEach(p_448672_ -> jsonobject.add(p_448672_.key().identifier().toString(), dumpRegistry((Registry<?>)p_448672_.value())));
      Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("registries.json");
      return DataProvider.saveStable(p_253743_, jsonobject, path);
   }

   private static <T> JsonElement dumpRegistry(Registry<T> p_124059_) {
      JsonObject jsonobject = new JsonObject();
      if (p_124059_ instanceof DefaultedRegistry) {
         Identifier identifier = ((DefaultedRegistry)p_124059_).getDefaultKey();
         jsonobject.addProperty("default", identifier.toString());
      }

      int i = BuiltInRegistries.REGISTRY.getId(p_124059_);
      jsonobject.addProperty("protocol_id", i);
      JsonObject jsonobject1 = new JsonObject();
      p_124059_.listElements().forEach(p_448675_ -> {
         T t = p_448675_.value();
         int j = p_124059_.getId(t);
         JsonObject jsonobject2 = new JsonObject();
         jsonobject2.addProperty("protocol_id", j);
         jsonobject1.add(p_448675_.key().identifier().toString(), jsonobject2);
      });
      jsonobject.add("entries", jsonobject1);
      return jsonobject;
   }

   @Override
   public final String getName() {
      return "Registry Dump";
   }
}
