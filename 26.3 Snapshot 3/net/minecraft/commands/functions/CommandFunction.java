package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface CommandFunction<T> {
   Identifier id();

   InstantiatedFunction<T> instantiate(@Nullable CompoundTag arguments, CommandDispatcher<T> dispatcher) throws FunctionInstantiationException;

   private static boolean shouldConcatenateNextLine(final CharSequence line) {
      int length = line.length();
      return length > 0 && line.charAt(length - 1) == '\\';
   }

   static <T extends ExecutionCommandSource<T>> CommandFunction<T> fromLines(
      final Identifier id, final CommandDispatcher<T> dispatcher, final T compilationContext, final List<String> lines
   ) {
      FunctionBuilder<T> functionBuilder = new FunctionBuilder<>();

      for (int i = 0; i < lines.size(); i++) {
         int lineNumber = i + 1;
         String inputLine = lines.get(i).trim();
         String line;
         if (shouldConcatenateNextLine(inputLine)) {
            StringBuilder builder = new StringBuilder(inputLine);

            do {
               if (++i == lines.size()) {
                  throw new IllegalArgumentException("Line continuation at end of file");
               }

               builder.deleteCharAt(builder.length() - 1);
               String innerLine = lines.get(i).trim();
               builder.append(innerLine);
               checkCommandLineLength(builder);
            } while (shouldConcatenateNextLine(builder));

            line = builder.toString();
         } else {
            line = inputLine;
         }

         checkCommandLineLength(line);
         StringReader input = new StringReader(line);
         if (input.canRead() && input.peek() != '#') {
            if (input.peek() == '/') {
               input.skip();
               if (input.peek() == '/') {
                  throw new IllegalArgumentException(
                     "Unknown or invalid command '" + line + "' on line " + lineNumber + " (if you intended to make a comment, use '#' not '//')"
                  );
               }

               String name = input.readUnquotedString();
               throw new IllegalArgumentException(
                  "Unknown or invalid command '" + line + "' on line " + lineNumber + " (did you mean '" + name + "'? Do not use a preceding forwards slash.)"
               );
            }

            if (input.peek() == '$') {
               functionBuilder.addMacro(line.substring(1), lineNumber, compilationContext);
            } else {
               try {
                  functionBuilder.addCommand(parseCommand(dispatcher, compilationContext, input));
               } catch (CommandSyntaxException e) {
                  throw new IllegalArgumentException("Whilst parsing command on line " + lineNumber + ": " + e.getMessage());
               }
            }
         }
      }

      return functionBuilder.build(id);
   }

   static void checkCommandLineLength(final CharSequence line) {
      if (line.length() > 2000000) {
         CharSequence truncated = line.subSequence(0, Math.min(512, 2000000));
         throw new IllegalStateException("Command too long: " + line.length() + " characters, contents: " + truncated + "...");
      }
   }

   static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> parseCommand(
      final CommandDispatcher<T> dispatcher, final T compilationContext, final StringReader input
   ) throws CommandSyntaxException {
      ParseResults<T> parse = dispatcher.parse(input, compilationContext);
      Commands.validateParseResults(parse);
      Optional<ContextChain<T>> commandChain = ContextChain.tryFlatten(parse.getContext().build(input.getString()));
      if (commandChain.isEmpty()) {
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parse.getReader());
      } else {
         return new BuildContexts.Unbound<>(input.getString(), commandChain.get());
      }
   }
}
