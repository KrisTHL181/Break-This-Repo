package net.minecraft.server.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public record ItemBody(ItemStack item, Optional<PlainMessage> description, boolean showDecorations, boolean showTooltip, int width, int height)
   implements DialogBody {
   public static final MapCodec<ItemBody> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_406954_ -> p_406954_.group(
            ItemStack.STRICT_CODEC.fieldOf("item").forGetter(ItemBody::item),
            PlainMessage.CODEC.optionalFieldOf("description").forGetter(ItemBody::description),
            Codec.BOOL.optionalFieldOf("show_decorations", true).forGetter(ItemBody::showDecorations),
            Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(ItemBody::showTooltip),
            ExtraCodecs.intRange(1, 256).optionalFieldOf("width", 16).forGetter(ItemBody::width),
            ExtraCodecs.intRange(1, 256).optionalFieldOf("height", 16).forGetter(ItemBody::height)
         )
         .apply(p_406954_, ItemBody::new)
   );

   @Override
   public MapCodec<ItemBody> mapCodec() {
      return MAP_CODEC;
   }
}
