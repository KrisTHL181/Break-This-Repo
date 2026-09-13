package net.minecraft.commands;

import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class BrigadierExceptions implements BuiltInExceptionProvider {
   private static final Dynamic2CommandExceptionType DOUBLE_TOO_SMALL = new Dynamic2CommandExceptionType(
      (found, min) -> Component.translatableEscape("argument.double.low", min, found)
   );
   private static final Dynamic2CommandExceptionType DOUBLE_TOO_BIG = new Dynamic2CommandExceptionType(
      (found, max) -> Component.translatableEscape("argument.double.big", max, found)
   );
   private static final Dynamic2CommandExceptionType FLOAT_TOO_SMALL = new Dynamic2CommandExceptionType(
      (found, min) -> Component.translatableEscape("argument.float.low", min, found)
   );
   private static final Dynamic2CommandExceptionType FLOAT_TOO_BIG = new Dynamic2CommandExceptionType(
      (found, max) -> Component.translatableEscape("argument.float.big", max, found)
   );
   private static final Dynamic2CommandExceptionType INTEGER_TOO_SMALL = new Dynamic2CommandExceptionType(
      (found, min) -> Component.translatableEscape("argument.integer.low", min, found)
   );
   private static final Dynamic2CommandExceptionType INTEGER_TOO_BIG = new Dynamic2CommandExceptionType(
      (found, max) -> Component.translatableEscape("argument.integer.big", max, found)
   );
   private static final Dynamic2CommandExceptionType LONG_TOO_SMALL = new Dynamic2CommandExceptionType(
      (found, min) -> Component.translatableEscape("argument.long.low", min, found)
   );
   private static final Dynamic2CommandExceptionType LONG_TOO_BIG = new Dynamic2CommandExceptionType(
      (found, max) -> Component.translatableEscape("argument.long.big", max, found)
   );
   private static final DynamicCommandExceptionType LITERAL_INCORRECT = new DynamicCommandExceptionType(
      expected -> Component.translatableEscape("argument.literal.incorrect", expected)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_START_OF_QUOTE = new SimpleCommandExceptionType(
      Component.translatable("parsing.quote.expected.start")
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_END_OF_QUOTE = new SimpleCommandExceptionType(
      Component.translatable("parsing.quote.expected.end")
   );
   private static final DynamicCommandExceptionType READER_INVALID_ESCAPE = new DynamicCommandExceptionType(
      character -> Component.translatableEscape("parsing.quote.escape", character)
   );
   private static final DynamicCommandExceptionType READER_INVALID_BOOL = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("parsing.bool.invalid", value)
   );
   private static final DynamicCommandExceptionType READER_INVALID_INT = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("parsing.int.invalid", value)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("parsing.int.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_LONG = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("parsing.long.invalid", value)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_LONG = new SimpleCommandExceptionType(Component.translatable("parsing.long.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_DOUBLE = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("parsing.double.invalid", value)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("parsing.double.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_FLOAT = new DynamicCommandExceptionType(
      value -> Component.translatableEscape("parsing.float.invalid", value)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_FLOAT = new SimpleCommandExceptionType(Component.translatable("parsing.float.expected"));
   private static final SimpleCommandExceptionType READER_EXPECTED_BOOL = new SimpleCommandExceptionType(Component.translatable("parsing.bool.expected"));
   private static final DynamicCommandExceptionType READER_EXPECTED_SYMBOL = new DynamicCommandExceptionType(
      symbol -> Component.translatableEscape("parsing.expected", symbol)
   );
   private static final SimpleCommandExceptionType DISPATCHER_UNKNOWN_COMMAND = new SimpleCommandExceptionType(
      Component.translatable("command.unknown.command")
   );
   private static final SimpleCommandExceptionType DISPATCHER_UNKNOWN_ARGUMENT = new SimpleCommandExceptionType(
      Component.translatable("command.unknown.argument")
   );
   private static final SimpleCommandExceptionType DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR = new SimpleCommandExceptionType(
      Component.translatable("command.expected.separator")
   );
   private static final DynamicCommandExceptionType DISPATCHER_PARSE_EXCEPTION = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("command.exception", message)
   );

   public Dynamic2CommandExceptionType doubleTooLow() {
      return DOUBLE_TOO_SMALL;
   }

   public Dynamic2CommandExceptionType doubleTooHigh() {
      return DOUBLE_TOO_BIG;
   }

   public Dynamic2CommandExceptionType floatTooLow() {
      return FLOAT_TOO_SMALL;
   }

   public Dynamic2CommandExceptionType floatTooHigh() {
      return FLOAT_TOO_BIG;
   }

   public Dynamic2CommandExceptionType integerTooLow() {
      return INTEGER_TOO_SMALL;
   }

   public Dynamic2CommandExceptionType integerTooHigh() {
      return INTEGER_TOO_BIG;
   }

   public Dynamic2CommandExceptionType longTooLow() {
      return LONG_TOO_SMALL;
   }

   public Dynamic2CommandExceptionType longTooHigh() {
      return LONG_TOO_BIG;
   }

   public DynamicCommandExceptionType literalIncorrect() {
      return LITERAL_INCORRECT;
   }

   public SimpleCommandExceptionType readerExpectedStartOfQuote() {
      return READER_EXPECTED_START_OF_QUOTE;
   }

   public SimpleCommandExceptionType readerExpectedEndOfQuote() {
      return READER_EXPECTED_END_OF_QUOTE;
   }

   public DynamicCommandExceptionType readerInvalidEscape() {
      return READER_INVALID_ESCAPE;
   }

   public DynamicCommandExceptionType readerInvalidBool() {
      return READER_INVALID_BOOL;
   }

   public DynamicCommandExceptionType readerInvalidInt() {
      return READER_INVALID_INT;
   }

   public SimpleCommandExceptionType readerExpectedInt() {
      return READER_EXPECTED_INT;
   }

   public DynamicCommandExceptionType readerInvalidLong() {
      return READER_INVALID_LONG;
   }

   public SimpleCommandExceptionType readerExpectedLong() {
      return READER_EXPECTED_LONG;
   }

   public DynamicCommandExceptionType readerInvalidDouble() {
      return READER_INVALID_DOUBLE;
   }

   public SimpleCommandExceptionType readerExpectedDouble() {
      return READER_EXPECTED_DOUBLE;
   }

   public DynamicCommandExceptionType readerInvalidFloat() {
      return READER_INVALID_FLOAT;
   }

   public SimpleCommandExceptionType readerExpectedFloat() {
      return READER_EXPECTED_FLOAT;
   }

   public SimpleCommandExceptionType readerExpectedBool() {
      return READER_EXPECTED_BOOL;
   }

   public DynamicCommandExceptionType readerExpectedSymbol() {
      return READER_EXPECTED_SYMBOL;
   }

   public SimpleCommandExceptionType dispatcherUnknownCommand() {
      return DISPATCHER_UNKNOWN_COMMAND;
   }

   public SimpleCommandExceptionType dispatcherUnknownArgument() {
      return DISPATCHER_UNKNOWN_ARGUMENT;
   }

   public SimpleCommandExceptionType dispatcherExpectedArgumentSeparator() {
      return DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR;
   }

   public DynamicCommandExceptionType dispatcherParseException() {
      return DISPATCHER_PARSE_EXCEPTION;
   }
}
