package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record MethodInfo<Params, Result>(String description, Optional<ParamInfo<Params>> params, Optional<ResultInfo<Result>> result) {
   public MethodInfo(String p_428854_, @Nullable ParamInfo<Params> p_425825_, @Nullable ResultInfo<Result> p_431170_) {
      this(p_428854_, Optional.ofNullable(p_425825_), Optional.ofNullable(p_431170_));
   }

   private static <Params> Optional<ParamInfo<Params>> toOptional(List<ParamInfo<Params>> p_460952_) {
      return p_460952_.isEmpty() ? Optional.empty() : Optional.of(p_460952_.getFirst());
   }

   private static <Params> List<ParamInfo<Params>> toList(Optional<ParamInfo<Params>> p_459308_) {
      return p_459308_.isPresent() ? List.of(p_459308_.get()) : List.of();
   }

   private static <Params> Codec<Optional<ParamInfo<Params>>> paramsTypedCodec() {
      return ParamInfo.typedCodec().codec().listOf().xmap(MethodInfo::toOptional, MethodInfo::toList);
   }

   static <Params, Result> MapCodec<MethodInfo<Params, Result>> typedCodec() {
      return RecordCodecBuilder.mapCodec(
         p_449116_ -> p_449116_.group(
               Codec.STRING.fieldOf("description").forGetter(MethodInfo::description),
               paramsTypedCodec().fieldOf("params").forGetter(MethodInfo::params),
               ResultInfo.typedCodec().optionalFieldOf("result").forGetter(MethodInfo::result)
            )
            .apply(p_449116_, MethodInfo::new)
      );
   }

   public MethodInfo.Named<Params, Result> named(Identifier p_457380_) {
      return new MethodInfo.Named<>(p_457380_, this);
   }

   public record Named<Params, Result>(Identifier name, MethodInfo<Params, Result> contents) {
      public static final Codec<MethodInfo.Named<?, ?>> CODEC = typedCodec();

      public static <Params, Result> Codec<MethodInfo.Named<Params, Result>> typedCodec() {
         return RecordCodecBuilder.create(
            p_449117_ -> p_449117_.group(
                  Identifier.CODEC.fieldOf("name").forGetter(MethodInfo.Named::name), MethodInfo.typedCodec().forGetter(MethodInfo.Named::contents)
               )
               .apply(p_449117_, MethodInfo.Named::new)
         );
      }
   }
}
