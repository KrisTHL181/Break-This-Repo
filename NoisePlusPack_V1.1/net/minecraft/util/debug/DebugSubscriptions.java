package net.minecraft.util.debug;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.level.redstone.Orientation;

public class DebugSubscriptions<T> {
   public static final DebugSubscription<?> DEDICATED_SERVER_TICK_TIME = registerSimple("dedicated_server_tick_time");
   public static final DebugSubscription<DebugBeeInfo> BEES = registerWithValue("bees", DebugBeeInfo.STREAM_CODEC);
   public static final DebugSubscription<DebugBrainDump> BRAINS = registerWithValue("brains", DebugBrainDump.STREAM_CODEC);
   public static final DebugSubscription<DebugBreezeInfo> BREEZES = registerWithValue("breezes", DebugBreezeInfo.STREAM_CODEC);
   public static final DebugSubscription<DebugGoalInfo> GOAL_SELECTORS = registerWithValue("goal_selectors", DebugGoalInfo.STREAM_CODEC);
   public static final DebugSubscription<DebugPathInfo> ENTITY_PATHS = registerWithValue("entity_paths", DebugPathInfo.STREAM_CODEC);
   public static final DebugSubscription<DebugEntityBlockIntersection> ENTITY_BLOCK_INTERSECTIONS = registerTemporaryValue(
      "entity_block_intersections", DebugEntityBlockIntersection.STREAM_CODEC, 100
   );
   public static final DebugSubscription<DebugHiveInfo> BEE_HIVES = registerWithValue("bee_hives", DebugHiveInfo.STREAM_CODEC);
   public static final DebugSubscription<DebugPoiInfo> POIS = registerWithValue("pois", DebugPoiInfo.STREAM_CODEC);
   public static final DebugSubscription<Orientation> REDSTONE_WIRE_ORIENTATIONS = registerTemporaryValue(
      "redstone_wire_orientations", Orientation.STREAM_CODEC, 200
   );
   public static final DebugSubscription<Unit> VILLAGE_SECTIONS = registerWithValue("village_sections", Unit.STREAM_CODEC);
   public static final DebugSubscription<List<BlockPos>> RAIDS = registerWithValue("raids", BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()));
   public static final DebugSubscription<List<DebugStructureInfo>> STRUCTURES = registerWithValue(
      "structures", DebugStructureInfo.STREAM_CODEC.apply(ByteBufCodecs.list())
   );
   public static final DebugSubscription<DebugGameEventListenerInfo> GAME_EVENT_LISTENERS = registerWithValue(
      "game_event_listeners", DebugGameEventListenerInfo.STREAM_CODEC
   );
   public static final DebugSubscription<BlockPos> NEIGHBOR_UPDATES = registerTemporaryValue("neighbor_updates", BlockPos.STREAM_CODEC, 200);
   public static final DebugSubscription<DebugGameEventInfo> GAME_EVENTS = registerTemporaryValue("game_events", DebugGameEventInfo.STREAM_CODEC, 60);

   public static DebugSubscription<?> bootstrap(Registry<DebugSubscription<?>> p_428718_) {
      return DEDICATED_SERVER_TICK_TIME;
   }

   private static DebugSubscription<?> registerSimple(String p_422904_) {
      return Registry.register(BuiltInRegistries.DEBUG_SUBSCRIPTION, Identifier.withDefaultNamespace(p_422904_), new DebugSubscription(null));
   }

   private static <T> DebugSubscription<T> registerWithValue(String p_429418_, StreamCodec<? super RegistryFriendlyByteBuf, T> p_428513_) {
      return Registry.register(BuiltInRegistries.DEBUG_SUBSCRIPTION, Identifier.withDefaultNamespace(p_429418_), new DebugSubscription<>(p_428513_));
   }

   private static <T> DebugSubscription<T> registerTemporaryValue(String p_430317_, StreamCodec<? super RegistryFriendlyByteBuf, T> p_424522_, int p_422345_) {
      return Registry.register(BuiltInRegistries.DEBUG_SUBSCRIPTION, Identifier.withDefaultNamespace(p_430317_), new DebugSubscription<>(p_424522_, p_422345_));
   }
}
