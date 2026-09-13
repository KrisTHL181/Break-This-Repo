package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;

public class IdentifierArgument implements ArgumentType<Identifier> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

    public static IdentifierArgument id() {
        return new IdentifierArgument();
    }

    public static Identifier getId(final CommandContext<CommandSourceStack> context, final String name) {
        return context.getArgument(name, Identifier.class);
    }

    public Identifier parse(final StringReader reader) throws CommandSyntaxException {
        return Identifier.read(reader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}