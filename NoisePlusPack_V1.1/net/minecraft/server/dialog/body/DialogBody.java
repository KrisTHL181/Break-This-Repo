package net.minecraft.server.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;

public interface DialogBody {
   Codec<DialogBody> DIALOG_BODY_CODEC = BuiltInRegistries.DIALOG_BODY_TYPE.byNameCodec().dispatch(DialogBody::mapCodec, p_409220_ -> p_409220_);
   Codec<List<DialogBody>> COMPACT_LIST_CODEC = ExtraCodecs.compactListCodec(DIALOG_BODY_CODEC);

   MapCodec<? extends DialogBody> mapCodec();
}
