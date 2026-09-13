package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

public record ServerLinksDialog(CommonDialogData common, Optional<ActionButton> exitAction, int columns, int buttonWidth) implements ButtonListDialog {
   public static final MapCodec<ServerLinksDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_409805_ -> p_409805_.group(
            CommonDialogData.MAP_CODEC.forGetter(ServerLinksDialog::common),
            ActionButton.CODEC.optionalFieldOf("exit_action").forGetter(ServerLinksDialog::exitAction),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("columns", 2).forGetter(ServerLinksDialog::columns),
            WIDTH_CODEC.optionalFieldOf("button_width", 150).forGetter(ServerLinksDialog::buttonWidth)
         )
         .apply(p_409805_, ServerLinksDialog::new)
   );

   @Override
   public MapCodec<ServerLinksDialog> codec() {
      return MAP_CODEC;
   }
}
