package net.minecraft.commands.execution;

import net.minecraft.commands.ExecutionCommandSource;
import org.jspecify.annotations.Nullable;

public interface ExecutionControl<T> {
   void queueNext(EntryAction<T> var1);

   void tracer(@Nullable TraceCallbacks var1);

   @Nullable TraceCallbacks tracer();

   Frame currentFrame();

   static <T extends ExecutionCommandSource<T>> ExecutionControl<T> create(final ExecutionContext<T> p_310088_, final Frame p_312154_) {
      return new ExecutionControl<T>() {
         @Override
         public void queueNext(EntryAction<T> p_311389_) {
            p_310088_.queueNext(new CommandQueueEntry<>(p_312154_, p_311389_));
         }

         @Override
         public void tracer(@Nullable TraceCallbacks p_313185_) {
            p_310088_.tracer(p_313185_);
         }

         @Override
         public @Nullable TraceCallbacks tracer() {
            return p_310088_.tracer();
         }

         @Override
         public Frame currentFrame() {
            return p_312154_;
         }
      };
   }
}
