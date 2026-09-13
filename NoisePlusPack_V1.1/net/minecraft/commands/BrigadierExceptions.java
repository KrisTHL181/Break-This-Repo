package net.minecraft.commands;

import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class BrigadierExceptions implements BuiltInExceptionProvider {
   private static final Dynamic2CommandExceptionType DOUBLE_TOO_SMALL = new Dynamic2CommandExceptionType(
      (p_308315_, p_308316_) -> Component.translatableEscape("argument.double.low", p_308316_, p_308315_)
   );
   private static final Dynamic2CommandExceptionType DOUBLE_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_308320_, p_308321_) -> Component.translatableEscape("argument.double.big", p_308321_, p_308320_)
   );
   private static final Dynamic2CommandExceptionType FLOAT_TOO_SMALL = new Dynamic2CommandExceptionType(
      (p_308335_, p_308336_) -> Component.translatableEscape("argument.float.low", p_308336_, p_308335_)
   );
   private static final Dynamic2CommandExceptionType FLOAT_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_308318_, p_308319_) -> Component.translatableEscape("argument.float.big", p_308319_, p_308318_)
   );
   private static final Dynamic2CommandExceptionType INTEGER_TOO_SMALL = new Dynamic2CommandExceptionType(
      (p_308323_, p_308324_) -> Component.translatableEscape("argument.integer.low", p_308324_, p_308323_)
   );
   private static final Dynamic2CommandExceptionType INTEGER_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_308328_, p_308329_) -> Component.translatableEscape("argument.integer.big", p_308329_, p_308328_)
   );
   private static final Dynamic2CommandExceptionType LONG_TOO_SMALL = new Dynamic2CommandExceptionType(
      (p_308325_, p_308326_) -> Component.translatableEscape("argument.long.low", p_308326_, p_308325_)
   );
   private static final Dynamic2CommandExceptionType LONG_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_308337_, p_308338_) -> Component.translatableEscape("argument.long.big", p_308338_, p_308337_)
   );
   private static final DynamicCommandExceptionType LITERAL_INCORRECT = new DynamicCommandExceptionType(
      p_308332_ -> Component.translatableEscape("argument.literal.incorrect", p_308332_)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_START_OF_QUOTE = new SimpleCommandExceptionType(
      Component.translatable("parsing.quote.expected.start")
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_END_OF_QUOTE = new SimpleCommandExceptionType(
      Component.translatable("parsing.quote.expected.end")
   );
   private static final DynamicCommandExceptionType READER_INVALID_ESCAPE = new DynamicCommandExceptionType(
      p_308322_ -> Component.translatableEscape("parsing.quote.escape", p_308322_)
   );
   private static final DynamicCommandExceptionType READER_INVALID_BOOL = new DynamicCommandExceptionType(
      p_308330_ -> Component.translatableEscape("parsing.bool.invalid", p_308330_)
   );
   private static final DynamicCommandExceptionType READER_INVALID_INT = new DynamicCommandExceptionType(
      p_308327_ -> Component.translatableEscape("parsing.int.invalid", p_308327_)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("parsing.int.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_LONG = new DynamicCommandExceptionType(
      p_308334_ -> Component.translatableEscape("parsing.long.invalid", p_308334_)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_LONG = new SimpleCommandExceptionType(Component.translatable("parsing.long.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_DOUBLE = new DynamicCommandExceptionType(
      p_308331_ -> Component.translatableEscape("parsing.double.invalid", p_308331_)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("parsing.double.expected"));
   private static final DynamicCommandExceptionType READER_INVALID_FLOAT = new DynamicCommandExceptionType(
      p_308339_ -> Component.translatableEscape("parsing.float.invalid", p_308339_)
   );
   private static final SimpleCommandExceptionType READER_EXPECTED_FLOAT = new SimpleCommandExceptionType(Component.translatable("parsing.float.expected"));
   private static final SimpleCommandExceptionType READER_EXPECTED_BOOL = new SimpleCommandExceptionType(Component.translatable("parsing.bool.expected"));
   private static final DynamicCommandExceptionType READER_EXPECTED_SYMBOL = new DynamicCommandExceptionType(
      p_308333_ -> Component.translatableEscape("parsing.expected", p_308333_)
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
      p_308317_ -> Component.translatableEscape("command.exception", p_308317_)
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
