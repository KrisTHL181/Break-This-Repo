package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record MethodInfo<Params, Result>(String description, Optional<ParamInfo<Params>> params, Optional<ResultInfo<Result>> result) {
    public MethodInfo(final String description, final @Nullable ParamInfo<Params> paramInfo, final @Nullable ResultInfo<Result> resultInfo) {
        this(description, Optional.ofNullable(paramInfo), Optional.ofNullable(resultInfo));
    }

    private static <Params> Optional<ParamInfo<Params>> toOptional(final List<ParamInfo<Params>> list) {
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    private static <Params> List<ParamInfo<Params>> toList(final Optional<ParamInfo<Params>> opt) {
        return opt.isPresent() ? List.of(opt.get()) : List.of();
    }

    private static <Params> Codec<Optional<ParamInfo<Params>>> paramsTypedCodec() {
        return ParamInfo.<Params>typedCodec().codec().listOf().xmap(MethodInfo::toOptional, MethodInfo::toList);
    }

    private static <Params, Result> MapCodec<MethodInfo<Params, Result>> typedCodec() {
        return (MapCodec)RecordCodecBuilder.<MethodInfo>mapCodec(
            i -> i.group(
                    Codec.STRING.fieldOf("description").forGetter(MethodInfo::description),
                    paramsTypedCodec().fieldOf("params").forGetter(MethodInfo::params),
                    ResultInfo.<Result>typedCodec().optionalFieldOf("result").forGetter(MethodInfo::result)
                )
                .apply(i, MethodInfo::new)
        );
    }

    public MethodInfo.Named<Params, Result> named(final Identifier name) {
        return new MethodInfo.Named<>(name, this);
    }

    public record Named<Params, Result>(Identifier name, MethodInfo<Params, Result> contents) {
        public static final Codec<MethodInfo.Named<?, ?>> CODEC = (Codec)typedCodec();

        public static <Params, Result> Codec<MethodInfo.Named<Params, Result>> typedCodec() {
            return RecordCodecBuilder.create(
                i -> i.group(
                        Identifier.CODEC.fieldOf("name").forGetter(MethodInfo.Named::name),
                        MethodInfo.<Params, Result>typedCodec().forGetter(MethodInfo.Named::contents)
                    )
                    .apply(i, MethodInfo.Named::new)
            );
        }
    }
}
