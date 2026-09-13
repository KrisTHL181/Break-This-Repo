package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record Message(Optional<String> literal, Optional<String> translatable, Optional<List<String>> translatableParams) {
   public static final Codec<Message> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.optionalFieldOf("literal").forGetter(Message::literal),
            Codec.STRING.optionalFieldOf("translatable").forGetter(Message::translatable),
            Codec.STRING.listOf().lenientOptionalFieldOf("translatableParams").forGetter(Message::translatableParams)
         )
         .apply(i, Message::new)
   );

   public Optional<Component> asComponent() {
      if (this.translatable.isPresent()) {
         String translationKey = this.translatable.get();
         if (this.translatableParams.isPresent()) {
            List<String> translationArgs = this.translatableParams.get();
            return Optional.of(Component.translatable(translationKey, translationArgs.toArray()));
         } else {
            return Optional.of(Component.translatable(translationKey));
         }
      } else {
         return this.literal.map(Component::literal);
      }
   }
}
