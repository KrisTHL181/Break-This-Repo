package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public interface IntegerModifier<Argument> extends AttributeModifier<Integer, Argument> {
    IntegerModifier<Integer> ADD = (IntegerModifier.Simple)Integer::sum;
    IntegerModifier<Integer> SUBTRACT = (IntegerModifier.Simple)(a, b) -> a - b;
    IntegerModifier<Integer> MULTIPLY = (IntegerModifier.Simple)(a, b) -> a * b;
    IntegerModifier<Integer> MINIMUM = (IntegerModifier.Simple)Math::min;
    IntegerModifier<Integer> MAXIMUM = (IntegerModifier.Simple)Math::max;

    Integer apply(Integer integer, Argument argument);

    @FunctionalInterface
    interface Simple extends IntegerModifier<Integer> {
        @Override
        default Codec<Integer> argumentCodec(final EnvironmentAttribute<Integer> type) {
            return Codec.INT;
        }

        @Override
        default LerpFunction<Integer> argumentKeyframeLerp(final EnvironmentAttribute<Integer> type) {
            return LerpFunction.ofInteger();
        }
    }
}
