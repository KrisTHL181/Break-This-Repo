package net.minecraft.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.chars.CharList;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.GreedyPatternParseRule;
import net.minecraft.util.parsing.packrat.commands.GreedyPredicateParseRule;
import net.minecraft.util.parsing.packrat.commands.NumberRunParseRule;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import net.minecraft.util.parsing.packrat.commands.UnquotedStringParseRule;
import org.jspecify.annotations.Nullable;

public class SnbtGrammar {
   private static final DynamicCommandExceptionType ERROR_NUMBER_PARSE_FAILURE = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("snbt.parser.number_parse_failure", message)
   );
   private static final DynamicCommandExceptionType ERROR_EXPECTED_HEX_ESCAPE = new DynamicCommandExceptionType(
      length -> Component.translatableEscape("snbt.parser.expected_hex_escape", length)
   );
   private static final DynamicCommandExceptionType ERROR_INVALID_CODEPOINT = new DynamicCommandExceptionType(
      codepoint -> Component.translatableEscape("snbt.parser.invalid_codepoint", codepoint)
   );
   private static final DynamicCommandExceptionType ERROR_NO_SUCH_OPERATION = new DynamicCommandExceptionType(
      operation -> Component.translatableEscape("snbt.parser.no_such_operation", operation)
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_INTEGER_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_integer_type"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_FLOAT_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_float_type"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_NON_NEGATIVE_NUMBER = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_non_negative_number"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_CHARACTER_NAME = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_character_name"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_ARRAY_ELEMENT_TYPE = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_array_element_type"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_UNQUOTED_START = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_unquoted_start"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_UNQUOTED_STRING = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_unquoted_string"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INVALID_STRING_CONTENTS = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.invalid_string_contents"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_BINARY_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_binary_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_UNDESCORE_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.underscore_not_allowed"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_DECIMAL_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_decimal_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EXPECTED_HEX_NUMERAL = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.expected_hex_numeral"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_EMPTY_KEY = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.empty_key"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_LEADING_ZERO_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.leading_zero_not_allowed"))
   );
   private static final DelayedException<CommandSyntaxException> ERROR_INFINITY_NOT_ALLOWED = DelayedException.create(
      new SimpleCommandExceptionType(Component.translatable("snbt.parser.infinity_not_allowed"))
   );
   private static final HexFormat HEX_ESCAPE = HexFormat.of().withUpperCase();
   private static final NumberRunParseRule BINARY_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_BINARY_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(final char c) {
         return switch (c) {
            case '0', '1', '_' -> true;
            default -> false;
         };
      }
   };
   private static final NumberRunParseRule DECIMAL_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_DECIMAL_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(final char c) {
         return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_' -> true;
            default -> false;
         };
      }
   };
   private static final NumberRunParseRule HEX_NUMERAL = new NumberRunParseRule(ERROR_EXPECTED_HEX_NUMERAL, ERROR_UNDESCORE_NOT_ALLOWED) {
      @Override
      protected boolean isAccepted(final char c) {
         return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', '_', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
            default -> false;
         };
      }
   };
   private static final GreedyPredicateParseRule PLAIN_STRING_CHUNK = new GreedyPredicateParseRule(1, ERROR_INVALID_STRING_CONTENTS) {
      @Override
      protected boolean isAccepted(final char c) {
         return switch (c) {
            case '"', '\'', '\\' -> false;
            default -> true;
         };
      }
   };
   private static final StringReaderTerms.TerminalCharacters NUMBER_LOOKEAHEAD = new StringReaderTerms.TerminalCharacters(CharList.of()) {
      @Override
      protected boolean isAccepted(final char c) {
         return SnbtGrammar.canStartNumber(c);
      }
   };
   private static final Pattern UNICODE_NAME = Pattern.compile("[-a-zA-Z0-9 ]+");

   private static DelayedException<CommandSyntaxException> createNumberParseError(final NumberFormatException ex) {
      return DelayedException.create(ERROR_NUMBER_PARSE_FAILURE, ex.getMessage());
   }

   public static @Nullable String escapeControlCharacters(final char c) {
      return switch (c) {
         case '\b' -> "b";
         case '\t' -> "t";
         case '\n' -> "n";
         default -> c < ' ' ? "x" + HEX_ESCAPE.toHexDigits((byte)c) : null;
         case '\f' -> "f";
         case '\r' -> "r";
      };
   }

   private static boolean isAllowedToStartUnquotedString(final char c) {
      return !canStartNumber(c);
   }

   private static boolean canStartNumber(final char c) {
      return switch (c) {
         case '+', '-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
         default -> false;
      };
   }

   private static boolean needsUnderscoreRemoval(final String contents) {
      return contents.indexOf(95) != -1;
   }

   private static void cleanAndAppend(final StringBuilder output, final String contents) {
      cleanAndAppend(output, contents, needsUnderscoreRemoval(contents));
   }

   private static void cleanAndAppend(final StringBuilder output, final String contents, final boolean needsUnderscoreRemoval) {
      if (needsUnderscoreRemoval) {
         for (char c : contents.toCharArray()) {
            if (c != '_') {
               output.append(c);
            }
         }
      } else {
         output.append(contents);
      }
   }

   private static short parseUnsignedShort(final String string, final int radix) {
      int parse = Integer.parseInt(string, radix);
      if (parse >> 16 == 0) {
         return (short)parse;
      } else {
         throw new NumberFormatException("out of range: " + parse);
      }
   }

   private static <T> @Nullable T createFloat(
      final DynamicOps<T> ops,
      final SnbtGrammar.Sign sign,
      final @Nullable String whole,
      final @Nullable String fraction,
      final SnbtGrammar.@Nullable Signed<String> exponent,
      final SnbtGrammar.@Nullable TypeSuffix typeSuffix,
      final ParseState<?> state
   ) {
      StringBuilder result = new StringBuilder();
      sign.append(result);
      if (whole != null) {
         cleanAndAppend(result, whole);
      }

      if (fraction != null) {
         result.append('.');
         cleanAndAppend(result, fraction);
      }

      if (exponent != null) {
         result.append('e');
         exponent.sign().append(result);
         cleanAndAppend(result, exponent.value);
      }

      try {
         String contents = result.toString();

         return (T)(switch (typeSuffix) {
            case null -> convertDouble(ops, state, contents);
            case FLOAT -> convertFloat(ops, state, contents);
            case DOUBLE -> convertDouble(ops, state, contents);
            default -> {
               state.errorCollector().store(state.mark(), ERROR_EXPECTED_FLOAT_TYPE);
               yield null;
            }
         });
      } catch (NumberFormatException e) {
         state.errorCollector().store(state.mark(), createNumberParseError(e));
         return null;
      }
   }

   private static <T> @Nullable T convertFloat(final DynamicOps<T> ops, final ParseState<?> state, final String contents) {
      float value = Float.parseFloat(contents);
      if (!Float.isFinite(value)) {
         state.errorCollector().store(state.mark(), ERROR_INFINITY_NOT_ALLOWED);
         return null;
      } else {
         return (T)ops.createFloat(value);
      }
   }

   private static <T> @Nullable T convertDouble(final DynamicOps<T> ops, final ParseState<?> state, final String contents) {
      double value = Double.parseDouble(contents);
      if (!Double.isFinite(value)) {
         state.errorCollector().store(state.mark(), ERROR_INFINITY_NOT_ALLOWED);
         return null;
      } else {
         return (T)ops.createDouble(value);
      }
   }

   private static String joinList(final List<String> list) {
      return switch (list.size()) {
         case 0 -> "";
         case 1 -> (String)list.getFirst();
         default -> String.join("", list);
      };
   }

   public static <T> Grammar<T> createParser(final DynamicOps<T> ops) {
      T trueValue = (T)ops.createBoolean(true);
      T falseValue = (T)ops.createBoolean(false);
      T emptyMapValue = (T)ops.emptyMap();
      T emptyList = (T)ops.emptyList();
      Dictionary<StringReader> rules = new Dictionary<>();
      Atom<SnbtGrammar.Sign> sign = Atom.of("sign");
      rules.put(
         sign,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('+'), Term.marker(sign, SnbtGrammar.Sign.PLUS)),
            Term.sequence(StringReaderTerms.character('-'), Term.marker(sign, SnbtGrammar.Sign.MINUS))
         ),
         scope -> scope.getOrThrow(sign)
      );
      Atom<SnbtGrammar.IntegerSuffix> integerSuffix = Atom.of("integer_suffix");
      rules.put(
         integerSuffix,
         Term.alternative(
            Term.sequence(
               StringReaderTerms.characters('u', 'U'),
               Term.alternative(
                  Term.sequence(
                     StringReaderTerms.characters('b', 'B'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.BYTE))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('s', 'S'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.SHORT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('i', 'I'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.INT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('l', 'L'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.UNSIGNED, SnbtGrammar.TypeSuffix.LONG))
                  )
               )
            ),
            Term.sequence(
               StringReaderTerms.characters('s', 'S'),
               Term.alternative(
                  Term.sequence(
                     StringReaderTerms.characters('b', 'B'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.BYTE))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('s', 'S'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.SHORT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('i', 'I'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.INT))
                  ),
                  Term.sequence(
                     StringReaderTerms.characters('l', 'L'),
                     Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(SnbtGrammar.SignedPrefix.SIGNED, SnbtGrammar.TypeSuffix.LONG))
                  )
               )
            ),
            Term.sequence(StringReaderTerms.characters('b', 'B'), Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.BYTE))),
            Term.sequence(StringReaderTerms.characters('s', 'S'), Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.SHORT))),
            Term.sequence(StringReaderTerms.characters('i', 'I'), Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.INT))),
            Term.sequence(StringReaderTerms.characters('l', 'L'), Term.marker(integerSuffix, new SnbtGrammar.IntegerSuffix(null, SnbtGrammar.TypeSuffix.LONG)))
         ),
         scope -> scope.getOrThrow(integerSuffix)
      );
      Atom<String> binaryNumeral = Atom.of("binary_numeral");
      rules.put(binaryNumeral, BINARY_NUMERAL);
      Atom<String> decimalNumeral = Atom.of("decimal_numeral");
      rules.put(decimalNumeral, DECIMAL_NUMERAL);
      Atom<String> hexNumeral = Atom.of("hex_numeral");
      rules.put(hexNumeral, HEX_NUMERAL);
      Atom<SnbtGrammar.IntegerLiteral> integerLiteral = Atom.of("integer_literal");
      NamedRule<StringReader, SnbtGrammar.IntegerLiteral> integerLiteralRule = rules.put(
         integerLiteral,
         Term.sequence(
            Term.optional(rules.named(sign)),
            Term.alternative(
               Term.sequence(
                  StringReaderTerms.character('0'),
                  Term.cut(),
                  Term.alternative(
                     Term.sequence(StringReaderTerms.characters('x', 'X'), Term.cut(), rules.named(hexNumeral)),
                     Term.sequence(StringReaderTerms.characters('b', 'B'), rules.named(binaryNumeral)),
                     Term.sequence(rules.named(decimalNumeral), Term.cut(), Term.fail(ERROR_LEADING_ZERO_NOT_ALLOWED)),
                     Term.marker(decimalNumeral, "0")
                  )
               ),
               rules.named(decimalNumeral)
            ),
            Term.optional(rules.named(integerSuffix))
         ),
         scope -> {
            SnbtGrammar.IntegerSuffix suffix = scope.getOrDefault(integerSuffix, SnbtGrammar.IntegerSuffix.EMPTY);
            SnbtGrammar.Sign signValue = scope.getOrDefault(sign, SnbtGrammar.Sign.PLUS);
            String decimalContents = scope.get(decimalNumeral);
            if (decimalContents != null) {
               return new SnbtGrammar.IntegerLiteral(signValue, SnbtGrammar.Base.DECIMAL, decimalContents, suffix);
            }

            String hexContents = scope.get(hexNumeral);
            if (hexContents != null) {
               return new SnbtGrammar.IntegerLiteral(signValue, SnbtGrammar.Base.HEX, hexContents, suffix);
            }

            String binaryContents = scope.getOrThrow(binaryNumeral);
            return new SnbtGrammar.IntegerLiteral(signValue, SnbtGrammar.Base.BINARY, binaryContents, suffix);
         }
      );
      Atom<SnbtGrammar.TypeSuffix> floatTypeSuffix = Atom.of("float_type_suffix");
      rules.put(
         floatTypeSuffix,
         Term.alternative(
            Term.sequence(StringReaderTerms.characters('f', 'F'), Term.marker(floatTypeSuffix, SnbtGrammar.TypeSuffix.FLOAT)),
            Term.sequence(StringReaderTerms.characters('d', 'D'), Term.marker(floatTypeSuffix, SnbtGrammar.TypeSuffix.DOUBLE))
         ),
         scope -> scope.getOrThrow(floatTypeSuffix)
      );
      Atom<SnbtGrammar.Signed<String>> floatExponentPart = Atom.of("float_exponent_part");
      rules.put(
         floatExponentPart,
         Term.sequence(StringReaderTerms.characters('e', 'E'), Term.optional(rules.named(sign)), rules.named(decimalNumeral)),
         scope -> new SnbtGrammar.Signed<>(scope.getOrDefault(sign, SnbtGrammar.Sign.PLUS), scope.getOrThrow(decimalNumeral))
      );
      Atom<String> floatWholePart = Atom.of("float_whole_part");
      Atom<String> floatFractionPart = Atom.of("float_fraction_part");
      Atom<T> floatLiteral = Atom.of("float_literal");
      rules.putComplex(
         floatLiteral,
         Term.sequence(
            Term.optional(rules.named(sign)),
            Term.alternative(
               Term.sequence(
                  rules.namedWithAlias(decimalNumeral, floatWholePart),
                  StringReaderTerms.character('.'),
                  Term.cut(),
                  Term.optional(rules.namedWithAlias(decimalNumeral, floatFractionPart)),
                  Term.optional(rules.named(floatExponentPart)),
                  Term.optional(rules.named(floatTypeSuffix))
               ),
               Term.sequence(
                  StringReaderTerms.character('.'),
                  Term.cut(),
                  rules.namedWithAlias(decimalNumeral, floatFractionPart),
                  Term.optional(rules.named(floatExponentPart)),
                  Term.optional(rules.named(floatTypeSuffix))
               ),
               Term.sequence(
                  rules.namedWithAlias(decimalNumeral, floatWholePart), rules.named(floatExponentPart), Term.cut(), Term.optional(rules.named(floatTypeSuffix))
               ),
               Term.sequence(rules.namedWithAlias(decimalNumeral, floatWholePart), Term.optional(rules.named(floatExponentPart)), rules.named(floatTypeSuffix))
            )
         ),
         state -> {
            Scope scope = state.scope();
            SnbtGrammar.Sign wholeSign = scope.getOrDefault(sign, SnbtGrammar.Sign.PLUS);
            String whole = scope.get(floatWholePart);
            String fraction = scope.get(floatFractionPart);
            SnbtGrammar.Signed<String> exponent = scope.get(floatExponentPart);
            SnbtGrammar.TypeSuffix typeSuffix = scope.get(floatTypeSuffix);
            return createFloat(ops, wholeSign, whole, fraction, exponent, typeSuffix, state);
         }
      );
      Atom<String> stringHex2 = Atom.of("string_hex_2");
      rules.put(stringHex2, new SnbtGrammar.SimpleHexLiteralParseRule(2));
      Atom<String> stringHex4 = Atom.of("string_hex_4");
      rules.put(stringHex4, new SnbtGrammar.SimpleHexLiteralParseRule(4));
      Atom<String> stringHex8 = Atom.of("string_hex_8");
      rules.put(stringHex8, new SnbtGrammar.SimpleHexLiteralParseRule(8));
      Atom<String> stringUnicodeName = Atom.of("string_unicode_name");
      rules.put(stringUnicodeName, new GreedyPatternParseRule(UNICODE_NAME, ERROR_INVALID_CHARACTER_NAME));
      Atom<String> stringEscapeSequence = Atom.of("string_escape_sequence");
      rules.putComplex(
         stringEscapeSequence,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('b'), Term.marker(stringEscapeSequence, "\b")),
            Term.sequence(StringReaderTerms.character('s'), Term.marker(stringEscapeSequence, " ")),
            Term.sequence(StringReaderTerms.character('t'), Term.marker(stringEscapeSequence, "\t")),
            Term.sequence(StringReaderTerms.character('n'), Term.marker(stringEscapeSequence, "\n")),
            Term.sequence(StringReaderTerms.character('f'), Term.marker(stringEscapeSequence, "\f")),
            Term.sequence(StringReaderTerms.character('r'), Term.marker(stringEscapeSequence, "\r")),
            Term.sequence(StringReaderTerms.character('\\'), Term.marker(stringEscapeSequence, "\\")),
            Term.sequence(StringReaderTerms.character('\''), Term.marker(stringEscapeSequence, "'")),
            Term.sequence(StringReaderTerms.character('"'), Term.marker(stringEscapeSequence, "\"")),
            Term.sequence(StringReaderTerms.character('x'), rules.named(stringHex2)),
            Term.sequence(StringReaderTerms.character('u'), rules.named(stringHex4)),
            Term.sequence(StringReaderTerms.character('U'), rules.named(stringHex8)),
            Term.sequence(StringReaderTerms.character('N'), StringReaderTerms.character('{'), rules.named(stringUnicodeName), StringReaderTerms.character('}'))
         ),
         state -> {
            Scope scope = state.scope();
            String plainEscape = scope.getAny(stringEscapeSequence);
            if (plainEscape != null) {
               return plainEscape;
            }

            String hexEscape = scope.getAny(stringHex2, stringHex4, stringHex8);
            if (hexEscape != null) {
               int codePoint = HexFormat.fromHexDigits(hexEscape);
               if (!Character.isValidCodePoint(codePoint)) {
                  state.errorCollector().store(state.mark(), DelayedException.create(ERROR_INVALID_CODEPOINT, String.format(Locale.ROOT, "U+%08X", codePoint)));
                  return null;
               } else {
                  return Character.toString(codePoint);
               }
            } else {
               String character = scope.getOrThrow(stringUnicodeName);

               int codePoint;
               try {
                  codePoint = Character.codePointOf(character);
               } catch (IllegalArgumentException e) {
                  state.errorCollector().store(state.mark(), ERROR_INVALID_CHARACTER_NAME);
                  return null;
               }

               return Character.toString(codePoint);
            }
         }
      );
      Atom<String> stringPlainContents = Atom.of("string_plain_contents");
      rules.put(stringPlainContents, PLAIN_STRING_CHUNK);
      Atom<List<String>> stringChunks = Atom.of("string_chunks");
      Atom<String> stringContents = Atom.of("string_contents");
      Atom<String> singleQuotedStringChunk = Atom.of("single_quoted_string_chunk");
      NamedRule<StringReader, String> singleQuotedStringChunkRule = rules.put(
         singleQuotedStringChunk,
         Term.alternative(
            rules.namedWithAlias(stringPlainContents, stringContents),
            Term.sequence(StringReaderTerms.character('\\'), rules.namedWithAlias(stringEscapeSequence, stringContents)),
            Term.sequence(StringReaderTerms.character('"'), Term.marker(stringContents, "\""))
         ),
         scope -> scope.getOrThrow(stringContents)
      );
      Atom<String> singleQuotedStringContents = Atom.of("single_quoted_string_contents");
      rules.put(singleQuotedStringContents, Term.repeated(singleQuotedStringChunkRule, stringChunks), scope -> joinList(scope.getOrThrow(stringChunks)));
      Atom<String> doubleQuotedStringChunk = Atom.of("double_quoted_string_chunk");
      NamedRule<StringReader, String> doubleQuotedStringChunkRule = rules.put(
         doubleQuotedStringChunk,
         Term.alternative(
            rules.namedWithAlias(stringPlainContents, stringContents),
            Term.sequence(StringReaderTerms.character('\\'), rules.namedWithAlias(stringEscapeSequence, stringContents)),
            Term.sequence(StringReaderTerms.character('\''), Term.marker(stringContents, "'"))
         ),
         scope -> scope.getOrThrow(stringContents)
      );
      Atom<String> doubleQuotedStringContents = Atom.of("double_quoted_string_contents");
      rules.put(doubleQuotedStringContents, Term.repeated(doubleQuotedStringChunkRule, stringChunks), scope -> joinList(scope.getOrThrow(stringChunks)));
      Atom<String> quotedStringLiteral = Atom.of("quoted_string_literal");
      rules.put(
         quotedStringLiteral,
         Term.alternative(
            Term.sequence(
               StringReaderTerms.character('"'),
               Term.cut(),
               Term.optional(rules.namedWithAlias(doubleQuotedStringContents, stringContents)),
               StringReaderTerms.character('"')
            ),
            Term.sequence(
               StringReaderTerms.character('\''),
               Term.optional(rules.namedWithAlias(singleQuotedStringContents, stringContents)),
               StringReaderTerms.character('\'')
            )
         ),
         scope -> scope.getOrThrow(stringContents)
      );
      Atom<String> unquotedString = Atom.of("unquoted_string");
      rules.put(unquotedString, new UnquotedStringParseRule(1, ERROR_EXPECTED_UNQUOTED_STRING));
      Atom<T> literal = Atom.of("literal");
      Atom<List<T>> argumentList = Atom.of("arguments");
      rules.put(
         argumentList,
         Term.repeatedWithTrailingSeparator(rules.forward(literal), argumentList, StringReaderTerms.character(',')),
         scope -> scope.getOrThrow(argumentList)
      );
      Atom<T> unquotedStringOrBuiltIn = Atom.of("unquoted_string_or_builtin");
      rules.putComplex(
         unquotedStringOrBuiltIn,
         Term.sequence(
            rules.named(unquotedString),
            Term.optional(Term.sequence(StringReaderTerms.character('('), rules.named(argumentList), StringReaderTerms.character(')')))
         ),
         state -> {
            Scope scope = state.scope();
            String contents = scope.getOrThrow(unquotedString);
            if (!contents.isEmpty() && isAllowedToStartUnquotedString(contents.charAt(0))) {
               List<T> arguments = scope.get(argumentList);
               if (arguments != null) {
                  SnbtOperations.BuiltinKey key = new SnbtOperations.BuiltinKey(contents, arguments.size());
                  SnbtOperations.BuiltinOperation operation = SnbtOperations.BUILTIN_OPERATIONS.get(key);
                  if (operation != null) {
                     return operation.run(ops, arguments, state);
                  }

                  state.errorCollector().store(state.mark(), DelayedException.create(ERROR_NO_SUCH_OPERATION, key.toString()));
                  return null;
               } else if (contents.equalsIgnoreCase("true")) {
                  return trueValue;
               } else {
                  return (T)(contents.equalsIgnoreCase("false") ? falseValue : ops.createString(contents));
               }
            } else {
               state.errorCollector().store(state.mark(), SnbtOperations.BUILTIN_IDS, ERROR_INVALID_UNQUOTED_START);
               return null;
            }
         }
      );
      Atom<String> mapKey = Atom.of("map_key");
      rules.put(
         mapKey,
         Term.alternative(rules.named(quotedStringLiteral), rules.named(unquotedString)),
         scope -> scope.getAnyOrThrow(quotedStringLiteral, unquotedString)
      );
      Atom<Entry<String, T>> mapEntry = Atom.of("map_entry");
      NamedRule<StringReader, Entry<String, T>> mapEntryRule = rules.putComplex(
         mapEntry, Term.sequence(rules.named(mapKey), StringReaderTerms.character(':'), rules.named(literal)), state -> {
            Scope scope = state.scope();
            String key = scope.getOrThrow(mapKey);
            if (key.isEmpty()) {
               state.errorCollector().store(state.mark(), ERROR_EMPTY_KEY);
               return null;
            } else {
               T value = scope.getOrThrow(literal);
               return Map.entry(key, value);
            }
         }
      );
      Atom<List<Entry<String, T>>> mapEntries = Atom.of("map_entries");
      rules.put(
         mapEntries, Term.repeatedWithTrailingSeparator(mapEntryRule, mapEntries, StringReaderTerms.character(',')), scope -> scope.getOrThrow(mapEntries)
      );
      Atom<T> mapLiteral = Atom.of("map_literal");
      rules.put(mapLiteral, Term.sequence(StringReaderTerms.character('{'), rules.named(mapEntries), StringReaderTerms.character('}')), scope -> {
         List<Entry<String, T>> entries = scope.getOrThrow(mapEntries);
         if (entries.isEmpty()) {
            return emptyMapValue;
         }

         Builder<T, T> builder = ImmutableMap.builderWithExpectedSize(entries.size());

         for (Entry<String, T> e : entries) {
            builder.put(ops.createString(e.getKey()), e.getValue());
         }

         return (T)ops.createMap(builder.buildKeepingLast());
      });
      Atom<List<T>> listEntries = Atom.of("list_entries");
      rules.put(
         listEntries,
         Term.repeatedWithTrailingSeparator(rules.forward(literal), listEntries, StringReaderTerms.character(',')),
         scope -> scope.getOrThrow(listEntries)
      );
      Atom<SnbtGrammar.ArrayPrefix> arrayPrefix = Atom.of("array_prefix");
      rules.put(
         arrayPrefix,
         Term.alternative(
            Term.sequence(StringReaderTerms.character('B'), Term.marker(arrayPrefix, SnbtGrammar.ArrayPrefix.BYTE)),
            Term.sequence(StringReaderTerms.character('L'), Term.marker(arrayPrefix, SnbtGrammar.ArrayPrefix.LONG)),
            Term.sequence(StringReaderTerms.character('I'), Term.marker(arrayPrefix, SnbtGrammar.ArrayPrefix.INT))
         ),
         scope -> scope.getOrThrow(arrayPrefix)
      );
      Atom<List<SnbtGrammar.IntegerLiteral>> intArrayEntries = Atom.of("int_array_entries");
      rules.put(
         intArrayEntries,
         Term.repeatedWithTrailingSeparator(integerLiteralRule, intArrayEntries, StringReaderTerms.character(',')),
         scope -> scope.getOrThrow(intArrayEntries)
      );
      Atom<T> listLiteral = Atom.of("list_literal");
      rules.putComplex(
         listLiteral,
         Term.sequence(
            StringReaderTerms.character('['),
            Term.alternative(Term.sequence(rules.named(arrayPrefix), StringReaderTerms.character(';'), rules.named(intArrayEntries)), rules.named(listEntries)),
            StringReaderTerms.character(']')
         ),
         state -> {
            Scope scope = state.scope();
            SnbtGrammar.ArrayPrefix arrayType = scope.get(arrayPrefix);
            if (arrayType != null) {
               List<SnbtGrammar.IntegerLiteral> entries = scope.getOrThrow(intArrayEntries);
               return entries.isEmpty() ? arrayType.create(ops) : arrayType.create(ops, entries, state);
            } else {
               List<T> entries = scope.getOrThrow(listEntries);
               return (T)(entries.isEmpty() ? emptyList : ops.createList(entries.stream()));
            }
         }
      );
      NamedRule<StringReader, T> literalRule = rules.putComplex(
         literal,
         Term.alternative(
            Term.sequence(Term.positiveLookahead(NUMBER_LOOKEAHEAD), Term.alternative(rules.namedWithAlias(floatLiteral, literal), rules.named(integerLiteral))),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.characters('"', '\'')), Term.cut(), rules.named(quotedStringLiteral)),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.character('{')), Term.cut(), rules.namedWithAlias(mapLiteral, literal)),
            Term.sequence(Term.positiveLookahead(StringReaderTerms.character('[')), Term.cut(), rules.namedWithAlias(listLiteral, literal)),
            rules.namedWithAlias(unquotedStringOrBuiltIn, literal)
         ),
         state -> {
            Scope scope = state.scope();
            String quotedString = scope.get(quotedStringLiteral);
            if (quotedString != null) {
               return (T)ops.createString(quotedString);
            }

            SnbtGrammar.IntegerLiteral integer = scope.get(integerLiteral);
            return integer != null ? integer.create(ops, state) : scope.getOrThrow(literal);
         }
      );
      return new Grammar<>(rules, literalRule);
   }

   private enum ArrayPrefix {
      BYTE(SnbtGrammar.TypeSuffix.BYTE) {
         private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.wrap(new byte[0]);

         @Override
         public <T> T create(final DynamicOps<T> ops) {
            return (T)ops.createByteList(EMPTY_BUFFER);
         }

         @Override
         public <T> @Nullable T create(final DynamicOps<T> ops, final List<SnbtGrammar.IntegerLiteral> entries, final ParseState<?> state) {
            ByteList result = new ByteArrayList();

            for (SnbtGrammar.IntegerLiteral entry : entries) {
               Number parsedNumber = this.buildNumber(entry, state);
               if (parsedNumber == null) {
                  return null;
               }

               result.add(parsedNumber.byteValue());
            }

            return (T)ops.createByteList(ByteBuffer.wrap(result.toByteArray()));
         }
      },
      INT(SnbtGrammar.TypeSuffix.INT, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT) {
         @Override
         public <T> T create(final DynamicOps<T> ops) {
            return (T)ops.createIntList(IntStream.empty());
         }

         @Override
         public <T> @Nullable T create(final DynamicOps<T> ops, final List<SnbtGrammar.IntegerLiteral> entries, final ParseState<?> state) {
            java.util.stream.IntStream.Builder result = IntStream.builder();

            for (SnbtGrammar.IntegerLiteral entry : entries) {
               Number parsedNumber = this.buildNumber(entry, state);
               if (parsedNumber == null) {
                  return null;
               }

               result.add(parsedNumber.intValue());
            }

            return (T)ops.createIntList(result.build());
         }
      },
      LONG(SnbtGrammar.TypeSuffix.LONG, SnbtGrammar.TypeSuffix.BYTE, SnbtGrammar.TypeSuffix.SHORT, SnbtGrammar.TypeSuffix.INT) {
         @Override
         public <T> T create(final DynamicOps<T> ops) {
            return (T)ops.createLongList(LongStream.empty());
         }

         @Override
         public <T> @Nullable T create(final DynamicOps<T> ops, final List<SnbtGrammar.IntegerLiteral> entries, final ParseState<?> state) {
            java.util.stream.LongStream.Builder result = LongStream.builder();

            for (SnbtGrammar.IntegerLiteral entry : entries) {
               Number parsedNumber = this.buildNumber(entry, state);
               if (parsedNumber == null) {
                  return null;
               }

               result.add(parsedNumber.longValue());
            }

            return (T)ops.createLongList(result.build());
         }
      };

      private final SnbtGrammar.TypeSuffix defaultType;
      private final Set<SnbtGrammar.TypeSuffix> additionalTypes;

      ArrayPrefix(final SnbtGrammar.TypeSuffix defaultType, final SnbtGrammar.TypeSuffix... additionalTypes) {
         this.additionalTypes = Set.of(additionalTypes);
         this.defaultType = defaultType;
      }

      public boolean isAllowed(final SnbtGrammar.TypeSuffix type) {
         return type == this.defaultType || this.additionalTypes.contains(type);
      }

      public abstract <T> T create(DynamicOps<T> ops);

      public abstract <T> @Nullable T create(DynamicOps<T> ops, List<SnbtGrammar.IntegerLiteral> entries, ParseState<?> state);

      protected @Nullable Number buildNumber(final SnbtGrammar.IntegerLiteral entry, final ParseState<?> state) {
         SnbtGrammar.TypeSuffix actualType = this.computeType(entry.suffix);
         if (actualType == null) {
            state.errorCollector().store(state.mark(), SnbtGrammar.ERROR_INVALID_ARRAY_ELEMENT_TYPE);
            return null;
         } else {
            return entry.create(JavaOps.INSTANCE, actualType, state);
         }
      }

      private SnbtGrammar.@Nullable TypeSuffix computeType(final SnbtGrammar.IntegerSuffix value) {
         SnbtGrammar.TypeSuffix type = value.type();
         if (type == null) {
            return this.defaultType;
         } else {
            return !this.isAllowed(type) ? null : type;
         }
      }
   }

   private enum Base {
      BINARY,
      DECIMAL,
      HEX;
   }

   private record IntegerLiteral(SnbtGrammar.Sign sign, SnbtGrammar.Base base, String digits, SnbtGrammar.IntegerSuffix suffix) {
      private SnbtGrammar.SignedPrefix signedOrDefault() {
         if (this.suffix.signed != null) {
            return this.suffix.signed;
         }

         return switch (this.base) {
            case BINARY, HEX -> SnbtGrammar.SignedPrefix.UNSIGNED;
            case DECIMAL -> SnbtGrammar.SignedPrefix.SIGNED;
         };
      }

      private String cleanupDigits(final SnbtGrammar.Sign sign) {
         boolean needsUnderscoreRemoval = SnbtGrammar.needsUnderscoreRemoval(this.digits);
         if (sign != SnbtGrammar.Sign.MINUS && !needsUnderscoreRemoval) {
            return this.digits;
         }

         StringBuilder result = new StringBuilder();
         sign.append(result);
         SnbtGrammar.cleanAndAppend(result, this.digits, needsUnderscoreRemoval);
         return result.toString();
      }

      public <T> @Nullable T create(final DynamicOps<T> ops, final ParseState<?> state) {
         return this.create(ops, Objects.requireNonNullElse(this.suffix.type, SnbtGrammar.TypeSuffix.INT), state);
      }

      public <T> @Nullable T create(final DynamicOps<T> ops, final SnbtGrammar.TypeSuffix type, final ParseState<?> state) {
         boolean isSigned = this.signedOrDefault() == SnbtGrammar.SignedPrefix.SIGNED;
         if (!isSigned && this.sign == SnbtGrammar.Sign.MINUS) {
            state.errorCollector().store(state.mark(), SnbtGrammar.ERROR_EXPECTED_NON_NEGATIVE_NUMBER);
            return null;
         }

         String fixedDigits = this.cleanupDigits(this.sign);

         int radix = switch (this.base) {
            case BINARY -> 2;
            case DECIMAL -> 10;
            case HEX -> 16;
         };

         try {
            if (isSigned) {
               return (T)(switch (type) {
                  case BYTE -> ops.createByte(Byte.parseByte(fixedDigits, radix));
                  case SHORT -> ops.createShort(Short.parseShort(fixedDigits, radix));
                  case INT -> ops.createInt(Integer.parseInt(fixedDigits, radix));
                  case LONG -> ops.createLong(Long.parseLong(fixedDigits, radix));
                  default -> {
                     state.errorCollector().store(state.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                     yield null;
                  }
               });
            } else {
               return (T)(switch (type) {
                  case BYTE -> ops.createByte(UnsignedBytes.parseUnsignedByte(fixedDigits, radix));
                  case SHORT -> ops.createShort(SnbtGrammar.parseUnsignedShort(fixedDigits, radix));
                  case INT -> ops.createInt(Integer.parseUnsignedInt(fixedDigits, radix));
                  case LONG -> ops.createLong(Long.parseUnsignedLong(fixedDigits, radix));
                  default -> {
                     state.errorCollector().store(state.mark(), SnbtGrammar.ERROR_EXPECTED_INTEGER_TYPE);
                     yield null;
                  }
               });
            }
         } catch (NumberFormatException e) {
            state.errorCollector().store(state.mark(), SnbtGrammar.createNumberParseError(e));
            return null;
         }
      }
   }

   private record IntegerSuffix(SnbtGrammar.@Nullable SignedPrefix signed, SnbtGrammar.@Nullable TypeSuffix type) {
      public static final SnbtGrammar.IntegerSuffix EMPTY = new SnbtGrammar.IntegerSuffix(null, null);
   }

   private enum Sign {
      PLUS,
      MINUS;

      public void append(final StringBuilder output) {
         if (this == MINUS) {
            output.append("-");
         }
      }
   }

   private record Signed<T>(SnbtGrammar.Sign sign, T value) {
   }

   private enum SignedPrefix {
      SIGNED,
      UNSIGNED;
   }

   private static class SimpleHexLiteralParseRule extends GreedyPredicateParseRule {
      public SimpleHexLiteralParseRule(final int size) {
         super(size, size, DelayedException.create(SnbtGrammar.ERROR_EXPECTED_HEX_ESCAPE, String.valueOf(size)));
      }

      @Override
      protected boolean isAccepted(final char c) {
         return switch (c) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f' -> true;
            default -> false;
         };
      }
   }

   private enum TypeSuffix {
      FLOAT,
      DOUBLE,
      BYTE,
      SHORT,
      INT,
      LONG;
   }
}
