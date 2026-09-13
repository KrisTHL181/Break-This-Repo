package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import java.util.List;

public interface SimpleDialog extends Dialog {
    @Override
    MapCodec<? extends SimpleDialog> codec();

    List<ActionButton> mainActions();
}