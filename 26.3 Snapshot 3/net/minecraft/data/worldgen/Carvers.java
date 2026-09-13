package net.minecraft.data.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.TrapezoidFloat;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.VeryBiasedToBottomInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.carver.CaveWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public interface Carvers {
   ResourceKey<WorldCarver> CAVE = createKey("cave");
   ResourceKey<WorldCarver> CAVE_EXTRA_UNDERGROUND = createKey("cave_extra_underground");
   ResourceKey<WorldCarver> CANYON = createKey("canyon");
   ResourceKey<WorldCarver> NETHER_CAVE = createKey("nether_cave");

   private static ResourceKey<WorldCarver> createKey(final String name) {
      return ResourceKey.create(Registries.CARVER, Identifier.withDefaultNamespace(name));
   }

   static void bootstrap(final BootstrapContext<WorldCarver> context) {
      context.register(
         CAVE,
         new CaveWorldCarver(
            0.15F,
            UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(180)),
            VeryBiasedToBottomInt.of(0, 14),
            TrapezoidFloat.of(0.0F, 3.0F, 1.0F),
            true,
            UniformFloat.of(0.1F, 0.9F),
            UniformFloat.of(0.7F, 1.4F),
            UniformFloat.of(0.8F, 1.3F),
            ConstantFloat.of(1.0F),
            UniformFloat.of(-1.0F, -0.4F)
         )
      );
      context.register(
         CAVE_EXTRA_UNDERGROUND,
         new CaveWorldCarver(
            0.07F,
            UniformHeight.of(VerticalAnchor.aboveBottom(8), VerticalAnchor.absolute(47)),
            VeryBiasedToBottomInt.of(0, 14),
            TrapezoidFloat.of(0.0F, 3.0F, 1.0F),
            true,
            UniformFloat.of(0.1F, 0.9F),
            UniformFloat.of(0.7F, 1.4F),
            UniformFloat.of(0.8F, 1.3F),
            ConstantFloat.of(1.0F),
            UniformFloat.of(-1.0F, -0.4F)
         )
      );
      context.register(
         CANYON,
         new CanyonWorldCarver(
            0.01F,
            UniformHeight.of(VerticalAnchor.absolute(10), VerticalAnchor.absolute(67)),
            UniformFloat.of(-0.125F, 0.125F),
            new CanyonWorldCarver.Shape(
               UniformFloat.of(0.75F, 1.0F), TrapezoidFloat.of(0.0F, 6.0F, 2.0F), 3, UniformFloat.of(0.75F, 1.0F), 1.0F, 0.0F, ConstantFloat.of(3.0F)
            )
         )
      );
      context.register(
         NETHER_CAVE,
         new CaveWorldCarver(
            0.2F,
            UniformHeight.of(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(1)),
            VeryBiasedToBottomInt.of(0, 9),
            TrapezoidFloat.of(0.0F, 6.0F, 2.0F),
            false,
            ConstantFloat.of(0.5F),
            ConstantFloat.of(1.0F),
            ConstantFloat.of(1.0F),
            ConstantFloat.of(5.0F),
            ConstantFloat.of(-0.7F)
         )
      );
   }
}
