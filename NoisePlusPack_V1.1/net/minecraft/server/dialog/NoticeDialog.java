package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.dialog.action.Action;

public record NoticeDialog(CommonDialogData common, ActionButton action) implements SimpleDialog {
   public static final ActionButton DEFAULT_ACTION = new ActionButton(new CommonButtonData(CommonComponents.GUI_OK, 150), Optional.empty());
   public static final MapCodec<NoticeDialog> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_408748_ -> p_408748_.group(
            CommonDialogData.MAP_CODEC.forGetter(NoticeDialog::common),
            ActionButton.CODEC.optionalFieldOf("action", DEFAULT_ACTION).forGetter(NoticeDialog::action)
         )
         .apply(p_408748_, NoticeDialog::new)
   );

   @Override
   public MapCodec<NoticeDialog> codec() {
      return MAP_CODEC;
   }

   @Override
   public Optional<Action> onCancel() {
      return this.action.action();
   }

   @Override
   public List<ActionButton> mainActions() {
      return List.of(this.action);
   }
}
