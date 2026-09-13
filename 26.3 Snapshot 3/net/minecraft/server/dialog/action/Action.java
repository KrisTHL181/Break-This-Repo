package net.minecraft.server.dialog.action;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;

public interface Action {
   Codec<Action> CODEC = BuiltInRegistries.DIALOG_ACTION_TYPE.byNameCodec().dispatch(Action::codec, c -> c);

   MapCodec<? extends Action> codec();

   Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> parameters);

   interface ValueGetter {
      String asTemplateSubstitution();

      Tag asTag();

      static Map<String, String> getAsTemplateSubstitutions(final Map<String, Action.ValueGetter> parameters) {
         return Maps.transformValues(parameters, Action.ValueGetter::asTemplateSubstitution);
      }

      static Action.ValueGetter of(final String value) {
         return new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return value;
            }

            @Override
            public Tag asTag() {
               return StringTag.valueOf(value);
            }
         };
      }

      static Action.ValueGetter of(final Supplier<String> value) {
         return new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return value.get();
            }

            @Override
            public Tag asTag() {
               return StringTag.valueOf(value.get());
            }
         };
      }
   }
}
