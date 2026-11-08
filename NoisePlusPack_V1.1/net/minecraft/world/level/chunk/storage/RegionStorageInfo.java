package net.minecraft.world.level.chunk.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RegionStorageInfo(String level, ResourceKey<Level> dimension, String type) {
   public RegionStorageInfo withTypeSuffix(String p_334043_) {
      return new RegionStorageInfo(this.level, this.dimension, this.type + p_334043_);
   }
}
