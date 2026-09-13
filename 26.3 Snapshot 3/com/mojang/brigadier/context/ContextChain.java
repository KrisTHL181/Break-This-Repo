package com.mojang.brigadier.context;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ContextChain<S> {
   private final List<CommandContext<S>> modifiers;
   private final CommandContext<S> executable;
   private ContextChain<S> nextStageCache = null;

   public ContextChain(List<CommandContext<S>> modifiers, CommandContext<S> executable) {
      if (executable.getCommand() == null) {
         throw new IllegalArgumentException("Last command in chain must be executable");
      }

      this.modifiers = modifiers;
      this.executable = executable;
   }

   public static <S> Optional<ContextChain<S>> tryFlatten(CommandContext<S> rootContext) {
      List<CommandContext<S>> modifiers = new ArrayList<>();
      CommandContext<S> current = rootContext;

      while (true) {
         CommandContext<S> child = current.getChild();
         if (child == null) {
            return current.getCommand() == null ? Optional.empty() : Optional.of(new ContextChain<>(modifiers, current));
         }

         modifiers.add(current);
         current = child;
      }
   }

   public static <S> Collection<S> runModifier(CommandContext<S> modifier, S source, ResultConsumer<S> resultConsumer, boolean forkedMode) throws CommandSyntaxException {
      RedirectModifier<S> sourceModifier = modifier.getRedirectModifier();
      if (sourceModifier == null) {
         return Collections.singleton(source);
      }

      CommandContext<S> contextToUse = modifier.copyFor(source);

      try {
         return sourceModifier.apply(contextToUse);
      } catch (CommandSyntaxException ex) {
         resultConsumer.onCommandComplete(contextToUse, false, 0);
         if (forkedMode) {
            return Collections.emptyList();
         } else {
            throw ex;
         }
      }
   }

   public static <S> int runExecutable(CommandContext<S> executable, S source, ResultConsumer<S> resultConsumer, boolean forkedMode) throws CommandSyntaxException {
      CommandContext<S> contextToUse = executable.copyFor(source);

      try {
         int result = executable.getCommand().run(contextToUse);
         resultConsumer.onCommandComplete(contextToUse, true, result);
         return forkedMode ? 1 : result;
      } catch (CommandSyntaxException ex) {
         resultConsumer.onCommandComplete(contextToUse, false, 0);
         if (forkedMode) {
            return 0;
         } else {
            throw ex;
         }
      }
   }

   public int executeAll(S source, ResultConsumer<S> resultConsumer) throws CommandSyntaxException {
      if (this.modifiers.isEmpty()) {
         return runExecutable(this.executable, source, resultConsumer, false);
      }

      boolean forkedMode = false;
      List<S> currentSources = Collections.singletonList(source);

      for (CommandContext<S> modifier : this.modifiers) {
         forkedMode |= modifier.isForked();
         List<S> nextSources = new ArrayList<>();

         for (S sourceToRun : currentSources) {
            nextSources.addAll(runModifier(modifier, sourceToRun, resultConsumer, forkedMode));
         }

         if (nextSources.isEmpty()) {
            return 0;
         }

         currentSources = nextSources;
      }

      int result = 0;

      for (S executionSource : currentSources) {
         result += runExecutable(this.executable, executionSource, resultConsumer, forkedMode);
      }

      return result;
   }

   public ContextChain.Stage getStage() {
      return this.modifiers.isEmpty() ? ContextChain.Stage.EXECUTE : ContextChain.Stage.MODIFY;
   }

   public CommandContext<S> getTopContext() {
      return this.modifiers.isEmpty() ? this.executable : this.modifiers.get(0);
   }

   public ContextChain<S> nextStage() {
      int modifierCount = this.modifiers.size();
      if (modifierCount == 0) {
         return null;
      }

      if (this.nextStageCache == null) {
         this.nextStageCache = new ContextChain<>(this.modifiers.subList(1, modifierCount), this.executable);
      }

      return this.nextStageCache;
   }

   public enum Stage {
      MODIFY,
      EXECUTE;
   }
}
