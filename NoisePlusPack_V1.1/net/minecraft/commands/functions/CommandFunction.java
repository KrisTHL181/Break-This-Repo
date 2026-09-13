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

   InstantiatedFunction<T> instantiate(@Nullable CompoundTag var1, CommandDispatcher<T> var2) throws FunctionInstantiationException;

   private static boolean shouldConcatenateNextLine(CharSequence p_310145_) {
      int i = p_310145_.length();
      return i > 0 && p_310145_.charAt(i - 1) == '\\';
   }

   static <T extends ExecutionCommandSource<T>> CommandFunction<T> fromLines(
      Identifier p_457373_, CommandDispatcher<T> p_310963_, T p_312231_, List<String> p_310814_
   ) {
      FunctionBuilder<T> functionbuilder = new FunctionBuilder<>();

      for (int i = 0; i < p_310814_.size(); i++) {
         int j = i + 1;
         String s = p_310814_.get(i).trim();
         String s1;
         if (shouldConcatenateNextLine(s)) {
            StringBuilder stringbuilder = new StringBuilder(s);

            do {
               if (++i == p_310814_.size()) {
                  throw new IllegalArgumentException("Line continuation at end of file");
               }

               stringbuilder.deleteCharAt(stringbuilder.length() - 1);
               String s2 = p_310814_.get(i).trim();
               stringbuilder.append(s2);
               checkCommandLineLength(stringbuilder);
            } while (shouldConcatenateNextLine(stringbuilder));

            s1 = stringbuilder.toString();
         } else {
            s1 = s;
         }

         checkCommandLineLength(s1);
         StringReader stringreader = new StringReader(s1);
         if (stringreader.canRead() && stringreader.peek() != '#') {
            if (stringreader.peek() == '/') {
               stringreader.skip();
               if (stringreader.peek() == '/') {
                  throw new IllegalArgumentException(
                     "Unknown or invalid command '" + s1 + "' on line " + j + " (if you intended to make a comment, use '#' not '//')"
                  );
               }

               String s3 = stringreader.readUnquotedString();
               throw new IllegalArgumentException(
                  "Unknown or invalid command '" + s1 + "' on line " + j + " (did you mean '" + s3 + "'? Do not use a preceding forwards slash.)"
               );
            }

            if (stringreader.peek() == '$') {
               functionbuilder.addMacro(s1.substring(1), j, p_312231_);
            } else {
               try {
                  functionbuilder.addCommand(parseCommand(p_310963_, p_312231_, stringreader));
               } catch (CommandSyntaxException commandsyntaxexception) {
                  throw new IllegalArgumentException("Whilst parsing command on line " + j + ": " + commandsyntaxexception.getMessage());
               }
            }
         }
      }

      return functionbuilder.build(p_457373_);
   }

   static void checkCommandLineLength(CharSequence p_332928_) {
      if (p_332928_.length() > 2000000) {
         CharSequence charsequence = p_332928_.subSequence(0, Math.min(512, 2000000));
         throw new IllegalStateException("Command too long: " + p_332928_.length() + " characters, contents: " + charsequence + "...");
      }
   }

   static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> parseCommand(CommandDispatcher<T> p_310812_, T p_312436_, StringReader p_310713_) throws CommandSyntaxException {
      ParseResults<T> parseresults = p_310812_.parse(p_310713_, p_312436_);
      Commands.validateParseResults(parseresults);
      Optional<ContextChain<T>> optional = ContextChain.tryFlatten(parseresults.getContext().build(p_310713_.getString()));
      if (optional.isEmpty()) {
         throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(parseresults.getReader());
      } else {
         return new BuildContexts.Unbound<>(p_310713_.getString(), optional.get());
      }
   }
}
