package net.minecraft.server.jsonrpc.api;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
   public static final Codec<? extends Schema<?>> CODEC = Codec.recursive(
         "Schema",
         p_422846_ -> RecordCodecBuilder.create(
            p_449128_ -> p_449128_.group(
                  ReferenceUtil.REFERENCE_CODEC.optionalFieldOf("$ref").forGetter(Schema::reference),
                  ExtraCodecs.compactListCodec(Codec.STRING).optionalFieldOf("type", List.of()).forGetter(Schema::type),
                  p_422846_.optionalFieldOf("items").forGetter(Schema::items),
                  Codec.unboundedMap(Codec.STRING, p_422846_).optionalFieldOf("properties", Map.of()).forGetter(Schema::properties),
                  Codec.STRING.listOf().optionalFieldOf("enum", List.of()).forGetter(Schema::enumValues)
               )
               .apply(p_449128_, (p_449121_, p_449122_, p_449123_, p_449124_, p_449125_) -> null)
         )
      )
      .validate(p_449126_ -> p_449126_ == null ? DataResult.error(() -> "Should not deserialize schema") : DataResult.success(p_449126_));
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
         this.properties.entrySet().stream().collect(Collectors.toMap(Entry::getKey, p_449120_ -> p_449120_.getValue().info())),
         this.enumValues,
         this.codec
      );
   }

   private static <T> SchemaComponent<T> registerSchema(String p_428921_, Schema<T> p_428909_) {
      SchemaComponent<T> schemacomponent = new SchemaComponent<>(p_428921_, ReferenceUtil.createLocalReference(p_428921_), p_428909_);
      SCHEMA_REGISTRY.add(schemacomponent);
      return schemacomponent;
   }

   public static List<SchemaComponent<?>> getSchemaRegistry() {
      return SCHEMA_REGISTRY;
   }

   public static <T> Schema<T> ofRef(URI p_424017_, Codec<T> p_452749_) {
      return new Schema<>(Optional.of(p_424017_), List.of(), Optional.empty(), Map.of(), List.of(), p_452749_);
   }

   public static <T> Schema<T> ofType(String p_425091_, Codec<T> p_459409_) {
      return ofTypes(List.of(p_425091_), p_459409_);
   }

   public static <T> Schema<T> ofTypes(List<String> p_457992_, Codec<T> p_460794_) {
      return new Schema<>(Optional.empty(), p_457992_, Optional.empty(), Map.of(), List.of(), p_460794_);
   }

   public static <E extends Enum<E> & StringRepresentable> Schema<E> ofEnum(Supplier<E[]> p_422914_) {
      return ofEnum(p_422914_, StringRepresentable.fromEnum(p_422914_));
   }

   public static <E extends Enum<E> & StringRepresentable> Schema<E> ofEnum(Supplier<E[]> p_453049_, Codec<E> p_454697_) {
      List<String> list = Stream.<Enum>of((Enum[])p_453049_.get()).map(p_422590_ -> ((StringRepresentable)p_422590_).getSerializedName()).toList();
      return ofEnum(list, p_454697_);
   }

   public static <T> Schema<T> ofEnum(List<String> p_427108_, Codec<T> p_452630_) {
      return new Schema<>(Optional.empty(), List.of("string"), Optional.empty(), Map.of(), p_427108_, p_452630_);
   }

   public static <T> Schema<List<T>> arrayOf(Schema<?> p_428324_, Codec<T> p_450894_) {
      return new Schema<>(Optional.empty(), List.of("array"), Optional.of(p_428324_), Map.of(), List.of(), p_450894_.listOf());
   }

   public static <T> Schema<T> record(Codec<T> p_450622_) {
      return new Schema<>(Optional.empty(), List.of("object"), Optional.empty(), Map.of(), List.of(), p_450622_);
   }

   private static <T> Schema<T> record(Map<String, Schema<?>> p_456210_, Codec<T> p_453973_) {
      return new Schema<>(Optional.empty(), List.of("object"), Optional.empty(), p_456210_, List.of(), p_453973_);
   }

   public Schema<T> withField(String p_424730_, Schema<?> p_425422_) {
      HashMap<String, Schema<?>> hashmap = new HashMap<>(this.properties);
      hashmap.put(p_424730_, p_425422_);
      return record(hashmap, this.codec);
   }

   public Schema<List<T>> asArray() {
      return arrayOf(this, this.codec);
   }
}
