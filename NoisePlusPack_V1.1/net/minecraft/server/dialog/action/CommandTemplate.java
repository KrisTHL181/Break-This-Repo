package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;

public record CommandTemplate(ParsedTemplate template) implements Action {
   public static final MapCodec<CommandTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_406000_ -> p_406000_.group(ParsedTemplate.CODEC.fieldOf("template").forGetter(CommandTemplate::template)).apply(p_406000_, CommandTemplate::new)
   );

   @Override
   public MapCodec<CommandTemplate> codec() {
      return MAP_CODEC;
   }

   @Override
   public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> p_406266_) {
      String s = this.template.instantiate(Action.ValueGetter.getAsTemplateSubstitutions(p_406266_));
      return Optional.of(new ClickEvent.RunCommand(s));
   }
}
