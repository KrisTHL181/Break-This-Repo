package net.minecraft.commands.execution;

@FunctionalInterface
public interface UnboundEntryAction<T> {
   void execute(T sender, ExecutionContext<T> context, Frame frame);

   default EntryAction<T> bind(final T sender) {
      return (context, frame) -> this.execute(sender, context, frame);
   }
}
