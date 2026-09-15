package net.minecraft.commands.execution;

@FunctionalInterface
public interface UnboundEntryAction<T> {
   void execute(T var1, ExecutionContext<T> var2, Frame var3);

   default EntryAction<T> bind(T p_312071_) {
      return (p_309583_, p_311194_) -> this.execute(p_312071_, p_309583_, p_311194_);
   }
}
