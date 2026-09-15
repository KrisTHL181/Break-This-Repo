package net.minecraft.commands.functions;

import java.util.List;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.resources.Identifier;

public interface InstantiatedFunction<T> {
    Identifier id();

    List<UnboundEntryAction<T>> entries();
}