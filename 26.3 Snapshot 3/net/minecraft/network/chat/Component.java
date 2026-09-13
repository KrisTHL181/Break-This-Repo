package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.data.DataSource;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CompilableString;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   @Override
   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(final int limit) {
      StringBuilder builder = new StringBuilder();
      this.visit(contents -> {
         int remaining = limit - builder.length();
         if (remaining <= 0) {
            return STOP_ITERATION;
         }

         builder.append(contents.length() <= remaining ? contents : contents.substring(0, remaining));
         return Optional.empty();
      });
      return builder.toString();
   }

   List<Component> getSiblings();

   default @Nullable String tryCollapseToString() {
      return this.getContents() instanceof PlainTextContents text && this.getSiblings().isEmpty() && this.getStyle().isEmpty() ? text.text() : null;
   }

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   @Override
   default <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
      Style selfStyle = this.getStyle().applyTo(parentStyle);
      Optional<T> selfResult = this.getContents().visit(output, selfStyle);
      if (selfResult.isPresent()) {
         return selfResult;
      }

      for (Component sibling : this.getSiblings()) {
         Optional<T> result = sibling.visit(output, selfStyle);
         if (result.isPresent()) {
            return result;
         }
      }

      return Optional.empty();
   }

   @Override
   default <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
      Optional<T> selfResult = this.getContents().visit(output);
      if (selfResult.isPresent()) {
         return selfResult;
      }

      for (Component sibling : this.getSiblings()) {
         Optional<T> result = sibling.visit(output);
         if (result.isPresent()) {
            return result;
         }
      }

      return Optional.empty();
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(final Style rootStyle) {
      List<Component> result = Lists.newArrayList();
      this.visit((style, contents) -> {
         if (!contents.isEmpty()) {
            result.add(literal(contents).withStyle(style));
         }

         return Optional.empty();
      }, rootStyle);
      return result;
   }

   default boolean contains(final Component other) {
      if (this.equals(other)) {
         return true;
      }

      List<Component> flat = this.toFlatList();
      List<Component> otherFlat = other.toFlatList(this.getStyle());
      return Collections.indexOfSubList(flat, otherFlat) != -1;
   }

   static Component nullToEmpty(final @Nullable String text) {
      return text != null ? literal(text) : CommonComponents.EMPTY;
   }

   static MutableComponent literal(final String text) {
      return MutableComponent.create(PlainTextContents.create(text));
   }

   static MutableComponent translatable(final String key) {
      return MutableComponent.create(new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(final String key, final Object... args) {
      return MutableComponent.create(new TranslatableContents(key, null, args));
   }

   static MutableComponent translatableEscape(final String key, final Object... args) {
      for (int i = 0; i < args.length; i++) {
         Object arg = args[i];
         if (!TranslatableContents.isAllowedPrimitiveArgument(arg) && !(arg instanceof Component)) {
            args[i] = String.valueOf(arg);
         }
      }

      return translatable(key, args);
   }

   static MutableComponent translatableWithFallback(final String key, final @Nullable String fallback) {
      return MutableComponent.create(new TranslatableContents(key, fallback, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(final String key, final @Nullable String fallback, final Object... args) {
      return MutableComponent.create(new TranslatableContents(key, fallback, args));
   }

   static MutableComponent empty() {
      return MutableComponent.create(PlainTextContents.EMPTY);
   }

   static MutableComponent keybind(final String name) {
      return MutableComponent.create(new KeybindContents(name));
   }

   static MutableComponent nbt(
      final CompilableString<NbtPathArgument.NbtPath> nbtPath,
      final boolean interpreting,
      final boolean plain,
      final Optional<Component> separator,
      final DataSource dataSource
   ) {
      return MutableComponent.create(new NbtContents(nbtPath, interpreting, plain, separator, dataSource));
   }

   static MutableComponent score(final CompilableString<EntitySelector> pattern, final String objective) {
      return MutableComponent.create(new ScoreContents(Either.left(pattern), objective));
   }

   static MutableComponent score(final String name, final String objective) {
      return MutableComponent.create(new ScoreContents(Either.right(name), objective));
   }

   static MutableComponent selector(final CompilableString<EntitySelector> pattern, final Optional<Component> separator) {
      return MutableComponent.create(new SelectorContents(pattern, separator));
   }

   static MutableComponent object(final ObjectInfo info) {
      return MutableComponent.create(new ObjectContents(info, Optional.empty()));
   }

   static MutableComponent object(final ObjectInfo info, final Component fallback) {
      return MutableComponent.create(new ObjectContents(info, Optional.of(fallback)));
   }

   static Component translationArg(final Date date) {
      return literal(date.toString());
   }

   static Component translationArg(final Message message) {
      return message instanceof Component component ? component : literal(message.getString());
   }

   static Component translationArg(final UUID uuid) {
      return literal(uuid.toString());
   }

   static Component translationArg(final Identifier id) {
      return literal(id.toString());
   }

   static Component translationArg(final ChunkPos chunkPos) {
      return literal(chunkPos.toString());
   }

   static Component translationArg(final URI uri) {
      return literal(uri.toString());
   }
}
