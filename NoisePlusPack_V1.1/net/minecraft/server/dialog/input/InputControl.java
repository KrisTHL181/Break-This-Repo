package net.minecraft.server.dialog.input;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;

public interface InputControl {
   MapCodec<InputControl> MAP_CODEC = BuiltInRegistries.INPUT_CONTROL_TYPE.byNameCodec().dispatchMap(InputControl::mapCodec, p_408668_ -> p_408668_);

   MapCodec<? extends InputControl> mapCodec();
}
