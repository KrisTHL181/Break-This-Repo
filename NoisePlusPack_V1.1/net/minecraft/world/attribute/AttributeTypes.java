package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.MoonPhase;

public interface AttributeTypes {
   AttributeType<Boolean> BOOLEAN = register("boolean", AttributeType.ofNotInterpolated(Codec.BOOL, AttributeModifier.BOOLEAN_LIBRARY));
   AttributeType<TriState> TRI_STATE = register("tri_state", AttributeType.ofNotInterpolated(TriState.CODEC));
   AttributeType<Float> FLOAT = register("float", AttributeType.ofInterpolated(Codec.FLOAT, AttributeModifier.FLOAT_LIBRARY, LerpFunction.ofFloat()));
   AttributeType<Float> ANGLE_DEGREES = register(
      "angle_degrees", AttributeType.ofInterpolated(Codec.FLOAT, AttributeModifier.FLOAT_LIBRARY, LerpFunction.ofFloat(), LerpFunction.ofDegrees(90.0F))
   );
   AttributeType<Integer> RGB_COLOR = register(
      "rgb_color", AttributeType.ofInterpolated(ExtraCodecs.STRING_RGB_COLOR, AttributeModifier.RGB_COLOR_LIBRARY, LerpFunction.ofColor())
   );
   AttributeType<Integer> ARGB_COLOR = register(
      "argb_color", AttributeType.ofInterpolated(ExtraCodecs.STRING_ARGB_COLOR, AttributeModifier.ARGB_COLOR_LIBRARY, LerpFunction.ofColor())
   );
   AttributeType<MoonPhase> MOON_PHASE = register("moon_phase", AttributeType.ofNotInterpolated(MoonPhase.CODEC));
   AttributeType<Activity> ACTIVITY = register("activity", AttributeType.ofNotInterpolated(BuiltInRegistries.ACTIVITY.byNameCodec()));
   AttributeType<BedRule> BED_RULE = register("bed_rule", AttributeType.ofNotInterpolated(BedRule.CODEC));
   AttributeType<ParticleOptions> PARTICLE = register("particle", AttributeType.ofNotInterpolated(ParticleTypes.CODEC));
   AttributeType<List<AmbientParticle>> AMBIENT_PARTICLES = register("ambient_particles", AttributeType.ofNotInterpolated(AmbientParticle.CODEC.listOf()));
   AttributeType<BackgroundMusic> BACKGROUND_MUSIC = register("background_music", AttributeType.ofNotInterpolated(BackgroundMusic.CODEC));
   AttributeType<AmbientSounds> AMBIENT_SOUNDS = register("ambient_sounds", AttributeType.ofNotInterpolated(AmbientSounds.CODEC));
   Codec<AttributeType<?>> CODEC = BuiltInRegistries.ATTRIBUTE_TYPE.byNameCodec();

   static AttributeType<?> bootstrap(Registry<AttributeType<?>> p_458120_) {
      return BOOLEAN;
   }

   static <Value> AttributeType<Value> register(String p_453091_, AttributeType<Value> p_455840_) {
      Registry.register(BuiltInRegistries.ATTRIBUTE_TYPE, Identifier.withDefaultNamespace(p_453091_), p_455840_);
      return p_455840_;
   }
}
