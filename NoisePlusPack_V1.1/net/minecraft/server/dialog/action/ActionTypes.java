package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public class ActionTypes {
   public static MapCodec<? extends Action> bootstrap(Registry<MapCodec<? extends Action>> p_406033_) {
      StaticAction.WRAPPED_CODECS
         .forEach((p_449109_, p_449110_) -> Registry.register(p_406033_, Identifier.withDefaultNamespace(p_449109_.getSerializedName()), p_449110_));
      Registry.register(p_406033_, Identifier.withDefaultNamespace("dynamic/run_command"), CommandTemplate.MAP_CODEC);
      return Registry.register(p_406033_, Identifier.withDefaultNamespace("dynamic/custom"), CustomAll.MAP_CODEC);
   }
}
