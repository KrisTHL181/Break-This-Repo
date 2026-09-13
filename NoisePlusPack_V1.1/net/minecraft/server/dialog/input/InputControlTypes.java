package net.minecraft.server.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class InputControlTypes {
   public static MapCodec<? extends InputControl> bootstrap(Registry<MapCodec<? extends InputControl>> p_408845_) {
      Registry.register(p_408845_, Identifier.withDefaultNamespace("boolean"), BooleanInput.MAP_CODEC);
      Registry.register(p_408845_, Identifier.withDefaultNamespace("number_range"), NumberRangeInput.MAP_CODEC);
      Registry.register(p_408845_, Identifier.withDefaultNamespace("single_option"), SingleOptionInput.MAP_CODEC);
      return Registry.register(p_408845_, Identifier.withDefaultNamespace("text"), TextInput.MAP_CODEC);
   }
}
