package rip.ysm.api.network.fabric;

import com.elfmcys.yesstevemodel.mixin.ConnectionAccessor;
import com.elfmcys.yesstevemodel.mixin.ServerCommonPacketListenerImplAccessor;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.PacketDirection;
import rip.ysm.api.network.fabric.client.YSMChannelClientImpl;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class YSMChannelImpl {

    private static final int FRAGMENT_DISCRIMINATOR = 255;
    private static final int FRAGMENT_DATA_SIZE = 30_000;
    private static final int MAX_FRAGMENT_COUNT = 128;
    private static final int MAX_REASSEMBLED_SIZE = 2 * 1024 * 1024;
    private static final long FRAGMENT_TIMEOUT_NANOS = 30_000_000_000L;

    private static final Map<Integer, Codec<?>> CODECS_BY_ID = new HashMap<>();
    private static final Map<Class<?>, Integer> ID_BY_CLASS = new HashMap<>();
    private static final Map<Connection, Map<Integer, FragmentAccumulator>> INCOMING_FRAGMENTS = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger();

    private static ResourceLocation channelId;
    private static volatile MinecraftServer currentServer;

    private YSMChannelImpl() {
    }

    public static void init(ResourceLocation id, String version) {
        channelId = id;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> currentServer = null);

        ServerPlayNetworking.registerGlobalReceiver(channelId, (server, player, handler, buf, responseSender) -> dispatch(buf, new ServerPacketContext(server, player, ((ServerCommonPacketListenerImplAccessor) handler).ysm$getConnection())));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            YSMChannelClientImpl.init(channelId);
        }
    }

    public static <T> void register(int discriminator, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        if ((discriminator & ~0xff) != 0) {
            throw new IllegalArgumentException("Discriminator must fit in an unsigned byte (0-255): " + discriminator);
        }
        Codec<T> codec = new Codec<>(type, encoder, decoder, handler);
        CODECS_BY_ID.put(discriminator & 0xff, codec);
        ID_BY_CLASS.put(type, discriminator & 0xff);
    }

    public static void dispatch(FriendlyByteBuf buf, PacketContext ctx) {
        int discriminator = buf.readUnsignedByte();
        if (discriminator == FRAGMENT_DISCRIMINATOR) {
            handleFragment(FragmentPacket.decode(buf), ctx);
            return;
        }
        Codec<?> codec = CODECS_BY_ID.get(discriminator);
        if (codec != null) {
            codec.dispatch(buf, ctx);
        }
    }

    public static void sendToServer(Object packet) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        FriendlyByteBuf encoded = encode(packet);
        if (packet instanceof C2SModelSyncPayload
                && NetworkHandler.serverSupportsModelSyncFragments()
                && encoded.readableBytes() > FRAGMENT_DATA_SIZE) {
            byte[] data = copyAndRelease(encoded);
            sendFragments(data);
            return;
        }
        YSMChannelClientImpl.sendToServer(channelId, encoded);
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        ServerPlayNetworking.send(player, channelId, encode(packet));
    }

    public static void sendToAll(Object packet) {
        MinecraftServer server = currentServer;
        if (server == null) {
            return;
        }
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, channelId, encode(packet));
        }
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, channelId, encode(packet));
        }
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        for (ServerPlayer p : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(p, channelId, encode(packet));
        }
        if (player instanceof ServerPlayer self) {
            ServerPlayNetworking.send(self, channelId, encode(packet));
        }
    }

    public static Packet<?> toClientboundPacket(Object packet) {
        return ServerPlayNetworking.createS2CPacket(channelId, encode(packet));
    }

    public static List<Packet<?>> toClientboundPackets(Object packet) {
        byte[] encoded = copyAndRelease(encode(packet));
        if (encoded.length <= FRAGMENT_DATA_SIZE) {
            return List.of(ServerPlayNetworking.createS2CPacket(channelId, new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded))));
        }
        if (encoded.length > MAX_REASSEMBLED_SIZE) {
            throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
        }
        List<Packet<?>> packets = new ArrayList<>();
        int transferId = NEXT_TRANSFER_ID.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            FriendlyByteBuf fragment = new FriendlyByteBuf(Unpooled.buffer());
            fragment.writeByte(FRAGMENT_DISCRIMINATOR);
            FragmentPacket.encode(new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)), fragment);
            packets.add(ServerPlayNetworking.createS2CPacket(channelId, fragment));
        }
        return packets;
    }

    public static Packet<?> toServerboundPacket(Object packet) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new IllegalStateException("toServerboundPacket can only be invoked from the client environment");
        }
        return YSMChannelClientImpl.toServerboundPacket(channelId, encode(packet));
    }

    private static FriendlyByteBuf encode(Object packet) {
        Integer id = ID_BY_CLASS.get(packet.getClass());
        if (id == null) {
            throw new IllegalStateException("Packet type not registered: " + packet.getClass());
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(id & 0xff);
        CODECS_BY_ID.get(id).encode(packet, buf);
        return buf;
    }

    private static byte[] copyAndRelease(FriendlyByteBuf buf) {
        try {
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void sendFragments(byte[] encoded) {
        if (encoded.length > MAX_REASSEMBLED_SIZE) {
            throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
        }
        int transferId = NEXT_TRANSFER_ID.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            FriendlyByteBuf fragment = new FriendlyByteBuf(Unpooled.buffer());
            fragment.writeByte(FRAGMENT_DISCRIMINATOR);
            FragmentPacket.encode(new FragmentPacket(
                    transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)
            ), fragment);
            YSMChannelClientImpl.sendToServer(channelId, fragment);
        }
    }

    private static void handleFragment(FragmentPacket packet, PacketContext context) {
        long now = System.nanoTime();
        Connection connection = context.getConnection();
        Map<Integer, FragmentAccumulator> newTransfers = new ConcurrentHashMap<>();
        Map<Integer, FragmentAccumulator> transfers = INCOMING_FRAGMENTS.putIfAbsent(connection, newTransfers);
        if (transfers == null) {
            transfers = newTransfers;
            Map<Integer, FragmentAccumulator> registeredTransfers = transfers;
            ((ConnectionAccessor) connection).ysm$getChannel().closeFuture()
                    .addListener(ignored -> INCOMING_FRAGMENTS.remove(connection, registeredTransfers));
        }
        transfers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateNanos > FRAGMENT_TIMEOUT_NANOS);

        FragmentAccumulator accumulator = transfers.computeIfAbsent(
                packet.transferId(), ignored -> new FragmentAccumulator(packet.fragmentCount())
        );
        byte[] complete = accumulator.add(packet, now);
        if (complete == null) {
            return;
        }
        transfers.remove(packet.transferId(), accumulator);
        if (transfers.isEmpty()) {
            INCOMING_FRAGMENTS.remove(connection, transfers);
        }

        FriendlyByteBuf original = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));
        try {
            dispatch(original, context);
            if (original.isReadable()) {
                throw new IllegalArgumentException("Fragmented YSM packet left " + original.readableBytes() + " unread bytes");
            }
        } finally {
            original.release();
        }
    }

    private record FragmentPacket(int transferId, int fragmentIndex, int fragmentCount, byte[] data) {
        private static void encode(FragmentPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.transferId);
            buf.writeVarInt(packet.fragmentIndex);
            buf.writeVarInt(packet.fragmentCount);
            buf.writeByteArray(packet.data);
        }

        private static FragmentPacket decode(FriendlyByteBuf buf) {
            return new FragmentPacket(
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(FRAGMENT_DATA_SIZE)
            );
        }
    }

    private static final class FragmentAccumulator {
        private final byte[][] fragments;
        private int received;
        private int totalSize;
        private volatile long lastUpdateNanos = System.nanoTime();

        private FragmentAccumulator(int fragmentCount) {
            if (fragmentCount <= 0 || fragmentCount > MAX_FRAGMENT_COUNT) {
                throw new IllegalArgumentException("Invalid YSM fragment count: " + fragmentCount);
            }
            this.fragments = new byte[fragmentCount][];
        }

        private synchronized byte[] add(FragmentPacket packet, long now) {
            if (packet.fragmentCount() != fragments.length
                    || packet.fragmentIndex() < 0
                    || packet.fragmentIndex() >= fragments.length) {
                throw new IllegalArgumentException("Inconsistent YSM fragment metadata");
            }
            lastUpdateNanos = now;
            if (fragments[packet.fragmentIndex()] == null) {
                fragments[packet.fragmentIndex()] = packet.data();
                received++;
                totalSize += packet.data().length;
                if (totalSize > MAX_REASSEMBLED_SIZE) {
                    throw new IllegalArgumentException("Fragmented YSM packet exceeds maximum size");
                }
            }
            if (received != fragments.length) {
                return null;
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream(totalSize);
            for (byte[] fragment : fragments) {
                output.write(fragment, 0, fragment.length);
            }
            return output.toByteArray();
        }
    }
}
