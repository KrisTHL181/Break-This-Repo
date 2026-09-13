package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record BooleanInput(Component label, boolean initial, String onTrue, String onFalse) implements InputControl {
   public static final MapCodec<BooleanInput> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_410613_ -> p_410613_.group(
            ComponentSerialization.CODEC.fieldOf("label").forGetter(BooleanInput::label),
            Codec.BOOL.optionalFieldOf("initial", false).forGetter(BooleanInput::initial),
            Codec.STRING.optionalFieldOf("on_true", "true").forGetter(BooleanInput::onTrue),
            Codec.STRING.optionalFieldOf("on_false", "false").forGetter(BooleanInput::onFalse)
         )
         .apply(p_410613_, BooleanInput::new)
   );

   @Override
   public MapCodec<BooleanInput> mapCodec() {
      return MAP_CODEC;
   }
}
