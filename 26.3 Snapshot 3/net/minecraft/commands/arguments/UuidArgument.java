package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class UuidArgument implements ArgumentType<UUID> {
   public static final SimpleCommandExceptionType ERROR_INVALID_UUID = new SimpleCommandExceptionType(Component.translatable("argument.uuid.invalid"));
   private static final Collection<String> EXAMPLES = Arrays.asList("dd12be42-52a9-4a91-a8a1-11c01849e498");
   private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^([-A-Fa-f0-9]+)");

   public static UUID getUuid(final CommandContext<CommandSourceStack> source, final String name) {
      return (UUID)source.getArgument(name, UUID.class);
   }

   public static UuidArgument uuid() {
      return new UuidArgument();
   }

   public UUID parse(final StringReader reader) throws CommandSyntaxException {
      String remaining = reader.getRemaining();
      Matcher matcher = ALLOWED_CHARACTERS.matcher(remaining);
      if (matcher.find()) {
         String maybeUUID = matcher.group(1);

         try {
            UUID result = UUID.fromString(maybeUUID);
            reader.setCursor(reader.getCursor() + maybeUUID.length());
            return result;
         } catch (IllegalArgumentException var6) {
         }
      }

      throw ERROR_INVALID_UUID.createWithContext(reader);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
