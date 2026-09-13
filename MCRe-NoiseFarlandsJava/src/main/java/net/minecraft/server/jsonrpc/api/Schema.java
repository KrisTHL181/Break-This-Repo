package net.minecraft.server.jsonrpc.api;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.DiscoveryService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.Message;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.PlayerService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRuleType;

public record Schema<T>(
    Optional<URI> reference, List<String> type, Optional<Schema<?>> items, Map<String, Schema<?>> properties, List<String> enumValues, Codec<T> codec
) {
    public static final Codec<? extends Schema<?>> CODEC = (Codec)Codec.<Schema>recursive(
            "Schema",
            subCodec -> RecordCodecBuilder.create(
                i -> i.group(
                        ReferenceUtil.REFERENCE_CODEC.optionalFieldOf("$ref").forGetter(Schema::reference),
                        ExtraCodecs.compactListCodec(Codec.STRING)
                            .optionalFieldOf("type", List.of())
                            .forGetter(Schema::type),
                        subCodec.optionalFieldOf("items").forGetter(Schema::items),
                        Codec.unboundedMap(Codec.STRING, subCodec)
                            .optionalFieldOf("properties", Map.of())
                            .forGetter(Schema::properties),
                        Codec.STRING.listOf().optionalFieldOf("enum", List.of()).forGetter(Schema::enumValues)
                    )
                    .apply(i, (ref, type, items, properties, enumValues) -> null)
            )
        )
        .validate(schema -> schema == null ? DataResult.error(() -> "Should not deserialize schema") : DataResult.success(schema));
    private static final List<SchemaComponent<?>> SCHEMA_REGISTRY = new ArrayList<>();
    public static final Schema<Boolean> BOOL_SCHEMA = ofType("boolean", Codec.BOOL);
    public static final Schema<Integer> INT_SCHEMA = ofType("integer", Codec.INT);
    public static final Schema<Either<Boolean, Integer>> BOOL_OR_INT_SCHEMA = ofTypes(List.of("boolean", "integer"), Codec.either(Codec.BOOL, Codec.INT));
    public static final Schema<Float> NUMBER_SCHEMA = ofType("number", Codec.FLOAT);
    public static final Schema<String> STRING_SCHEMA = ofType("string", Codec.STRING);
    public static final Schema<UUID> UUID_SCHEMA = ofType("string", UUIDUtil.CODEC);
    public static final Schema<DiscoveryService.DiscoverResponse> DISCOVERY_SCHEMA = ofType("string", DiscoveryService.DiscoverResponse.CODEC.codec());
    public static final SchemaComponent<Difficulty> DIFFICULTY_SCHEMA = registerSchema("difficulty", ofEnum(Difficulty::values, Difficulty.CODEC));
    public static final SchemaComponent<GameType> GAME_TYPE_SCHEMA = registerSchema("game_type", ofEnum(GameType::values, GameType.CODEC));
    public static final Schema<PermissionLevel> PERMISSION_LEVEL_SCHEMA = ofType("integer", PermissionLevel.INT_CODEC);
    public static final SchemaComponent<PlayerDto> PLAYER_SCHEMA = registerSchema(
        "player", record(PlayerDto.CODEC.codec()).withField("id", UUID_SCHEMA).withField("name", STRING_SCHEMA)
    );
    public static final SchemaComponent<DiscoveryService.DiscoverInfo> VERSION_SCHEMA = registerSchema(
        "version", record(DiscoveryService.DiscoverInfo.CODEC.codec()).withField("name", STRING_SCHEMA).withField("protocol", INT_SCHEMA)
    );
    public static final SchemaComponent<ServerStateService.ServerState> SERVER_STATE_SCHEMA = registerSchema(
        "server_state",
        record(ServerStateService.ServerState.CODEC)
            .withField("started", BOOL_SCHEMA)
            .withField("players", PLAYER_SCHEMA.asRef().asArray())
            .withField("version", VERSION_SCHEMA.asRef())
    );
    public static final Schema<GameRuleType> RULE_TYPE_SCHEMA = ofEnum(GameRuleType::values);
    public static final SchemaComponent<GameRulesService.GameRuleUpdate<?>> TYPED_GAME_RULE_SCHEMA = registerSchema(
        "typed_game_rule",
        record(GameRulesService.GameRuleUpdate.TYPED_CODEC)
            .withField("key", STRING_SCHEMA)
            .withField("value", BOOL_OR_INT_SCHEMA)
            .withField("type", RULE_TYPE_SCHEMA)
    );
    public static final SchemaComponent<GameRulesService.GameRuleUpdate<?>> UNTYPED_GAME_RULE_SCHEMA = registerSchema(
        "untyped_game_rule", record(GameRulesService.GameRuleUpdate.CODEC).withField("key", STRING_SCHEMA).withField("value", BOOL_OR_INT_SCHEMA)
    );
    public static final SchemaComponent<Message> MESSAGE_SCHEMA = registerSchema(
        "message",
        record(Message.CODEC)
            .withField("literal", STRING_SCHEMA)
            .withField("translatable", STRING_SCHEMA)
            .withField("translatableParams", STRING_SCHEMA.asArray())
    );
    public static final SchemaComponent<ServerStateService.SystemMessage> SYSTEM_MESSAGE_SCHEMA = registerSchema(
        "system_message",
        record(ServerStateService.SystemMessage.CODEC)
            .withField("message", MESSAGE_SCHEMA.asRef())
            .withField("overlay", BOOL_SCHEMA)
            .withField("receivingPlayers", PLAYER_SCHEMA.asRef().asArray())
    );
    public static final SchemaComponent<PlayerService.KickDto> KICK_PLAYER_SCHEMA = registerSchema(
        "kick_player", record(PlayerService.KickDto.CODEC.codec()).withField("message", MESSAGE_SCHEMA.asRef()).withField("player", PLAYER_SCHEMA.asRef())
    );
    public static final SchemaComponent<OperatorService.OperatorDto> OPERATOR_SCHEMA = registerSchema(
        "operator",
        record(OperatorService.OperatorDto.CODEC.codec())
            .withField("player", PLAYER_SCHEMA.asRef())
            .withField("bypassesPlayerLimit", BOOL_SCHEMA)
            .withField("permissionLevel", INT_SCHEMA)
    );
    public static final SchemaComponent<IpBanlistService.IncomingIpBanDto> INCOMING_IP_BAN_SCHEMA = registerSchema(
        "incoming_ip_ban",
        record(IpBanlistService.IncomingIpBanDto.CODEC.codec())
            .withField("player", PLAYER_SCHEMA.asRef())
            .withField("ip", STRING_SCHEMA)
            .withField("reason", STRING_SCHEMA)
            .withField("source", STRING_SCHEMA)
            .withField("expires", STRING_SCHEMA)
    );
    public static final SchemaComponent<IpBanlistService.IpBanDto> IP_BAN_SCHEMA = registerSchema(
        "ip_ban",
        record(IpBanlistService.IpBanDto.CODEC.codec())
            .withField("ip", STRING_SCHEMA)
            .withField("reason", STRING_SCHEMA)
            .withField("source", STRING_SCHEMA)
            .withField("expires", STRING_SCHEMA)
    );
    public static final SchemaComponent<BanlistService.UserBanDto> PLAYER_BAN_SCHEMA = registerSchema(
        "user_ban",
        record(BanlistService.UserBanDto.CODEC.codec())
            .withField("player", PLAYER_SCHEMA.asRef())
            .withField("reason", STRING_SCHEMA)
            .withField("source", STRING_SCHEMA)
            .withField("expires", STRING_SCHEMA)
    );

    public static <T> Codec<Schema<T>> typedCodec() {
        return (Codec<Schema<T>>)CODEC;
    }

    public Schema<T> info() {
        return new Schema<>(
            this.reference,
            this.type,
            this.items.map(Schema::info),
            this.properties.entrySet().stream().collect(Collectors.toMap(Entry::getKey, b -> b.getValue().info())),
            this.enumValues,
            this.codec
        );
    }

    private static <T> SchemaComponent<T> registerSchema(final String name, final Schema<T> schema) {
        SchemaComponent<T> entry = new SchemaComponent<>(name, ReferenceUtil.createLocalReference(name), schema);
        SCHEMA_REGISTRY.add(entry);
        return entry;
    }

    public static List<SchemaComponent<?>> getSchemaRegistry() {
        return SCHEMA_REGISTRY;
    }

    public static <T> Schema<T> ofRef(final URI ref, final Codec<T> codec) {
        return new Schema<>(Optional.of(ref), List.of(), Optional.empty(), Map.of(), List.of(), codec);
    }

    public static <T> Schema<T> ofType(final String type, final Codec<T> codec) {
        return ofTypes(List.of(type), codec);
    }

    public static <T> Schema<T> ofTypes(final List<String> types, final Codec<T> codec) {
        return new Schema<>(Optional.empty(), types, Optional.empty(), Map.of(), List.of(), codec);
    }

    public static <E extends Enum<E> & StringRepresentable> Schema<E> ofEnum(final Supplier<E[]> values) {
        return ofEnum(values, StringRepresentable.fromEnum(values));
    }

    public static <E extends Enum<E> & StringRepresentable> Schema<E> ofEnum(final Supplier<E[]> values, final Codec<E> codec) {
        List<String> enumValues = Stream.<Enum>of((Enum[])values.get()).map(rec$ -> ((StringRepresentable)rec$).getSerializedName()).toList();
        return ofEnum(enumValues, codec);
    }

    public static <T> Schema<T> ofEnum(final List<String> enumValues, final Codec<T> codec) {
        return new Schema<>(Optional.empty(), List.of("string"), Optional.empty(), Map.of(), enumValues, codec);
    }

    public static <T> Schema<List<T>> arrayOf(final Schema<?> item, final Codec<T> codec) {
        return new Schema<>(Optional.empty(), List.of("array"), Optional.of(item), Map.of(), List.of(), codec.listOf());
    }

    public static <T> Schema<T> record(final Codec<T> codec) {
        return new Schema<>(Optional.empty(), List.of("object"), Optional.empty(), Map.of(), List.of(), codec);
    }

    private static <T> Schema<T> record(final Map<String, Schema<?>> properties, final Codec<T> codec) {
        return new Schema<>(Optional.empty(), List.of("object"), Optional.empty(), properties, List.of(), codec);
    }

    public Schema<T> withField(final String name, final Schema<?> field) {
        HashMap<String, Schema<?>> properties = new HashMap<>(this.properties);
        properties.put(name, field);
        return record(properties, this.codec);
    }

    public Schema<List<T>> asArray() {
        return arrayOf(this, this.codec);
    }
}
