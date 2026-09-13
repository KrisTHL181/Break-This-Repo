package net.minecraft.commands.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.context.ContextChain.Stage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.network.chat.Component;

public class BuildContexts<T extends ExecutionCommandSource<T>> {
   @VisibleForTesting
   public static final DynamicCommandExceptionType ERROR_FORK_LIMIT_REACHED = new DynamicCommandExceptionType(
      p_311924_ -> Component.translatableEscape("command.forkLimit", p_311924_)
   );
   private final String commandInput;
   private final ContextChain<T> command;

   public BuildContexts(String p_310420_, ContextChain<T> p_313082_) {
      this.commandInput = p_310420_;
      this.command = p_313082_;
   }

   protected void execute(T p_309755_, List<T> p_310231_, ExecutionContext<T> p_311779_, Frame p_313162_, ChainModifiers p_310618_) {
      ContextChain<T> contextchain = this.command;
      ChainModifiers chainmodifiers = p_310618_;
      List<T> list = p_310231_;
      if (contextchain.getStage() != Stage.EXECUTE) {
         p_311779_.profiler().push(() -> "prepare " + this.commandInput);

         try {
            int i = p_311779_.forkLimit();

            while (contextchain.getStage() != Stage.EXECUTE) {
               CommandContext<T> commandcontext = contextchain.getTopContext();
               if (commandcontext.isForked()) {
                  chainmodifiers = chainmodifiers.setForked();
               }

               RedirectModifier<T> redirectmodifier = commandcontext.getRedirectModifier();
               if (redirectmodifier instanceof CustomModifierExecutor custommodifierexecutor) {
                  custommodifierexecutor.apply(p_309755_, list, contextchain, chainmodifiers, ExecutionControl.create(p_311779_, p_313162_));
                  return;
               }

               if (redirectmodifier != null) {
                  p_311779_.incrementCost();
                  boolean flag = chainmodifiers.isForked();
                  List<T> list1 = new ObjectArrayList();

                  for (T t : list) {
                     try {
                        Collection<T> collection = ContextChain.runModifier(commandcontext, t, (p_311026_, p_312291_, p_310245_) -> {}, flag);
                        if (list1.size() + collection.size() >= i) {
                           p_309755_.handleError(ERROR_FORK_LIMIT_REACHED.create(i), flag, p_311779_.tracer());
                           return;
                        }

                        list1.addAll(collection);
                     } catch (CommandSyntaxException commandsyntaxexception) {
                        t.handleError(commandsyntaxexception, flag, p_311779_.tracer());
                        if (!flag) {
                           return;
                        }
                     }
                  }

                  list = list1;
               }

               contextchain = contextchain.nextStage();
            }
         } finally {
            p_311779_.profiler().pop();
         }
      }

      if (list.isEmpty()) {
         if (chainmodifiers.isReturn()) {
            p_311779_.queueNext(new CommandQueueEntry<>(p_313162_, FallthroughTask.instance()));
         }
      } else {
         CommandContext<T> commandcontext1 = contextchain.getTopContext();
         if (commandcontext1.getCommand() instanceof CustomCommandExecutor customcommandexecutor) {
            ExecutionControl<T> executioncontrol = ExecutionControl.create(p_311779_, p_313162_);

            for (T t2 : list) {
               customcommandexecutor.run(t2, contextchain, chainmodifiers, executioncontrol);
            }
         } else {
            if (chainmodifiers.isReturn()) {
               T t1 = list.get(0);
               t1 = t1.withCallback(CommandResultCallback.chain(t1.callback(), p_313162_.returnValueConsumer()));
               list = List.of(t1);
            }

            ExecuteCommand<T> executecommand = new ExecuteCommand<>(this.commandInput, chainmodifiers, commandcontext1);
            ContinuationTask.schedule(p_311779_, p_313162_, list, (p_311832_, p_309437_) -> new CommandQueueEntry<>(p_311832_, executecommand.bind(p_309437_)));
         }
      }
   }

   protected void traceCommandStart(ExecutionContext<T> p_311913_, Frame p_312311_) {
      TraceCallbacks tracecallbacks = p_311913_.tracer();
      if (tracecallbacks != null) {
         tracecallbacks.onCommand(p_312311_.depth(), this.commandInput);
      }
   }

   @Override
   public String toString() {
      return this.commandInput;
   }

   public static class Continuation<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final ChainModifiers modifiers;
      private final T originalSource;
      private final List<T> sources;

      public Continuation(String p_312336_, ContextChain<T> p_312118_, ChainModifiers p_311446_, T p_312390_, List<T> p_311252_) {
         super(p_312336_, p_312118_);
         this.originalSource = p_312390_;
         this.sources = p_311252_;
         this.modifiers = p_311446_;
      }

      @Override
      public void execute(ExecutionContext<T> p_310784_, Frame p_310508_) {
         this.execute(this.originalSource, this.sources, p_310784_, p_310508_, this.modifiers);
      }
   }

   public static class TopLevel<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final T source;

      public TopLevel(String p_312552_, ContextChain<T> p_309758_, T p_313175_) {
         super(p_312552_, p_309758_);
         this.source = p_313175_;
      }

      @Override
      public void execute(ExecutionContext<T> p_310161_, Frame p_311746_) {
         this.traceCommandStart(p_310161_, p_311746_);
         this.execute(this.source, List.of(this.source), p_310161_, p_311746_, ChainModifiers.DEFAULT);
      }
   }

   public static class Unbound<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements UnboundEntryAction<T> {
      public Unbound(String p_312191_, ContextChain<T> p_309892_) {
         super(p_312191_, p_309892_);
      }

      public void execute(T p_310320_, ExecutionContext<T> p_313071_, Frame p_310123_) {
         this.traceCommandStart(p_313071_, p_310123_);
         this.execute(p_310320_, List.of(p_310320_), p_313071_, p_310123_, ChainModifiers.DEFAULT);
      }
   }
}
