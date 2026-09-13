package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ComponentUtils {
   public static final String DEFAULT_SEPARATOR_TEXT = ", ";
   public static final Component DEFAULT_SEPARATOR = Component.literal(", ").withStyle(ChatFormatting.GRAY);
   public static final Component DEFAULT_NO_STYLE_SEPARATOR = Component.literal(", ");

   @CheckReturnValue
   public static MutableComponent mergeStyles(MutableComponent p_130751_, Style p_130752_) {
      if (p_130752_.isEmpty()) {
         return p_130751_;
      } else {
         Style style = p_130751_.getStyle();
         if (style.isEmpty()) {
            return p_130751_.setStyle(p_130752_);
         } else {
            return style.equals(p_130752_) ? p_130751_ : p_130751_.setStyle(style.applyTo(p_130752_));
         }
      }
   }

   @CheckReturnValue
   public static Component mergeStyles(Component p_453732_, Style p_456730_) {
      if (p_456730_.isEmpty()) {
         return p_453732_;
      } else {
         Style style = p_453732_.getStyle();
         if (style.isEmpty()) {
            return p_453732_.copy().setStyle(p_456730_);
         } else {
            return style.equals(p_456730_) ? p_453732_ : p_453732_.copy().setStyle(style.applyTo(p_456730_));
         }
      }
   }

   public static Optional<MutableComponent> updateForEntity(
      @Nullable CommandSourceStack p_178425_, Optional<Component> p_178426_, @Nullable Entity p_178427_, int p_178428_
   ) throws CommandSyntaxException {
      return p_178426_.isPresent() ? Optional.of(updateForEntity(p_178425_, p_178426_.get(), p_178427_, p_178428_)) : Optional.empty();
   }

   public static MutableComponent updateForEntity(@Nullable CommandSourceStack p_130732_, Component p_130733_, @Nullable Entity p_130734_, int p_130735_) throws CommandSyntaxException {
      if (p_130735_ > 100) {
         return p_130733_.copy();
      }

      MutableComponent mutablecomponent = p_130733_.getContents().resolve(p_130732_, p_130734_, p_130735_ + 1);

      for (Component component : p_130733_.getSiblings()) {
         mutablecomponent.append(updateForEntity(p_130732_, component, p_130734_, p_130735_ + 1));
      }

      return mutablecomponent.withStyle(resolveStyle(p_130732_, p_130733_.getStyle(), p_130734_, p_130735_));
   }

   private static Style resolveStyle(@Nullable CommandSourceStack p_130737_, Style p_130738_, @Nullable Entity p_130739_, int p_130740_) throws CommandSyntaxException {
      if (p_130738_.getHoverEvent() instanceof HoverEvent.ShowText(Component $$6)) {
         HoverEvent hoverevent = new HoverEvent.ShowText(updateForEntity(p_130737_, $$6, p_130739_, p_130740_ + 1));
         return p_130738_.withHoverEvent(hoverevent);
      } else {
         return p_130738_;
      }
   }

   public static Component formatList(Collection<String> p_130744_) {
      return formatAndSortList(p_130744_, p_130742_ -> Component.literal(p_130742_).withStyle(ChatFormatting.GREEN));
   }

   public static <T extends Comparable<T>> Component formatAndSortList(Collection<T> p_130746_, Function<T, Component> p_130747_) {
      if (p_130746_.isEmpty()) {
         return CommonComponents.EMPTY;
      }

      if (p_130746_.size() == 1) {
         return p_130747_.apply(p_130746_.iterator().next());
      }

      List<T> list = Lists.newArrayList(p_130746_);
      list.sort(Comparable::compareTo);
      return formatList(list, p_130747_);
   }

   public static <T> Component formatList(Collection<? extends T> p_178441_, Function<T, Component> p_178442_) {
      return formatList(p_178441_, DEFAULT_SEPARATOR, p_178442_);
   }

   public static <T> MutableComponent formatList(Collection<? extends T> p_178430_, Optional<? extends Component> p_178431_, Function<T, Component> p_178432_) {
      return formatList(p_178430_, (Component)DataFixUtils.orElse(p_178431_, DEFAULT_SEPARATOR), p_178432_);
   }

   public static Component formatList(Collection<? extends Component> p_178434_, Component p_178435_) {
      return formatList(p_178434_, p_178435_, Function.identity());
   }

   public static <T> MutableComponent formatList(Collection<? extends T> p_178437_, Component p_178438_, Function<T, Component> p_178439_) {
      if (p_178437_.isEmpty()) {
         return Component.empty();
      }

      if (p_178437_.size() == 1) {
         return p_178439_.apply((T)p_178437_.iterator().next()).copy();
      }

      MutableComponent mutablecomponent = Component.empty();
      boolean flag = true;

      for (T t : p_178437_) {
         if (!flag) {
            mutablecomponent.append(p_178438_);
         }

         mutablecomponent.append(p_178439_.apply(t));
         flag = false;
      }

      return mutablecomponent;
   }

   public static MutableComponent wrapInSquareBrackets(Component p_130749_) {
      return Component.translatable("chat.square_brackets", p_130749_);
   }

   public static Component fromMessage(Message p_130730_) {
      return p_130730_ instanceof Component component ? component : Component.literal(p_130730_.getString());
   }

   public static boolean isTranslationResolvable(@Nullable Component p_237135_) {
      if (p_237135_ != null && p_237135_.getContents() instanceof TranslatableContents translatablecontents) {
         String s1 = translatablecontents.getKey();
         String s = translatablecontents.getFallback();
         return s != null || Language.getInstance().has(s1);
      } else {
         return true;
      }
   }

   public static MutableComponent copyOnClickText(String p_260039_) {
      return wrapInSquareBrackets(
         Component.literal(p_260039_)
            .withStyle(
               p_389914_ -> p_389914_.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent.CopyToClipboard(p_260039_))
                  .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click")))
                  .withInsertion(p_260039_)
            )
      );
   }
}
