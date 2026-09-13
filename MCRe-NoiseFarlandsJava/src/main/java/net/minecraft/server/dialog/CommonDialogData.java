package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.body.DialogBody;

public record CommonDialogData(
    Component title,
    Optional<Component> externalTitle,
    boolean canCloseWithEscape,
    boolean pause,
    DialogAction afterAction,
    List<DialogBody> body,
    List<Input> inputs
) {
    public static final MapCodec<CommonDialogData> MAP_CODEC = RecordCodecBuilder.<CommonDialogData>mapCodec(
            i -> i.group(
                    ComponentSerialization.CODEC.fieldOf("title").forGetter(CommonDialogData::title),
                    ComponentSerialization.CODEC.optionalFieldOf("external_title").forGetter(CommonDialogData::externalTitle),
                    Codec.BOOL.optionalFieldOf("can_close_with_escape", true).forGetter(CommonDialogData::canCloseWithEscape),
                    Codec.BOOL.optionalFieldOf("pause", true).forGetter(CommonDialogData::pause),
                    DialogAction.CODEC.optionalFieldOf("after_action", DialogAction.CLOSE).forGetter(CommonDialogData::afterAction),
                    DialogBody.COMPACT_LIST_CODEC.optionalFieldOf("body", List.of()).forGetter(CommonDialogData::body),
                    Input.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(CommonDialogData::inputs)
                )
                .apply(i, CommonDialogData::new)
        )
        .validate(
            data -> data.pause && !data.afterAction.willUnpause()
                ? DataResult.error(() -> "Dialogs that pause the game must use after_action values that unpause it after user action!")
                : DataResult.success(data)
        );

    public Component computeExternalTitle() {
        return this.externalTitle.orElse(this.title);
    }
}