package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.util.ExtraCodecs;

public interface Dialog {
   Codec<Integer> WIDTH_CODEC = ExtraCodecs.intRange(1, 1024);
   Codec<Dialog> DIRECT_CODEC = BuiltInRegistries.DIALOG_TYPE.byNameCodec().dispatch(Dialog::codec, p_409522_ -> p_409522_);
   Codec<Holder<Dialog>> CODEC = RegistryFileCodec.create(Registries.DIALOG, DIRECT_CODEC);
   Codec<HolderSet<Dialog>> LIST_CODEC = RegistryCodecs.homogeneousList(Registries.DIALOG, DIRECT_CODEC);
   StreamCodec<RegistryFriendlyByteBuf, Holder<Dialog>> STREAM_CODEC = ByteBufCodecs.holder(
      Registries.DIALOG, ByteBufCodecs.fromCodecWithRegistriesTrusted(DIRECT_CODEC)
   );
   StreamCodec<ByteBuf, Dialog> CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(DIRECT_CODEC);

   CommonDialogData common();

   MapCodec<? extends Dialog> codec();

   Optional<Action> onCancel();
}
