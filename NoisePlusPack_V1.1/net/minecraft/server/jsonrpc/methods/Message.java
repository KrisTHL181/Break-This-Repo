package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record Message(Optional<String> literal, Optional<String> translatable, Optional<List<String>> translatableParams) {
   public static final Codec<Message> CODEC = RecordCodecBuilder.create(
      p_423029_ -> p_423029_.group(
            Codec.STRING.optionalFieldOf("literal").forGetter(Message::literal),
            Codec.STRING.optionalFieldOf("translatable").forGetter(Message::translatable),
            Codec.STRING.listOf().lenientOptionalFieldOf("translatableParams").forGetter(Message::translatableParams)
         )
         .apply(p_423029_, Message::new)
   );

   public Optional<Component> asComponent() {
      if (this.translatable.isPresent()) {
         String s = this.translatable.get();
         if (this.translatableParams.isPresent()) {
            List<String> list = this.translatableParams.get();
            return Optional.of(Component.translatable(s, list.toArray()));
         } else {
            return Optional.of(Component.translatable(s));
         }
      } else {
         return this.literal.map(Component::literal);
      }
   }
}
