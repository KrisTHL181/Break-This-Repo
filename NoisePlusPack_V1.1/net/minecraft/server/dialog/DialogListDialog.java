package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;

public record DialogListDialog(CommonDialogData common, HolderSet<Dialog> dialogs, Optional<ActionButton> exitAction, int columns, int buttonWidth)
   implements ButtonListDialog {
   public static final MapCodec<DialogListDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_408936_ -> p_408936_.group(
            CommonDialogData.MAP_CODEC.forGetter(DialogListDialog::common),
            Dialog.LIST_CODEC.fieldOf("dialogs").forGetter(DialogListDialog::dialogs),
            ActionButton.CODEC.optionalFieldOf("exit_action").forGetter(DialogListDialog::exitAction),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("columns", 2).forGetter(DialogListDialog::columns),
            WIDTH_CODEC.optionalFieldOf("button_width", 150).forGetter(DialogListDialog::buttonWidth)
         )
         .apply(p_408936_, DialogListDialog::new)
   );

   @Override
   public MapCodec<DialogListDialog> codec() {
      return MAP_CODEC;
   }
}
