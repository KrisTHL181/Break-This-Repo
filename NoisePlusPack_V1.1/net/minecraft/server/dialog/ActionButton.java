package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.server.dialog.action.Action;

public record ActionButton(CommonButtonData button, Optional<Action> action) {
   public static final Codec<ActionButton> CODEC = RecordCodecBuilder.create(
      p_407743_ -> p_407743_.group(
            CommonButtonData.MAP_CODEC.forGetter(ActionButton::button), Action.CODEC.optionalFieldOf("action").forGetter(ActionButton::action)
         )
         .apply(p_407743_, ActionButton::new)
   );
}
