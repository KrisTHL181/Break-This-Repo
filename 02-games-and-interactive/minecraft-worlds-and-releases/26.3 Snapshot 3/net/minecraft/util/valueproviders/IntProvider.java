package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

public interface IntProvider {
   int sample(RandomSource random);

   int minInclusive();

   int maxInclusive();

   MapCodec<? extends IntProvider> codec();
}
