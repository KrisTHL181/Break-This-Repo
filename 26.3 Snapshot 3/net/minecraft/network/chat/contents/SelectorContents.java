package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CompilableString;
import net.minecraft.world.entity.Entity;

public record SelectorContents(CompilableString<EntitySelector> selector, Optional<Component> separator) implements ComponentContents {
   public static final MapCodec<SelectorContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            EntitySelector.COMPILABLE_CODEC.fieldOf("selector").forGetter(SelectorContents::selector),
            ComponentSerialization.CODEC.optionalFieldOf("separator").forGetter(SelectorContents::separator)
         )
         .apply(i, SelectorContents::new)
   );

   @Override
   public MapCodec<SelectorContents> codec() {
      return MAP_CODEC;
   }

   @Override
   public MutableComponent resolve(final ResolutionContext context, final int recursionDepth) throws CommandSyntaxException {
      CommandSourceStack source = context.source();
      if (source == null) {
         return Component.empty();
      }

      Optional<? extends Component> resolvedSeparator = ComponentUtils.resolve(context, this.separator, recursionDepth);
      return ComponentUtils.formatList(this.selector.compiled().findEntities(source), resolvedSeparator, Entity::getDisplayName);
   }

   @Override
   public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style currentStyle) {
      return output.accept(currentStyle, this.selector.source());
   }

   @Override
   public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
      return output.accept(this.selector.source());
   }

   @Override
   public String toString() {
      return "pattern{" + this.selector + "}";
   }
}
