package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class StructureProcessorTypes {
   public static MapCodec<? extends StructureProcessor> bootstrap(final Registry<MapCodec<? extends StructureProcessor>> registry) {
      Registry.register(registry, "blackstone_replace", BlackstoneReplaceProcessor.MAP_CODEC);
      Registry.register(registry, "block_age", BlockAgeProcessor.MAP_CODEC);
      Registry.register(registry, "block_ignore", BlockIgnoreProcessor.MAP_CODEC);
      Registry.register(registry, "block_rot", BlockRotProcessor.MAP_CODEC);
      Registry.register(registry, "capped", CappedProcessor.MAP_CODEC);
      Registry.register(registry, "gravity", GravityProcessor.MAP_CODEC);
      Registry.register(registry, "jigsaw_replacement", JigsawReplacementProcessor.MAP_CODEC);
      Registry.register(registry, "lava_submerged_block", LavaSubmergedBlockProcessor.MAP_CODEC);
      Registry.register(registry, "nop", NopProcessor.MAP_CODEC);
      Registry.register(registry, "protected_blocks", ProtectedBlockProcessor.MAP_CODEC);
      return Registry.register(registry, "rule", RuleProcessor.MAP_CODEC);
   }
}
