package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ResultInfo<Result>(String name, Schema<Result> schema) {
   // ===== 修改：显式指定 RecordCodecBuilder 类型参数，并显式指定 Schema.typedCodec 的泛型 =====
   public static <Result> Codec<ResultInfo<Result>> typedCodec() {
      return RecordCodecBuilder.<ResultInfo<Result>>create(
         i -> i.group(
               Codec.STRING.fieldOf("name").forGetter(ResultInfo::name),
               Schema.<Result>typedCodec().fieldOf("schema").forGetter(ResultInfo::schema)
            )
            .apply(i, ResultInfo::new)
      );
   }
}