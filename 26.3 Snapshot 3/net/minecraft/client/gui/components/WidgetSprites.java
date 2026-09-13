package net.minecraft.client.gui.components;

import net.minecraft.resources.Identifier;

public record WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused, Identifier disabledFocused) {
   public WidgetSprites(final Identifier sprite) {
      this(sprite, sprite, sprite, sprite);
   }

   public WidgetSprites(final Identifier sprite, final Identifier focused) {
      this(sprite, sprite, focused, focused);
   }

   public WidgetSprites(final Identifier enabled, final Identifier disabled, final Identifier focused) {
      this(enabled, disabled, focused, disabled);
   }

   public Identifier get(final boolean enabled, final boolean focused) {
      if (enabled) {
         return focused ? this.enabledFocused : this.enabled;
      } else {
         return focused ? this.disabledFocused : this.disabled;
      }
   }
}
