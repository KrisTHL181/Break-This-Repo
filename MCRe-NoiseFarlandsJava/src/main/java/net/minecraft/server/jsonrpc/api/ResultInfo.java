package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ResultInfo<Result>(String name, Schema<Result> schema) {
    public static <Result> Codec<ResultInfo<Result>> typedCodec() {
        return RecordCodecBuilder.create(
            i -> i.group(Codec.STRING.fieldOf("name").forGetter(ResultInfo::name), Schema.<Result>typedCodec().fieldOf("schema").forGetter(ResultInfo::schema))
                .apply(i, ResultInfo::new)
        );
    }
}