package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.util.Util;

public record StaticAction(ClickEvent value) implements Action {
   public static final Map<ClickEvent.Action, MapCodec<StaticAction>> WRAPPED_CODECS = Util.make(() -> {
      Map<ClickEvent.Action, MapCodec<StaticAction>> map = new EnumMap<>(ClickEvent.Action.class);

      for (ClickEvent.Action clickevent$action : ClickEvent.Action.class.getEnumConstants()) {
         if (clickevent$action.isAllowedFromServer()) {
            MapCodec<ClickEvent> mapcodec = clickevent$action.valueCodec();
            map.put(clickevent$action, mapcodec.xmap(StaticAction::new, StaticAction::value));
         }
      }

      return Collections.unmodifiableMap(map);
   });

   @Override
   public MapCodec<StaticAction> codec() {
      return WRAPPED_CODECS.get(this.value.action());
   }

   @Override
   public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> p_408882_) {
      return Optional.of(this.value);
   }
}
