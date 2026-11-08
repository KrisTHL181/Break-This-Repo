package net.minecraft.network.chat.contents.objects;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.FontDescription;

public interface ObjectInfo {
   FontDescription fontDescription();

   String description();

   MapCodec<? extends ObjectInfo> codec();
}
