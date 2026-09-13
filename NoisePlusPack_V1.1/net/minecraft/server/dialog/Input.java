package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.input.InputControl;

public record Input(String key, InputControl control) {
   public static final Codec<Input> CODEC = RecordCodecBuilder.create(
      p_409994_ -> p_409994_.group(ParsedTemplate.VARIABLE_CODEC.fieldOf("key").forGetter(Input::key), InputControl.MAP_CODEC.forGetter(Input::control))
         .apply(p_409994_, Input::new)
   );
}
