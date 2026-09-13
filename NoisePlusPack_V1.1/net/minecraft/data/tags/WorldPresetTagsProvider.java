package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public class WorldPresetTagsProvider extends KeyTagProvider<WorldPreset> {
   public WorldPresetTagsProvider(PackOutput p_255701_, CompletableFuture<HolderLookup.Provider> p_255974_) {
      super(p_255701_, Registries.WORLD_PRESET, p_255974_);
   }

   @Override
   protected void addTags(HolderLookup.Provider p_255734_) {
      this.tag(WorldPresetTags.NORMAL)
         .add(WorldPresets.NORMAL)
         .add(WorldPresets.FLAT)
         .add(WorldPresets.LARGE_BIOMES)
         .add(WorldPresets.AMPLIFIED)
         .add(WorldPresets.SINGLE_BIOME_SURFACE);
      this.tag(WorldPresetTags.EXTENDED).addTag(WorldPresetTags.NORMAL).add(WorldPresets.DEBUG);
   }
}
