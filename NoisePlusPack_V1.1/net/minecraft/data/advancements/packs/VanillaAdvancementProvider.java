package net.minecraft.data.advancements.packs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;

public class VanillaAdvancementProvider {
   public static AdvancementProvider create(PackOutput p_255890_, CompletableFuture<HolderLookup.Provider> p_255777_) {
      return new AdvancementProvider(
         p_255890_,
         p_255777_,
         List.of(
            new VanillaTheEndAdvancements(),
            new VanillaHusbandryAdvancements(),
            new VanillaAdventureAdvancements(),
            new VanillaNetherAdvancements(),
            new VanillaStoryAdvancements()
         )
      );
   }
}
