package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
   public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
   public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(
      p_369294_ -> p_369294_.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)
         )
         .apply(p_369294_, ShaderDefines::new)
   );

   public static ShaderDefines.Builder builder() {
      return new ShaderDefines.Builder();
   }

   public ShaderDefines withOverrides(ShaderDefines p_361942_) {
      if (this.isEmpty()) {
         return p_361942_;
      }

      if (p_361942_.isEmpty()) {
         return this;
      }

      com.google.common.collect.ImmutableMap.Builder<String, String> builder = ImmutableMap.builderWithExpectedSize(
         this.values.size() + p_361942_.values.size()
      );
      builder.putAll(this.values);
      builder.putAll(p_361942_.values);
      com.google.common.collect.ImmutableSet.Builder<String> builder1 = ImmutableSet.builderWithExpectedSize(this.flags.size() + p_361942_.flags.size());
      builder1.addAll(this.flags);
      builder1.addAll(p_361942_.flags);
      return new ShaderDefines(builder.buildKeepingLast(), builder1.build());
   }

   public String asSourceDirectives() {
      StringBuilder stringbuilder = new StringBuilder();

      for (Entry<String, String> entry : this.values.entrySet()) {
         String s = entry.getKey();
         String s1 = entry.getValue();
         stringbuilder.append("#define ").append(s).append(" ").append(s1).append('\n');
      }

      for (String s2 : this.flags) {
         stringbuilder.append("#define ").append(s2).append('\n');
      }

      return stringbuilder.toString();
   }

   public boolean isEmpty() {
      return this.values.isEmpty() && this.flags.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
      private final com.google.common.collect.ImmutableSet.Builder<String> flags = ImmutableSet.builder();

      Builder() {
      }

      public ShaderDefines.Builder define(String p_360918_, String p_368570_) {
         if (p_368570_.isBlank()) {
            throw new IllegalArgumentException("Cannot define empty string");
         }

         this.values.put(p_360918_, escapeNewLines(p_368570_));
         return this;
      }

      private static String escapeNewLines(String p_363744_) {
         return p_363744_.replaceAll("\n", "\\\\\n");
      }

      public ShaderDefines.Builder define(String p_363194_, float p_365800_) {
         this.values.put(p_363194_, String.valueOf(p_365800_));
         return this;
      }

      public ShaderDefines.Builder define(String p_395500_, int p_397721_) {
         this.values.put(p_395500_, String.valueOf(p_397721_));
         return this;
      }

      public ShaderDefines.Builder define(String p_367054_) {
         this.flags.add(p_367054_);
         return this;
      }

      public ShaderDefines build() {
         return new ShaderDefines(this.values.build(), this.flags.build());
      }
   }
}
