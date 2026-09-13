package net.minecraft.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.Grammar;

public class TagParser<T> {
   public static final SimpleCommandExceptionType ERROR_TRAILING_DATA = new SimpleCommandExceptionType(Component.translatable("argument.nbt.trailing"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_COMPOUND = new SimpleCommandExceptionType(
      Component.translatable("argument.nbt.expected.compound")
   );
   public static final char ELEMENT_SEPARATOR = ',';
   public static final char NAME_VALUE_SEPARATOR = ':';
   private static final TagParser<Tag> NBT_OPS_PARSER = create(NbtOps.INSTANCE);
   public static final Codec<CompoundTag> FLATTENED_CODEC = Codec.STRING
      .comapFlatMap(
         s -> {
            try {
               Tag result = NBT_OPS_PARSER.parseFully(s);
               return result instanceof CompoundTag compoundTag
                  ? DataResult.success(compoundTag, Lifecycle.stable())
                  : DataResult.error(() -> "Expected compound tag, got " + result);
            } catch (CommandSyntaxException e) {
               return DataResult.error(e::getMessage);
            }
         },
         CompoundTag::toString
      );
   public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
   private final DynamicOps<T> ops;
   private final Grammar<T> grammar;

   private TagParser(final DynamicOps<T> ops, final Grammar<T> grammar) {
      this.ops = ops;
      this.grammar = grammar;
   }

   public DynamicOps<T> getOps() {
      return this.ops;
   }

   public static <T> TagParser<T> create(final DynamicOps<T> ops) {
      return new TagParser<>(ops, SnbtGrammar.createParser(ops));
   }

   private static CompoundTag castToCompoundOrThrow(final StringReader reader, final Tag result) throws CommandSyntaxException {
      if (result instanceof CompoundTag compoundTag) {
         return compoundTag;
      } else {
         throw ERROR_EXPECTED_COMPOUND.createWithContext(reader);
      }
   }

   public static CompoundTag parseCompoundFully(final String input) throws CommandSyntaxException {
      StringReader reader = new StringReader(input);
      return castToCompoundOrThrow(reader, NBT_OPS_PARSER.parseFully(reader));
   }

   public T parseFully(final String input) throws CommandSyntaxException {
      return this.parseFully(new StringReader(input));
   }

   public T parseFully(final StringReader reader) throws CommandSyntaxException {
      T result = this.grammar.parseForCommands(reader);
      reader.skipWhitespace();
      if (reader.canRead()) {
         throw ERROR_TRAILING_DATA.createWithContext(reader);
      } else {
         return result;
      }
   }

   public T parseAsArgument(final StringReader reader) throws CommandSyntaxException {
      return this.grammar.parseForCommands(reader);
   }

   public static CompoundTag parseCompoundAsArgument(final StringReader reader) throws CommandSyntaxException {
      Tag result = NBT_OPS_PARSER.parseAsArgument(reader);
      return castToCompoundOrThrow(reader, result);
   }
}
