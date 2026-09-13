package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class MacroFunction<T extends ExecutionCommandSource<T>> implements CommandFunction<T> {
   private static final DecimalFormat DECIMAL_FORMAT = Util.make(
      new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ROOT)), format -> format.setMaximumFractionDigits(15)
   );
   private static final int MAX_CACHE_ENTRIES = 8;
   private final List<String> parameters;
   private final Object2ObjectLinkedOpenHashMap<List<String>, InstantiatedFunction<T>> cache = new Object2ObjectLinkedOpenHashMap(8, 0.25F);
   private final Identifier id;
   private final List<MacroFunction.Entry<T>> entries;

   public MacroFunction(final Identifier id, final List<MacroFunction.Entry<T>> entries, final List<String> parameters) {
      this.id = id;
      this.entries = entries;
      this.parameters = parameters;
   }

   @Override
   public Identifier id() {
      return this.id;
   }

   @Override
   public InstantiatedFunction<T> instantiate(final @Nullable CompoundTag arguments, final CommandDispatcher<T> dispatcher) throws FunctionInstantiationException {
      if (arguments == null) {
         throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", Component.translationArg(this.id())));
      }

      List<String> parameterValues = new ArrayList<>(this.parameters.size());

      for (String argument : this.parameters) {
         Tag argumentValue = arguments.get(argument);
         if (argumentValue == null) {
            throw new FunctionInstantiationException(
               Component.translatable("commands.function.error.missing_argument", Component.translationArg(this.id()), argument)
            );
         }

         parameterValues.add(stringify(argumentValue));
      }

      InstantiatedFunction<T> cachedFunction = (InstantiatedFunction<T>)this.cache.getAndMoveToLast(parameterValues);
      if (cachedFunction != null) {
         return cachedFunction;
      }

      if (this.cache.size() >= 8) {
         this.cache.removeFirst();
      }

      InstantiatedFunction<T> function = this.substituteAndParse(this.parameters, parameterValues, dispatcher);
      this.cache.put(parameterValues, function);
      return function;
   }

   private static String stringify(final Tag tag) {
      return switch (tag) {
         case FloatTag(float value) -> DECIMAL_FORMAT.format(value);
         case DoubleTag(double value) -> DECIMAL_FORMAT.format(value);
         case ByteTag(byte value) -> String.valueOf(value);
         case ShortTag(short value) -> String.valueOf(value);
         case LongTag(long value) -> String.valueOf(value);
         case StringTag(String value) -> value;
         default -> tag.toString();
      };
   }

   private static void lookupValues(final List<String> values, final IntList indicesToSelect, final List<String> selectedValuesOutput) {
      selectedValuesOutput.clear();
      indicesToSelect.forEach(index -> selectedValuesOutput.add(values.get(index)));
   }

   private InstantiatedFunction<T> substituteAndParse(final List<String> keys, final List<String> values, final CommandDispatcher<T> dispatcher) throws FunctionInstantiationException {
      List<UnboundEntryAction<T>> newEntries = new ArrayList<>(this.entries.size());
      List<String> entryArguments = new ArrayList<>(values.size());

      for (MacroFunction.Entry<T> entry : this.entries) {
         lookupValues(values, entry.parameters(), entryArguments);
         newEntries.add(entry.instantiate(entryArguments, dispatcher, this.id));
      }

      return new PlainTextFunction<>(this.id().withPath(id -> id + "/" + keys.hashCode()), newEntries);
   }

   interface Entry<T> {
      IntList parameters();

      UnboundEntryAction<T> instantiate(List<String> substitutions, CommandDispatcher<T> dispatcher, Identifier funtionId) throws FunctionInstantiationException;
   }

   static class MacroEntry<T extends ExecutionCommandSource<T>> implements MacroFunction.Entry<T> {
      private final StringTemplate template;
      private final IntList parameters;
      private final T compilationContext;

      public MacroEntry(final StringTemplate template, final IntList parameters, final T compilationContext) {
         this.template = template;
         this.parameters = parameters;
         this.compilationContext = compilationContext;
      }

      @Override
      public IntList parameters() {
         return this.parameters;
      }

      @Override
      public UnboundEntryAction<T> instantiate(final List<String> substitutions, final CommandDispatcher<T> dispatcher, final Identifier functionId) throws FunctionInstantiationException {
         String command = this.template.substitute(substitutions);

         try {
            return CommandFunction.parseCommand(dispatcher, this.compilationContext, new StringReader(command));
         } catch (CommandSyntaxException e) {
            throw new FunctionInstantiationException(
               Component.translatable("commands.function.error.parse", Component.translationArg(functionId), command, e.getMessage())
            );
         }
      }
   }

   static class PlainTextEntry<T> implements MacroFunction.Entry<T> {
      private final UnboundEntryAction<T> compiledAction;

      public PlainTextEntry(final UnboundEntryAction<T> compiledAction) {
         this.compiledAction = compiledAction;
      }

      @Override
      public IntList parameters() {
         return IntLists.emptyList();
      }

      @Override
      public UnboundEntryAction<T> instantiate(final List<String> substitutions, final CommandDispatcher<T> dispatcher, final Identifier functionId) {
         return this.compiledAction;
      }
   }
}
