package net.minecraft.server.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class InputControlTypes {
   public static MapCodec<? extends InputControl> bootstrap(final Registry<MapCodec<? extends InputControl>> registry) {
      Registry.register(registry, Identifier.withDefaultNamespace("boolean"), BooleanInput.MAP_CODEC);
      Registry.register(registry, Identifier.withDefaultNamespace("number_range"), NumberRangeInput.MAP_CODEC);
      Registry.register(registry, Identifier.withDefaultNamespace("single_option"), SingleOptionInput.MAP_CODEC);
      return Registry.register(registry, Identifier.withDefaultNamespace("text"), TextInput.MAP_CODEC);
   }
}
