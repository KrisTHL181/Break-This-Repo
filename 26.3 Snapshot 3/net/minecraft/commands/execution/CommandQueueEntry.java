package net.minecraft.commands.execution;

public record CommandQueueEntry<T>(Frame frame, EntryAction<T> action) {
   public void execute(final ExecutionContext<T> context) {
      this.action.execute(context, this.frame);
   }
}
