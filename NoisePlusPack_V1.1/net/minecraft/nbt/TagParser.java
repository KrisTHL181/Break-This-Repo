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
         p_389906_ -> {
            try {
               Tag tag = NBT_OPS_PARSER.parseFully(p_389906_);
               return tag instanceof CompoundTag compoundtag
                  ? DataResult.success(compoundtag, Lifecycle.stable())
                  : DataResult.error(() -> "Expected compound tag, got " + tag);
            } catch (CommandSyntaxException commandsyntaxexception) {
               return DataResult.error(commandsyntaxexception::getMessage);
            }
         },
         CompoundTag::toString
      );
   public static final Codec<CompoundTag> LENIENT_CODEC = Codec.withAlternative(FLATTENED_CODEC, CompoundTag.CODEC);
   private final DynamicOps<T> ops;
   private final Grammar<T> grammar;

   private TagParser(DynamicOps<T> p_394786_, Grammar<T> p_391349_) {
      this.ops = p_394786_;
      this.grammar = p_391349_;
   }

   public DynamicOps<T> getOps() {
      return this.ops;
   }

   public static <T> TagParser<T> create(DynamicOps<T> p_397777_) {
      return new TagParser<>(p_397777_, SnbtGrammar.createParser(p_397777_));
   }

   private static CompoundTag castToCompoundOrThrow(StringReader p_395454_, Tag p_393787_) throws CommandSyntaxException {
      if (p_393787_ instanceof CompoundTag compoundtag) {
         return compoundtag;
      } else {
         throw ERROR_EXPECTED_COMPOUND.createWithContext(p_395454_);
      }
   }

   public static CompoundTag parseCompoundFully(String p_393455_) throws CommandSyntaxException {
      StringReader stringreader = new StringReader(p_393455_);
      return castToCompoundOrThrow(stringreader, NBT_OPS_PARSER.parseFully(stringreader));
   }

   public T parseFully(String p_395844_) throws CommandSyntaxException {
      return this.parseFully(new StringReader(p_395844_));
   }

   public T parseFully(StringReader p_397863_) throws CommandSyntaxException {
      T t = this.grammar.parseForCommands(p_397863_);
      p_397863_.skipWhitespace();
      if (p_397863_.canRead()) {
         throw ERROR_TRAILING_DATA.createWithContext(p_397863_);
      } else {
         return t;
      }
   }

   public T parseAsArgument(StringReader p_394876_) throws CommandSyntaxException {
      return this.grammar.parseForCommands(p_394876_);
   }

   public static CompoundTag parseCompoundAsArgument(StringReader p_397337_) throws CommandSyntaxException {
      Tag tag = NBT_OPS_PARSER.parseAsArgument(p_397337_);
      return castToCompoundOrThrow(p_397337_, tag);
   }
}
