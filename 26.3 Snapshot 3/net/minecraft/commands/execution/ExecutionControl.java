package net.minecraft.commands.execution;

import net.minecraft.commands.ExecutionCommandSource;
import org.jspecify.annotations.Nullable;

public interface ExecutionControl<T> {
   void queueNext(EntryAction<T> action);

   void tracer(@Nullable TraceCallbacks tracer);

   @Nullable TraceCallbacks tracer();

   Frame currentFrame();

   static <T extends ExecutionCommandSource<T>> ExecutionControl<T> create(final ExecutionContext<T> context, final Frame frame) {
      return new ExecutionControl<T>() {
         @Override
         public void queueNext(final EntryAction<T> action) {
            context.queueNext(new CommandQueueEntry<>(frame, action));
         }

         @Override
         public void tracer(final @Nullable TraceCallbacks tracer) {
            context.tracer(tracer);
         }

         @Override
         public @Nullable TraceCallbacks tracer() {
            return context.tracer();
         }

         @Override
         public Frame currentFrame() {
            return frame;
         }
      };
   }
}
