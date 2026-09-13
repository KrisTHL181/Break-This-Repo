package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ParamInfo<Param>(String name, Schema<Param> schema, boolean required) {
   public ParamInfo(String p_428974_, Schema<Param> p_426709_) {
      this(p_428974_, p_426709_, true);
   }

   public static <Param> MapCodec<ParamInfo<Param>> typedCodec() {
      return RecordCodecBuilder.mapCodec(
         p_449118_ -> p_449118_.group(
               Codec.STRING.fieldOf("name").forGetter(ParamInfo::name),
               Schema.typedCodec().fieldOf("schema").forGetter(ParamInfo::schema),
               Codec.BOOL.fieldOf("required").forGetter(ParamInfo::required)
            )
            .apply(p_449118_, ParamInfo::new)
      );
   }
}
