package dev.miru.options.base;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class SettingItem<T> {
   private T value;
   private T defaultValue;
   private String settingId = "";
   private Function<T, String> toString;
   private BiFunction<T, SettingItem<T>, SettingItem<T>> toggleFunction = (vx, s) -> s;
   private Function<String, T> parseFunction = str -> null;
   public static final BiFunction<Boolean, SettingItem<Boolean>, SettingItem<Boolean>> BOOL_REVERSE_ACTION = (v, s) -> s.setValue(!v);
   public static final Function<String, Double> DOUBLE_PARSE_ACTION = Double::parseDouble;
   public static final Function<String, Boolean> BOOLEAN_PARSE_ACTION = Boolean::parseBoolean;

   public SettingItem(T v, Function<T, String> toString) {
      if (v != null) {
         this.value = v;
         this.defaultValue = v;
         this.toString = toString == null ? Object::toString : toString;
      } else {
         throw new IllegalArgumentException("Value cannot be null");
      }
   }

   public SettingItem(T v) {
      this(v, null);
   }

   public T getValue() {
      return this.value;
   }

   public SettingItem<T> setDefaultValue(T value) {
      if (value != null) {
         this.defaultValue = value;
         return this;
      } else {
         throw new IllegalArgumentException("Value can't be null");
      }
   }

   public SettingItem<T> setToggleFunction(BiFunction<T, SettingItem<T>, SettingItem<T>> function) {
      if (function != null) {
         this.toggleFunction = function;
      }

      return this;
   }

   public SettingItem<T> setParseFunction(Function<String, T> function) {
      if (function != null) {
         this.parseFunction = function;
      }

      return this;
   }

   public SettingItem<T> settingId(String id) {
      this.settingId = id;
      return this;
   }

   public SettingItem<T> setNonToggleable() {
      return this.setToggleFunction(null);
   }

   public SettingItem<T> toggle() {
      if (this.toggleFunction == null) {
         throw new RuntimeException("Trying to call a null toggle function");
      } else {
         return this.toggleFunction.apply(this.value, this);
      }
   }

   public SettingItem<T> parse(String string) {
      if (this.parseFunction == null) {
         throw new RuntimeException("Trying to call null parse function");
      }

      this.value = this.parseFunction.apply(string);
      return this;
   }

   public SettingItem<T> parse(HashMap<String, String> map) {
      if (!map.containsKey(this.settingId)) {
         throw new RuntimeException("No key %s in map!".formatted(this.settingId));
      } else {
         return this.parse(map.get(this.settingId));
      }
   }

   public void reset() {
      this.value = this.defaultValue;
   }

   public SettingItem<T> setValue(T v) {
      if (v != null) {
         this.value = v;
         return this;
      } else {
         throw new IllegalArgumentException("Value cannot be null");
      }
   }

   @Override
   public String toString() {
      return this.toString.apply(this.value);
   }

   public Component toComponent() {
      return Component.literal(this.toString.apply(this.value));
   }

   public Tooltip toTooltip() {
      return Tooltip.create(this.toComponent());
   }

   public String settingId() {
      return this.settingId;
   }
}
