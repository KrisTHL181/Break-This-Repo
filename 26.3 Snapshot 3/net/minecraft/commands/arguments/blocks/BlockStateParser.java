package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockStateParser {
   public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(
      Component.translatable("argument.block.tag.disallowed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("argument.block.id.invalid", id)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType(
      (block, property) -> Component.translatableEscape("argument.block.property.unknown", block, property)
   );
   public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType(
      (block, property) -> Component.translatableEscape("argument.block.property.duplicate", property, block)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType(
      (block, property, value) -> Component.translatableEscape("argument.block.property.invalid", block, value, property)
   );
   public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType(
      (block, property) -> Component.translatableEscape("argument.block.property.novalue", property, block)
   );
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(
      Component.translatable("argument.block.property.unclosed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      tag -> Component.translatableEscape("arguments.block.tag.unknown", tag)
   );
   private static final char SYNTAX_START_PROPERTIES = '[';
   private static final char SYNTAX_START_NBT = '{';
   private static final char SYNTAX_END_PROPERTIES = ']';
   private static final char SYNTAX_EQUALS = '=';
   private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
   private static final char SYNTAX_TAG = '#';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup<Block> blocks;
   private final StringReader reader;
   private final boolean forTesting;
   private final boolean allowNbt;
   private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
   private final Map<String, String> vagueProperties = Maps.newHashMap();
   private Identifier id = Identifier.withDefaultNamespace("");
   private @Nullable StateDefinition<Block, BlockState> definition;
   private @Nullable BlockState state;
   private @Nullable CompoundTag nbt;
   private @Nullable HolderSet<Block> tag;
   private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

   private BlockStateParser(final HolderLookup<Block> blocks, final StringReader reader, final boolean forTesting, final boolean allowNbt) {
      this.blocks = blocks;
      this.reader = reader;
      this.forTesting = forTesting;
      this.allowNbt = allowNbt;
   }

   public static BlockStateParser.BlockResult parseForBlock(final HolderLookup<Block> blocks, final String value, final boolean allowNbt) throws CommandSyntaxException {
      return parseForBlock(blocks, new StringReader(value), allowNbt);
   }

   public static BlockStateParser.BlockResult parseForBlock(final HolderLookup<Block> blocks, final StringReader reader, final boolean allowNbt) throws CommandSyntaxException {
      int cursor = reader.getCursor();

      try {
         BlockStateParser parser = new BlockStateParser(blocks, reader, false, allowNbt);
         parser.parse();
         return new BlockStateParser.BlockResult(parser.state, parser.properties, parser.nbt);
      } catch (CommandSyntaxException e) {
         reader.setCursor(cursor);
         throw e;
      }
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      final HolderLookup<Block> blocks, final String value, final boolean allowNbt
   ) throws CommandSyntaxException {
      return parseForTesting(blocks, new StringReader(value), allowNbt);
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      final HolderLookup<Block> blocks, final StringReader reader, final boolean allowNbt
   ) throws CommandSyntaxException {
      int cursor = reader.getCursor();

      try {
         BlockStateParser parser = new BlockStateParser(blocks, reader, true, allowNbt);
         parser.parse();
         return parser.tag != null
            ? Either.right(new BlockStateParser.TagResult(parser.tag, parser.vagueProperties, parser.nbt))
            : Either.left(new BlockStateParser.BlockResult(parser.state, parser.properties, parser.nbt));
      } catch (CommandSyntaxException e) {
         reader.setCursor(cursor);
         throw e;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(
      final HolderLookup<Block> blocks, final SuggestionsBuilder builder, final boolean forTesting, final boolean allowNbt
   ) {
      StringReader reader = new StringReader(builder.getInput());
      reader.setCursor(builder.getStart());
      BlockStateParser parser = new BlockStateParser(blocks, reader, forTesting, allowNbt);

      try {
         parser.parse();
      } catch (CommandSyntaxException var7) {
      }

      return parser.suggestions.apply(builder.createOffset(reader.getCursor()));
   }

   private void parse() throws CommandSyntaxException {
      if (this.forTesting) {
         this.suggestions = this::suggestBlockIdOrTag;
      } else {
         this.suggestions = this::suggestItem;
      }

      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.readTag();
         this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readVagueProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      } else {
         this.readBlock();
         this.suggestions = this::suggestOpenPropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      }

      if (this.allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
         this.suggestions = SUGGEST_NOTHING;
         this.readNbt();
      }
   }

   private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty()) {
         builder.suggest(String.valueOf(']'));
      }

      return this.suggestPropertyName(builder);
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty()) {
         builder.suggest(String.valueOf(']'));
      }

      return this.suggestVaguePropertyName(builder);
   }

   private CompletableFuture<Suggestions> suggestPropertyName(final SuggestionsBuilder builder) {
      String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);

      for (Property<?> property : this.state.getProperties()) {
         if (!this.properties.containsKey(property) && property.getName().startsWith(prefix)) {
            builder.suggest(property.getName() + "=");
         }
      }

      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyName(final SuggestionsBuilder builder) {
      String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
      if (this.tag != null) {
         for (Holder<Block> block : this.tag) {
            for (Property<?> property : block.value().getStateDefinition().getProperties()) {
               if (!this.vagueProperties.containsKey(property.getName()) && property.getName().startsWith(prefix)) {
                  builder.suggest(property.getName() + "=");
               }
            }
         }
      }

      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty() && this.hasBlockEntity()) {
         builder.suggest(String.valueOf('{'));
      }

      return builder.buildFuture();
   }

   private boolean hasBlockEntity() {
      if (this.state != null) {
         return this.state.hasBlockEntity();
      }

      if (this.tag != null) {
         for (Holder<Block> block : this.tag) {
            if (block.value().defaultBlockState().hasBlockEntity()) {
               return true;
            }
         }
      }

      return false;
   }

   private CompletableFuture<Suggestions> suggestEquals(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty()) {
         builder.suggest(String.valueOf('='));
      }

      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty()) {
         builder.suggest(String.valueOf(']'));
      }

      if (builder.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
         builder.suggest(String.valueOf(','));
      }

      return builder.buildFuture();
   }

   private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(final SuggestionsBuilder builder, final Property<T> property) {
      for (T value : property.getPossibleValues()) {
         if (value instanceof Integer v) {
            builder.suggest(v);
         } else {
            builder.suggest(property.getName(value));
         }
      }

      return builder;
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyValue(final SuggestionsBuilder builder, final String key) {
      boolean hasMoreProperties = false;
      if (this.tag != null) {
         for (Holder<Block> blockHolder : this.tag) {
            Block block = blockHolder.value();
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property != null) {
               addSuggestions(builder, property);
            }

            if (!hasMoreProperties) {
               for (Property<?> prop : block.getStateDefinition().getProperties()) {
                  if (!this.vagueProperties.containsKey(prop.getName())) {
                     hasMoreProperties = true;
                     break;
                  }
               }
            }
         }
      }

      if (hasMoreProperties) {
         builder.suggest(String.valueOf(','));
      }

      builder.suggest(String.valueOf(']'));
      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty() && this.tag != null) {
         boolean hasProperties = false;
         boolean hasEntity = false;

         for (Holder<Block> blockHolder : this.tag) {
            Block block = blockHolder.value();
            hasProperties |= !block.getStateDefinition().getProperties().isEmpty();
            hasEntity |= block.defaultBlockState().hasBlockEntity();
            if (hasProperties && hasEntity) {
               break;
            }
         }

         if (hasProperties) {
            builder.suggest(String.valueOf('['));
         }

         if (hasEntity) {
            builder.suggest(String.valueOf('{'));
         }
      }

      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(final SuggestionsBuilder builder) {
      if (builder.getRemaining().isEmpty()) {
         if (!this.definition.getProperties().isEmpty()) {
            builder.suggest(String.valueOf('['));
         }

         if (this.state.hasBlockEntity()) {
            builder.suggest(String.valueOf('{'));
         }
      }

      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), builder, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(final SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::identifier), builder);
   }

   private CompletableFuture<Suggestions> suggestBlockIdOrTag(final SuggestionsBuilder builder) {
      this.suggestTag(builder);
      this.suggestItem(builder);
      return builder.buildFuture();
   }

   private void readBlock() throws CommandSyntaxException {
      int start = this.reader.getCursor();
      this.id = Identifier.read(this.reader);
      Block block = this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
         this.reader.setCursor(start);
         return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
      }).value();
      this.definition = block.getStateDefinition();
      this.state = block.defaultBlockState();
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.forTesting) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      }

      int start = this.reader.getCursor();
      this.reader.expect('#');
      this.suggestions = this::suggestTag;
      Identifier id = Identifier.read(this.reader);
      this.tag = this.blocks.get(TagKey.create(Registries.BLOCK, id)).orElseThrow(() -> {
         this.reader.setCursor(start);
         return ERROR_UNKNOWN_TAG.createWithContext(this.reader, id.toString());
      });
   }

   private void readProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestPropertyNameOrEnd;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int keyStart = this.reader.getCursor();
         String key = this.reader.readString();
         Property<?> property = this.definition.getProperty(key);
         if (property == null) {
            this.reader.setCursor(keyStart);
            throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), key);
         }

         if (this.properties.containsKey(property)) {
            this.reader.setCursor(keyStart);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), key);
         }

         this.reader.skipWhitespace();
         this.suggestions = this::suggestEquals;
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), key);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = builder -> addSuggestions(builder, property).buildFuture();
         int start = this.reader.getCursor();
         this.setValue(property, this.reader.readString(), start);
         this.suggestions = this::suggestNextPropertyOrEnd;
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestPropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readVagueProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestVaguePropertyNameOrEnd;
      int valueStart = -1;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int keyStart = this.reader.getCursor();
         String key = this.reader.readString();
         if (this.vagueProperties.containsKey(key)) {
            this.reader.setCursor(keyStart);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), key);
         }

         this.reader.skipWhitespace();
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            this.reader.setCursor(keyStart);
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), key);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = builder -> this.suggestVaguePropertyValue(builder, key);
         valueStart = this.reader.getCursor();
         String value = this.reader.readString();
         this.vagueProperties.put(key, value);
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            valueStart = -1;
            if (this.reader.peek() != ',') {
               if (this.reader.peek() != ']') {
                  throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
               }
               break;
            }

            this.reader.skip();
            this.suggestions = this::suggestVaguePropertyName;
         }
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         if (valueStart >= 0) {
            this.reader.setCursor(valueStart);
         }

         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = TagParser.parseCompoundAsArgument(this.reader);
   }

   private <T extends Comparable<T>> void setValue(final Property<T> property, final String raw, final int start) throws CommandSyntaxException {
      Optional<T> value = property.getValue(raw);
      if (value.isPresent()) {
         this.state = this.state.setValue(property, value.get());
         this.properties.put(property, value.get());
      } else {
         this.reader.setCursor(start);
         throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), property.getName(), raw);
      }
   }

   public static String serialize(final BlockState state) {
      final StringBuilder result = new StringBuilder(state.typeHolder().unwrapKey().map(r -> r.identifier().toString()).orElse("air"));
      if (!state.isSingletonState()) {
         result.append('[');
         state.getValues().forEach(new Consumer<Property.Value<?>>() {
            private boolean separate = false;

            public void accept(final Property.Value<?> value) {
               if (this.separate) {
                  result.append(',');
               }

               result.append(value.property().getName());
               result.append('=');
               result.append(value.valueName());
               this.separate = true;
            }
         });
         result.append(']');
      }

      return result.toString();
   }

   public record BlockResult(BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
   }

   public record TagResult(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
   }
}
