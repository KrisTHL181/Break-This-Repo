package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public record ShaderDefines(Map<String, String> values, Set<String> flags) {
   public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
   public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
         )
         .apply(i, ShaderDefines::new)
   );

   public static ShaderDefines.Builder builder() {
      return new ShaderDefines.Builder();
   }

   public ShaderDefines withOverrides(final ShaderDefines defines) {
      if (this.isEmpty()) {
         return defines;
      }

      if (defines.isEmpty()) {
         return this;
      }

      com.google.common.collect.ImmutableMap.Builder<String, String> newValues = ImmutableMap.builderWithExpectedSize(
         this.values.size() + defines.values.size()
      );
      newValues.putAll(this.values);
      newValues.putAll(defines.values);
      com.google.common.collect.ImmutableSet.Builder<String> newFlags = ImmutableSet.builderWithExpectedSize(this.flags.size() + defines.flags.size());
      newFlags.addAll(this.flags);
      newFlags.addAll(defines.flags);
      return new ShaderDefines(newValues.buildKeepingLast(), newFlags.build());
   }

   public String asSourceDirectives() {
      StringBuilder directives = new StringBuilder();

      for (Entry<String, String> entry : this.values.entrySet()) {
         String key = entry.getKey();
         String value = entry.getValue();
         directives.append("#define ").append(key).append(" ").append(value).append('\n');
      }

      for (String flag : this.flags) {
         directives.append("#define ").append(flag).append('\n');
      }

      return directives.toString();
   }

   public boolean isEmpty() {
      return this.values.isEmpty() && this.flags.isEmpty();
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
      private final com.google.common.collect.ImmutableSet.Builder<String> flags = ImmutableSet.builder();

      private Builder() {
      }

      public ShaderDefines.Builder define(final String key, final String value) {
         if (value.isBlank()) {
            throw new IllegalArgumentException("Cannot define empty string");
         }

         this.values.put(key, escapeNewLines(value));
         return this;
      }

      private static String escapeNewLines(final String value) {
         return value.replaceAll("\n", "\\\\\n");
      }

      public ShaderDefines.Builder define(final String key, final float value) {
         this.values.put(key, String.valueOf(value));
         return this;
      }

      public ShaderDefines.Builder define(final String key, final int value) {
         this.values.put(key, String.valueOf(value));
         return this;
      }

      public ShaderDefines.Builder define(final String key) {
         this.flags.add(key);
         return this;
      }

      public ShaderDefines build() {
         return new ShaderDefines(this.values.build(), this.flags.build());
      }
   }
}
