package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.network.chat.contents.objects.ObjectInfos;

public record ObjectContents(ObjectInfo contents, Optional<Component> fallback) implements ComponentContents {
   private static final String PLACEHOLDER = Character.toString('￼');
   public static final MapCodec<ObjectContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            ObjectInfos.CODEC.forGetter(ObjectContents::contents), ComponentSerialization.CODEC.optionalFieldOf("fallback").forGetter(ObjectContents::fallback)
         )
         .apply(i, ObjectContents::new)
   );

   @Override
   public MapCodec<ObjectContents> codec() {
      return MAP_CODEC;
   }

   @Override
   public MutableComponent resolve(final ResolutionContext context, final int recursionDepth) throws CommandSyntaxException {
      Optional<MutableComponent> fallback = ComponentUtils.resolve(context, this.fallback, recursionDepth);
      ObjectInfo validatedContents = context.validate(this.contents);
      return validatedContents == null
         ? fallback.orElseGet(() -> Component.literal(this.contents.defaultFallback()))
         : MutableComponent.create(new ObjectContents(validatedContents, fallback.map(o -> (Component)o)));
   }

   @Override
   public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
      return this.fallback.isPresent() ? this.fallback.get().visit(output) : output.accept(this.contents.defaultFallback());
   }

   @Override
   public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style currentStyle) {
      return output.accept(currentStyle.withFont(this.contents.fontDescription()), PLACEHOLDER);
   }
}
