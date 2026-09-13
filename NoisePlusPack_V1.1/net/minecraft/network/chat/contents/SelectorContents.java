package net.minecraft.network.chat.contents;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public record SelectorContents(SelectorPattern selector, Optional<Component> separator) implements ComponentContents {
   public static final MapCodec<SelectorContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_358480_ -> p_358480_.group(
            SelectorPattern.CODEC.fieldOf("selector").forGetter(SelectorContents::selector),
            ComponentSerialization.CODEC.optionalFieldOf("separator").forGetter(SelectorContents::separator)
         )
         .apply(p_358480_, SelectorContents::new)
   );

   @Override
   public MapCodec<SelectorContents> codec() {
      return MAP_CODEC;
   }

   @Override
   public MutableComponent resolve(@Nullable CommandSourceStack p_237468_, @Nullable Entity p_237469_, int p_237470_) throws CommandSyntaxException {
      if (p_237468_ == null) {
         return Component.empty();
      }

      Optional<? extends Component> optional = ComponentUtils.updateForEntity(p_237468_, this.separator, p_237469_, p_237470_);
      return ComponentUtils.formatList(this.selector.resolved().findEntities(p_237468_), optional, Entity::getDisplayName);
   }

   @Override
   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> p_237476_, Style p_237477_) {
      return p_237476_.accept(p_237477_, this.selector.pattern());
   }

   @Override
   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> p_237474_) {
      return p_237474_.accept(this.selector.pattern());
   }

   @Override
   public String toString() {
      return "pattern{" + this.selector + "}";
   }
}
