package net.minecraft.network.chat.contents;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.network.chat.contents.objects.ObjectInfos;

public record ObjectContents(ObjectInfo contents) implements ComponentContents {
   private static final String PLACEHOLDER = Character.toString('￼');
   public static final MapCodec<ObjectContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_423545_ -> p_423545_.group(ObjectInfos.CODEC.forGetter(ObjectContents::contents)).apply(p_423545_, ObjectContents::new)
   );

   @Override
   public MapCodec<ObjectContents> codec() {
      return MAP_CODEC;
   }

   @Override
   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_426518_) {
      return p_426518_.accept(this.contents.description());
   }

   @Override
   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_422639_, Style p_423469_) {
      return p_422639_.accept(p_423469_.withFont(this.contents.fontDescription()), PLACEHOLDER);
   }
}
