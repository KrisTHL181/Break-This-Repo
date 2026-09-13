package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RootPlacerType<P extends RootPlacer> {
   public static final RootPlacerType<MangroveRootPlacer> MANGROVE_ROOT_PLACER = register("mangrove_root_placer", MangroveRootPlacer.CODEC);
   private final MapCodec<P> codec;

   private static <P extends RootPlacer> RootPlacerType<P> register(String p_225905_, MapCodec<P> p_329098_) {
      return Registry.register(BuiltInRegistries.ROOT_PLACER_TYPE, p_225905_, new RootPlacerType<>(p_329098_));
   }

   private RootPlacerType(MapCodec<P> p_333995_) {
      this.codec = p_333995_;
   }

   public MapCodec<P> codec() {
      return this.codec;
   }
}
