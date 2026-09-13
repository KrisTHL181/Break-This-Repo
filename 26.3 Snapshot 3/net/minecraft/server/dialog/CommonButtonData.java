package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record CommonButtonData(Component label, Optional<Component> tooltip, int width) {
   public static final int DEFAULT_WIDTH = 150;
   public static final MapCodec<CommonButtonData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            ComponentSerialization.CODEC.fieldOf("label").forGetter(CommonButtonData::label),
            ComponentSerialization.CODEC.optionalFieldOf("tooltip").forGetter(CommonButtonData::tooltip),
            Dialog.WIDTH_CODEC.optionalFieldOf("width", 150).forGetter(CommonButtonData::width)
         )
         .apply(i, CommonButtonData::new)
   );

   public CommonButtonData(final Component label, final int width) {
      this(label, Optional.empty(), width);
   }
}
