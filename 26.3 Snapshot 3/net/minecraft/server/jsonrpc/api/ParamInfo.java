package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ParamInfo<Param>(String name, Schema<Param> schema, boolean required) {
   public ParamInfo(final String name, final Schema<Param> schema) {
      this(name, schema, true);
   }

   // ===== 修改：显式指定 RecordCodecBuilder 类型参数，并显式指定 Schema.typedCodec 的泛型 =====
   public static <Param> MapCodec<ParamInfo<Param>> typedCodec() {
      return RecordCodecBuilder.<ParamInfo<Param>>mapCodec(
         i -> i.group(
               Codec.STRING.fieldOf("name").forGetter(ParamInfo::name),
               Schema.<Param>typedCodec().fieldOf("schema").forGetter(ParamInfo::schema),
               Codec.BOOL.fieldOf("required").forGetter(ParamInfo::required)
            )
            .apply(i, ParamInfo::new)
      );
   }
}