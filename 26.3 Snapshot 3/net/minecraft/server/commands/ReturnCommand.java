package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ContextChain;
import java.util.List;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.FallthroughTask;

public class ReturnCommand {
   public static <T extends ExecutionCommandSource<T>> void register(final CommandDispatcher<T> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)LiteralArgumentBuilder.literal("return")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(RequiredArgumentBuilder.argument("value", IntegerArgumentType.integer()).executes(new ReturnCommand.ReturnValueCustomExecutor())))
               .then(LiteralArgumentBuilder.literal("fail").executes(new ReturnCommand.ReturnFailCustomExecutor())))
            .then(LiteralArgumentBuilder.literal("run").forward(dispatcher.getRoot(), new ReturnCommand.ReturnFromCommandCustomModifier(), false))
      );
   }

   private static class ReturnFailCustomExecutor<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor.CommandAdapter<T> {
      public void run(final T sender, final ContextChain<T> currentStep, final ChainModifiers modifiers, final ExecutionControl<T> output) {
         sender.callback().onFailure();
         Frame frame = output.currentFrame();
         frame.returnFailure();
         frame.discard();
      }
   }

   private static class ReturnFromCommandCustomModifier<T extends ExecutionCommandSource<T>> implements CustomModifierExecutor.ModifierAdapter<T> {
      public void apply(
         final T originalSource,
         final List<T> currentSources,
         final ContextChain<T> currentStep,
         final ChainModifiers modifiers,
         final ExecutionControl<T> output
      ) {
         if (currentSources.isEmpty()) {
            if (modifiers.isReturn()) {
               output.queueNext(FallthroughTask.instance());
            }
         } else {
            output.currentFrame().discard();
            ContextChain<T> nextState = currentStep.nextStage();
            String command = nextState.getTopContext().getInput();
            output.queueNext(new BuildContexts.Continuation<>(command, nextState, modifiers.setReturn(), originalSource, currentSources));
         }
      }
   }

   private static class ReturnValueCustomExecutor<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor.CommandAdapter<T> {
      public void run(final T sender, final ContextChain<T> currentStep, final ChainModifiers modifiers, final ExecutionControl<T> output) {
         int returnValue = IntegerArgumentType.getInteger(currentStep.getTopContext(), "value");
         sender.callback().onSuccess(returnValue);
         Frame frame = output.currentFrame();
         frame.returnSuccess(returnValue);
         frame.discard();
      }
   }
}
