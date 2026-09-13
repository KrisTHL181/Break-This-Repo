package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.server.dialog.action.Action;

public interface ButtonListDialog extends Dialog {
    @Override
    MapCodec<? extends ButtonListDialog> codec();

    int columns();

    Optional<ActionButton> exitAction();

    @Override
    default Optional<Action> onCancel() {
        return this.exitAction().flatMap(ActionButton::action);
    }
}