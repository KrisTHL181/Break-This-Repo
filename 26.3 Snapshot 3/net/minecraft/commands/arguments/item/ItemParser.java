package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.mutable.MutableObject;

public class ItemParser {
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("argument.item.id.invalid", id)
   );
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_COMPONENT = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("arguments.item.component.unknown", id)
   );
   private static final Dynamic2CommandExceptionType ERROR_MALFORMED_COMPONENT = new Dynamic2CommandExceptionType(
      (type, message) -> Component.translatableEscape("arguments.item.component.malformed", type, message)
   );
   private static final SimpleCommandExceptionType ERROR_EXPECTED_COMPONENT = new SimpleCommandExceptionType(
      Component.translatable("arguments.item.component.expected")
   );
   private static final DynamicCommandExceptionType ERROR_REPEATED_COMPONENT = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("arguments.item.component.repeated", id)
   );
   public static final char SYNTAX_START_COMPONENTS = '[';
   public static final char SYNTAX_END_COMPONENTS = ']';
   public static final char SYNTAX_COMPONENT_SEPARATOR = ',';
   public static final char SYNTAX_COMPONENT_ASSIGNMENT = '=';
   public static final char SYNTAX_REMOVED_COMPONENT = '!';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup.RegistryLookup<Item> items;
   private final RegistryOps<Tag> registryOps;
   private final TagParser<Tag> tagParser;

   public ItemParser(final HolderLookup.Provider registries) {
      this.items = registries.lookupOrThrow(Registries.ITEM);
      this.registryOps = registries.createSerializationContext(NbtOps.INSTANCE);
      this.tagParser = TagParser.create(this.registryOps);
   }

   public ItemInput parse(final StringReader reader) throws CommandSyntaxException {
      final MutableObject<Holder<Item>> itemResult = new MutableObject();
      final DataComponentPatch.Builder componentsBuilder = DataComponentPatch.builder();
      this.parse(reader, new ItemParser.Visitor() {
         @Override
         public void visitItem(final Holder<Item> item) {
            itemResult.setValue(item);
         }

         @Override
         public <T> void visitComponent(final DataComponentType<T> type, final T value) {
            componentsBuilder.set(type, value);
         }

         @Override
         public <T> void visitRemovedComponent(final DataComponentType<T> type) {
            componentsBuilder.remove(type);
         }
      });
      Holder<Item> item = Objects.requireNonNull((Holder<Item>)itemResult.get(), "Parser gave no item");
      DataComponentPatch components = componentsBuilder.build();
      return new ItemInput(item, components);
   }

   public void parse(final StringReader reader, final ItemParser.Visitor visitor) throws CommandSyntaxException {
      int cursor = reader.getCursor();

      try {
         new ItemParser.State(reader, visitor).parse();
      } catch (CommandSyntaxException e) {
         reader.setCursor(cursor);
         throw e;
      }
   }

   public CompletableFuture<Suggestions> fillSuggestions(final SuggestionsBuilder builder) {
      StringReader reader = new StringReader(builder.getInput());
      reader.setCursor(builder.getStart());
      ItemParser.SuggestionsVisitor handler = new ItemParser.SuggestionsVisitor();
      ItemParser.State state = new ItemParser.State(reader, handler);

      try {
         state.parse();
      } catch (CommandSyntaxException var6) {
      }

      return handler.resolveSuggestions(builder, reader);
   }

   private class State {
      private final StringReader reader;
      private final ItemParser.Visitor visitor;

      private State(final StringReader reader, final ItemParser.Visitor visitor) {
         this.reader = reader;
         this.visitor = visitor;
      }

      public void parse() throws CommandSyntaxException {
         this.visitor.visitSuggestions(this::suggestItem);
         this.readItem();
         this.visitor.visitSuggestions(this::suggestStartComponents);
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
            this.readComponents();
         }
      }

      private void readItem() throws CommandSyntaxException {
         int cursor = this.reader.getCursor();
         Identifier id = Identifier.read(this.reader);
         this.visitor.visitItem(ItemParser.this.items.get(ResourceKey.create(Registries.ITEM, id)).orElseThrow(() -> {
            this.reader.setCursor(cursor);
            return ItemParser.ERROR_UNKNOWN_ITEM.createWithContext(this.reader, id);
         }));
      }

      private void readComponents() throws CommandSyntaxException {
         this.reader.expect('[');
         this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
         Set<DataComponentType<?>> visitedComponents = new ReferenceArraySet();

         while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == '!') {
               this.reader.skip();
               this.visitor.visitSuggestions(this::suggestComponent);
               DataComponentType<?> componentType = readComponentType(this.reader);
               if (!visitedComponents.add(componentType)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create(componentType);
               }

               this.visitor.visitRemovedComponent(componentType);
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
            } else {
               DataComponentType<?> componentType = readComponentType(this.reader);
               if (!visitedComponents.add(componentType)) {
                  throw ItemParser.ERROR_REPEATED_COMPONENT.create(componentType);
               }

               this.visitor.visitSuggestions(this::suggestAssignment);
               this.reader.skipWhitespace();
               this.reader.expect('=');
               this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
               this.reader.skipWhitespace();
               this.readComponent(ItemParser.this.tagParser, ItemParser.this.registryOps, componentType);
               this.reader.skipWhitespace();
            }

            this.visitor.visitSuggestions(this::suggestNextOrEndComponents);
            if (!this.reader.canRead() || this.reader.peek() != ',') {
               break;
            }

            this.reader.skip();
            this.reader.skipWhitespace();
            this.visitor.visitSuggestions(this::suggestComponentAssignmentOrRemoval);
            if (!this.reader.canRead()) {
               throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(this.reader);
            }
         }

         this.reader.expect(']');
         this.visitor.visitSuggestions(ItemParser.SUGGEST_NOTHING);
      }

      public static DataComponentType<?> readComponentType(final StringReader reader) throws CommandSyntaxException {
         if (!reader.canRead()) {
            throw ItemParser.ERROR_EXPECTED_COMPONENT.createWithContext(reader);
         }

         int cursor = reader.getCursor();
         Identifier id = Identifier.read(reader);
         DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
         if (component != null && !component.isTransient()) {
            return component;
         }

         reader.setCursor(cursor);
         throw ItemParser.ERROR_UNKNOWN_COMPONENT.createWithContext(reader, id);
      }

      private <T, O> void readComponent(final TagParser<O> tagParser, final RegistryOps<O> registryOps, final DataComponentType<T> componentType) throws CommandSyntaxException {
         int cursor = this.reader.getCursor();
         O tag = tagParser.parseAsArgument(this.reader);
         DataResult<T> result = componentType.codecOrThrow().parse(registryOps, tag);
         this.visitor.visitComponent(componentType, (T)result.getOrThrow(message -> {
            this.reader.setCursor(cursor);
            return ItemParser.ERROR_MALFORMED_COMPONENT.createWithContext(this.reader, componentType.toString(), message);
         }));
      }

      private CompletableFuture<Suggestions> suggestStartComponents(final SuggestionsBuilder builder) {
         if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('['));
         }

         return builder.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestNextOrEndComponents(final SuggestionsBuilder builder) {
         if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf(','));
            builder.suggest(String.valueOf(']'));
         }

         return builder.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestAssignment(final SuggestionsBuilder builder) {
         if (builder.getRemaining().isEmpty()) {
            builder.suggest(String.valueOf('='));
         }

         return builder.buildFuture();
      }

      private CompletableFuture<Suggestions> suggestItem(final SuggestionsBuilder builder) {
         return SharedSuggestionProvider.suggestResource(ItemParser.this.items.listElementIds().map(ResourceKey::identifier), builder);
      }

      private CompletableFuture<Suggestions> suggestComponentAssignmentOrRemoval(final SuggestionsBuilder builder) {
         builder.suggest(String.valueOf('!'));
         return this.suggestComponent(builder, String.valueOf('='));
      }

      private CompletableFuture<Suggestions> suggestComponent(final SuggestionsBuilder builder) {
         return this.suggestComponent(builder, "");
      }

      private CompletableFuture<Suggestions> suggestComponent(final SuggestionsBuilder builder, final String suffix) {
         String contents = builder.getRemaining().toLowerCase(Locale.ROOT);
         SharedSuggestionProvider.filterResources(BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet(), contents, entry -> entry.getKey().identifier(), entry -> {
            DataComponentType<?> type = entry.getValue();
            if (type.codec() != null) {
               Identifier id = entry.getKey().identifier();
               builder.suggest(id + suffix);
            }
         });
         return builder.buildFuture();
      }
   }

   private static class SuggestionsVisitor implements ItemParser.Visitor {
      private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = ItemParser.SUGGEST_NOTHING;

      @Override
      public void visitSuggestions(final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions) {
         this.suggestions = suggestions;
      }

      public CompletableFuture<Suggestions> resolveSuggestions(final SuggestionsBuilder builder, final StringReader reader) {
         return this.suggestions.apply(builder.createOffset(reader.getCursor()));
      }
   }

   public interface Visitor {
      default void visitItem(final Holder<Item> item) {
      }

      default <T> void visitComponent(final DataComponentType<T> type, final T value) {
      }

      default <T> void visitRemovedComponent(final DataComponentType<T> type) {
      }

      default void visitSuggestions(final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions) {
      }
   }
}
