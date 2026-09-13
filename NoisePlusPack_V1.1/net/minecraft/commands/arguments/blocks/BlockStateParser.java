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
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
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
      p_308397_ -> Component.translatableEscape("argument.block.id.invalid", p_308397_)
   );
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType(
      (p_308394_, p_308395_) -> Component.translatableEscape("argument.block.property.unknown", p_308394_, p_308395_)
   );
   public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType(
      (p_308398_, p_308399_) -> Component.translatableEscape("argument.block.property.duplicate", p_308399_, p_308398_)
   );
   public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType(
      (p_308391_, p_308392_, p_308393_) -> Component.translatableEscape("argument.block.property.invalid", p_308391_, p_308393_, p_308392_)
   );
   public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType(
      (p_308400_, p_308401_) -> Component.translatableEscape("argument.block.property.novalue", p_308401_, p_308400_)
   );
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(
      Component.translatable("argument.block.property.unclosed")
   );
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(
      p_308396_ -> Component.translatableEscape("arguments.block.tag.unknown", p_308396_)
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

   private BlockStateParser(HolderLookup<Block> p_234673_, StringReader p_234674_, boolean p_234675_, boolean p_234676_) {
      this.blocks = p_234673_;
      this.reader = p_234674_;
      this.forTesting = p_234675_;
      this.allowNbt = p_234676_;
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> p_251394_, String p_248677_, boolean p_250430_) throws CommandSyntaxException {
      return parseForBlock(p_251394_, new StringReader(p_248677_), p_250430_);
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> p_234692_, StringReader p_234693_, boolean p_234694_) throws CommandSyntaxException {
      int i = p_234693_.getCursor();

      try {
         BlockStateParser blockstateparser = new BlockStateParser(p_234692_, p_234693_, false, p_234694_);
         blockstateparser.parse();
         return new BlockStateParser.BlockResult(blockstateparser.state, blockstateparser.properties, blockstateparser.nbt);
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_234693_.setCursor(i);
         throw commandsyntaxexception;
      }
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      HolderLookup<Block> p_252082_, String p_251830_, boolean p_249125_
   ) throws CommandSyntaxException {
      return parseForTesting(p_252082_, new StringReader(p_251830_), p_249125_);
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(
      HolderLookup<Block> p_234717_, StringReader p_234718_, boolean p_234719_
   ) throws CommandSyntaxException {
      int i = p_234718_.getCursor();

      try {
         BlockStateParser blockstateparser = new BlockStateParser(p_234717_, p_234718_, true, p_234719_);
         blockstateparser.parse();
         return blockstateparser.tag != null
            ? Either.right(new BlockStateParser.TagResult(blockstateparser.tag, blockstateparser.vagueProperties, blockstateparser.nbt))
            : Either.left(new BlockStateParser.BlockResult(blockstateparser.state, blockstateparser.properties, blockstateparser.nbt));
      } catch (CommandSyntaxException commandsyntaxexception) {
         p_234718_.setCursor(i);
         throw commandsyntaxexception;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(
      HolderLookup<Block> p_234696_, SuggestionsBuilder p_234697_, boolean p_234698_, boolean p_234699_
   ) {
      StringReader stringreader = new StringReader(p_234697_.getInput());
      stringreader.setCursor(p_234697_.getStart());
      BlockStateParser blockstateparser = new BlockStateParser(p_234696_, stringreader, p_234698_, p_234699_);

      try {
         blockstateparser.parse();
      } catch (CommandSyntaxException var7) {
      }

      return blockstateparser.suggestions.apply(p_234697_.createOffset(stringreader.getCursor()));
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

   private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder p_234684_) {
      if (p_234684_.getRemaining().isEmpty()) {
         p_234684_.suggest(String.valueOf(']'));
      }

      return this.suggestPropertyName(p_234684_);
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder p_234715_) {
      if (p_234715_.getRemaining().isEmpty()) {
         p_234715_.suggest(String.valueOf(']'));
      }

      return this.suggestVaguePropertyName(p_234715_);
   }

   private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder p_234729_) {
      String s = p_234729_.getRemaining().toLowerCase(Locale.ROOT);

      for (Property<?> property : this.state.getProperties()) {
         if (!this.properties.containsKey(property) && property.getName().startsWith(s)) {
            p_234729_.suggest(property.getName() + "=");
         }
      }

      return p_234729_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder p_234731_) {
      String s = p_234731_.getRemaining().toLowerCase(Locale.ROOT);
      if (this.tag != null) {
         for (Holder<Block> holder : this.tag) {
            for (Property<?> property : holder.value().getStateDefinition().getProperties()) {
               if (!this.vagueProperties.containsKey(property.getName()) && property.getName().startsWith(s)) {
                  p_234731_.suggest(property.getName() + "=");
               }
            }
         }
      }

      return p_234731_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder p_234733_) {
      if (p_234733_.getRemaining().isEmpty() && this.hasBlockEntity()) {
         p_234733_.suggest(String.valueOf('{'));
      }

      return p_234733_.buildFuture();
   }

   private boolean hasBlockEntity() {
      if (this.state != null) {
         return this.state.hasBlockEntity();
      }

      if (this.tag != null) {
         for (Holder<Block> holder : this.tag) {
            if (holder.value().defaultBlockState().hasBlockEntity()) {
               return true;
            }
         }
      }

      return false;
   }

   private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder p_234735_) {
      if (p_234735_.getRemaining().isEmpty()) {
         p_234735_.suggest(String.valueOf('='));
      }

      return p_234735_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder p_234737_) {
      if (p_234737_.getRemaining().isEmpty()) {
         p_234737_.suggest(String.valueOf(']'));
      }

      if (p_234737_.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
         p_234737_.suggest(String.valueOf(','));
      }

      return p_234737_.buildFuture();
   }

   private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder p_116787_, Property<T> p_116788_) {
      for (T t : p_116788_.getPossibleValues()) {
         if (t instanceof Integer integer) {
            p_116787_.suggest(integer);
         } else {
            p_116787_.suggest(p_116788_.getName(t));
         }
      }

      return p_116787_;
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder p_234686_, String p_234687_) {
      boolean flag = false;
      if (this.tag != null) {
         for (Holder<Block> holder : this.tag) {
            Block block = holder.value();
            Property<?> property = block.getStateDefinition().getProperty(p_234687_);
            if (property != null) {
               addSuggestions(p_234686_, property);
            }

            if (!flag) {
               for (Property<?> property1 : block.getStateDefinition().getProperties()) {
                  if (!this.vagueProperties.containsKey(property1.getName())) {
                     flag = true;
                     break;
                  }
               }
            }
         }
      }

      if (flag) {
         p_234686_.suggest(String.valueOf(','));
      }

      p_234686_.suggest(String.valueOf(']'));
      return p_234686_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder p_234739_) {
      if (p_234739_.getRemaining().isEmpty() && this.tag != null) {
         boolean flag = false;
         boolean flag1 = false;

         for (Holder<Block> holder : this.tag) {
            Block block = holder.value();
            flag |= !block.getStateDefinition().getProperties().isEmpty();
            flag1 |= block.defaultBlockState().hasBlockEntity();
            if (flag && flag1) {
               break;
            }
         }

         if (flag) {
            p_234739_.suggest(String.valueOf('['));
         }

         if (flag1) {
            p_234739_.suggest(String.valueOf('{'));
         }
      }

      return p_234739_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder p_234741_) {
      if (p_234741_.getRemaining().isEmpty()) {
         if (!this.definition.getProperties().isEmpty()) {
            p_234741_.suggest(String.valueOf('['));
         }

         if (this.state.hasBlockEntity()) {
            p_234741_.suggest(String.valueOf('{'));
         }
      }

      return p_234741_.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder p_234743_) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), p_234743_, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder p_234745_) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::identifier), p_234745_);
   }

   private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder p_234747_) {
      this.suggestTag(p_234747_);
      this.suggestItem(p_234747_);
      return p_234747_.buildFuture();
   }

   private void readBlock() throws CommandSyntaxException {
      int i = this.reader.getCursor();
      this.id = Identifier.read(this.reader);
      Block block = this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
         this.reader.setCursor(i);
         return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
      }).value();
      this.definition = block.getStateDefinition();
      this.state = block.defaultBlockState();
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.forTesting) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      }

      int i = this.reader.getCursor();
      this.reader.expect('#');
      this.suggestions = this::suggestTag;
      Identifier identifier = Identifier.read(this.reader);
      this.tag = this.blocks.get(TagKey.create(Registries.BLOCK, identifier)).orElseThrow(() -> {
         this.reader.setCursor(i);
         return ERROR_UNKNOWN_TAG.createWithContext(this.reader, identifier.toString());
      });
   }

   private void readProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestPropertyNameOrEnd;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int i = this.reader.getCursor();
         String s = this.reader.readString();
         Property<?> property = this.definition.getProperty(s);
         if (property == null) {
            this.reader.setCursor(i);
            throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
         }

         if (this.properties.containsKey(property)) {
            this.reader.setCursor(i);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
         }

         this.reader.skipWhitespace();
         this.suggestions = this::suggestEquals;
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), s);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = p_234690_ -> addSuggestions(p_234690_, property).buildFuture();
         int j = this.reader.getCursor();
         this.setValue(property, this.reader.readString(), j);
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
      int i = -1;
      this.reader.skipWhitespace();

      while (this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int j = this.reader.getCursor();
         String s = this.reader.readString();
         if (this.vagueProperties.containsKey(s)) {
            this.reader.setCursor(j);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), s);
         }

         this.reader.skipWhitespace();
         if (!this.reader.canRead() || this.reader.peek() != '=') {
            this.reader.setCursor(j);
            throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), s);
         }

         this.reader.skip();
         this.reader.skipWhitespace();
         this.suggestions = p_234712_ -> this.suggestVaguePropertyValue(p_234712_, s);
         i = this.reader.getCursor();
         String s1 = this.reader.readString();
         this.vagueProperties.put(s, s1);
         this.reader.skipWhitespace();
         if (this.reader.canRead()) {
            i = -1;
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
         if (i >= 0) {
            this.reader.setCursor(i);
         }

         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = TagParser.parseCompoundAsArgument(this.reader);
   }

   private <T extends Comparable<T>> void setValue(Property<T> p_116776_, String p_116777_, int p_116778_) throws CommandSyntaxException {
      Optional<T> optional = p_116776_.getValue(p_116777_);
      if (optional.isPresent()) {
         this.state = this.state.setValue(p_116776_, optional.get());
         this.properties.put(p_116776_, optional.get());
      } else {
         this.reader.setCursor(p_116778_);
         throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), p_116776_.getName(), p_116777_);
      }
   }

   public static String serialize(BlockState p_116770_) {
      StringBuilder stringbuilder = new StringBuilder(p_116770_.getBlockHolder().unwrapKey().map(p_448510_ -> p_448510_.identifier().toString()).orElse("air"));
      if (!p_116770_.getProperties().isEmpty()) {
         stringbuilder.append('[');
         boolean flag = false;

         for (Entry<Property<?>, Comparable<?>> entry : p_116770_.getValues().entrySet()) {
            if (flag) {
               stringbuilder.append(',');
            }

            appendProperty(stringbuilder, entry.getKey(), entry.getValue());
            flag = true;
         }

         stringbuilder.append(']');
      }

      return stringbuilder.toString();
   }

   private static <T extends Comparable<T>> void appendProperty(StringBuilder p_116803_, Property<T> p_116804_, Comparable<?> p_116805_) {
      p_116803_.append(p_116804_.getName());
      p_116803_.append('=');
      p_116803_.append(p_116804_.getName((T)p_116805_));
   }

   public record BlockResult(BlockState blockState, Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag nbt) {
   }

   public record TagResult(HolderSet<Block> tag, Map<String, String> vagueProperties, @Nullable CompoundTag nbt) {
   }
}
