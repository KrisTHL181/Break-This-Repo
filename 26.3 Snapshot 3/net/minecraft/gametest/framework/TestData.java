package net.minecraft.gametest.framework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Rotation;

public record TestData<EnvironmentType>(
   EnvironmentType environment,
   Identifier structure,
   int maxTicks,
   int setupTicks,
   boolean required,
   Rotation rotation,
   boolean manualOnly,
   int maxAttempts,
   int requiredSuccesses,
   boolean skyAccess,
   int padding
) {
   public static final MapCodec<TestData<Holder<TestEnvironmentDefinition<?>>>> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            TestEnvironmentDefinition.CODEC.fieldOf("environment").forGetter(TestData::environment),
            Identifier.CODEC.fieldOf("structure").forGetter(TestData::structure),
            ExtraCodecs.POSITIVE_INT.fieldOf("max_ticks").forGetter(TestData::maxTicks),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("setup_ticks", 0).forGetter(TestData::setupTicks),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(TestData::required),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(TestData::rotation),
            Codec.BOOL.optionalFieldOf("manual_only", false).forGetter(TestData::manualOnly),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("max_attempts", 1).forGetter(TestData::maxAttempts),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("required_successes", 1).forGetter(TestData::requiredSuccesses),
            Codec.BOOL.optionalFieldOf("sky_access", false).forGetter(TestData::skyAccess),
            ExtraCodecs.intRange(0, 128).optionalFieldOf("padding", 0).forGetter(TestData::padding)
         )
         .apply(i, TestData::new)
   );

   public TestData(
      final EnvironmentType environment, final Identifier structure, final int maxTicks, final int setupTicks, final boolean required, final Rotation rotation
   ) {
      this(environment, structure, maxTicks, setupTicks, required, rotation, false, 1, 1, false, 0);
   }

   public TestData(final EnvironmentType environment, final Identifier structure, final int maxTicks, final int setupTicks, final boolean required) {
      this(environment, structure, maxTicks, setupTicks, required, Rotation.NONE);
   }

   public <T> TestData<T> map(final Function<EnvironmentType, T> mapper) {
      return new TestData<>(
         mapper.apply(this.environment),
         this.structure,
         this.maxTicks,
         this.setupTicks,
         this.required,
         this.rotation,
         this.manualOnly,
         this.maxAttempts,
         this.requiredSuccesses,
         this.skyAccess,
         this.padding
      );
   }
}
