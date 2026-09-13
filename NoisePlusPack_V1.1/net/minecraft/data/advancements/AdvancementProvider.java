package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public class AdvancementProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;
   private final List<AdvancementSubProvider> subProviders;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public AdvancementProvider(PackOutput p_256529_, CompletableFuture<HolderLookup.Provider> p_255722_, List<AdvancementSubProvider> p_255883_) {
      this.pathProvider = p_256529_.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
      this.subProviders = p_255883_;
      this.registries = p_255722_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_254268_) {
      return this.registries.thenCompose(p_325834_ -> {
         Set<Identifier> set = new HashSet<>();
         List<CompletableFuture<?>> list = new ArrayList<>();
         Consumer<AdvancementHolder> consumer = p_448634_ -> {
            if (!set.add(p_448634_.id())) {
               throw new IllegalStateException("Duplicate advancement " + p_448634_.id());
            }

            Path path = this.pathProvider.json(p_448634_.id());
            list.add(DataProvider.saveStable(p_254268_, p_325834_, Advancement.CODEC, p_448634_.value(), path));
         };

         for (AdvancementSubProvider advancementsubprovider : this.subProviders) {
            advancementsubprovider.generate(p_325834_, consumer);
         }

         return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
      });
   }

   @Override
   public final String getName() {
      return "Advancements";
   }
}
