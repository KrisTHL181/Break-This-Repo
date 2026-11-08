package rip.ysm.api.network.forge;

import com.elfmcys.yesstevemodel.mixin.ConnectionAccessor;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SModelSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import rip.ysm.api.network.PacketContext;
import rip.ysm.api.network.PacketDirection;

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

    private static final Map<Integer, LocalCodec<?>> codecs = new HashMap<>();
    private static final Map<Connection, Map<Integer, FragmentAccumulator>> incomingFragments = new ConcurrentHashMap<>();
    private static final AtomicInteger nextTransferId = new AtomicInteger();

    private static SimpleChannel channel;

    private YSMChannelImpl() {
    }

    public static void init(ResourceLocation channelId, String version) {
        channel = NetworkRegistry.newSimpleChannel(channelId, () -> version, str -> true, str -> true);
        channel.registerMessage(FRAGMENT_DISCRIMINATOR, FragmentPacket.class,
                FragmentPacket::encode, FragmentPacket::decode,
                (packet, ctxSupplier) -> {
                    handleFragment(packet, new PacketContextImpl(ctxSupplier));
                    ctxSupplier.get().setPacketHandled(true);
                });
    }

    public static <T> void register(int discriminator, Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        codecs.put(discriminator & 0xff, new LocalCodec<>(type, decoder, handler, direction));
        channel.registerMessage(discriminator, type, encoder, decoder,
                (msg, ctxSupplier) -> {
                    handler.accept(msg, new PacketContextImpl(ctxSupplier));
                    ctxSupplier.get().setPacketHandled(true);
                },
                Optional.of(toForge(direction))
        );
    }

    public static void sendToServer(Object packet) {
        if (packet instanceof C2SModelSyncPayload && NetworkHandler.serverSupportsModelSyncFragments()) {
            byte[] encoded = encode(packet);
            if (encoded.length > FRAGMENT_DATA_SIZE) {
                sendFragments(encoded, channel::sendToServer);
                return;
            }
        }
        channel.sendToServer(packet);
    }

    public static void sendToClientPlayer(Object packet, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(Object packet) {
        channel.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToTrackingEntity(Object packet, Entity entity) {
        channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendToTrackingEntityAndSelf(Object packet, Player player) {
        channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static Packet<?> toClientboundPacket(Object packet) {
        return channel.toVanillaPacket(packet, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static List<Packet<?>> toClientboundPackets(Object packet) {
        byte[] encoded = encode(packet);
        if (encoded.length <= FRAGMENT_DATA_SIZE) {
            return List.of(channel.toVanillaPacket(packet, NetworkDirection.PLAY_TO_CLIENT));
        }
        List<Packet<?>> packets = new ArrayList<>();
        int transferId = nextTransferId.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            FragmentPacket fragment = new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to));
            packets.add(channel.toVanillaPacket(fragment, NetworkDirection.PLAY_TO_CLIENT));
        }
        return packets;
    }

    public static Packet<?> toServerboundPacket(Object packet) {
        return channel.toVanillaPacket(packet, NetworkDirection.PLAY_TO_SERVER);
    }

    private static NetworkDirection toForge(PacketDirection direction) {
        return switch (direction) {
            case PLAY_TO_CLIENT -> NetworkDirection.PLAY_TO_CLIENT;
            case PLAY_TO_SERVER -> NetworkDirection.PLAY_TO_SERVER;
        };
    }

    private static byte[] encode(Object packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            channel.encodeMessage(packet, buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    private static void sendFragments(byte[] encoded, java.util.function.Consumer<Object> sender) {
        int transferId = nextTransferId.incrementAndGet();
        int fragmentCount = (encoded.length + FRAGMENT_DATA_SIZE - 1) / FRAGMENT_DATA_SIZE;
        for (int index = 0; index < fragmentCount; index++) {
            int from = index * FRAGMENT_DATA_SIZE;
            int to = Math.min(from + FRAGMENT_DATA_SIZE, encoded.length);
            sender.accept(new FragmentPacket(transferId, index, fragmentCount, Arrays.copyOfRange(encoded, from, to)));
        }
    }

    private static void handleFragment(FragmentPacket packet, PacketContext context) {
        PacketDirection expected = context.isServerSide() ? PacketDirection.PLAY_TO_SERVER : PacketDirection.PLAY_TO_CLIENT;

        long now = System.nanoTime();
        Connection connection = context.getConnection();
        Map<Integer, FragmentAccumulator> newTransfers = new ConcurrentHashMap<>();
        Map<Integer, FragmentAccumulator> transfers = incomingFragments.putIfAbsent(connection, newTransfers);
        if (transfers == null) {
            transfers = newTransfers;
            Map<Integer, FragmentAccumulator> registeredTransfers = transfers;
            ((ConnectionAccessor) connection).ysm$getChannel().closeFuture()
                    .addListener(ignored -> incomingFragments.remove(connection, registeredTransfers));
        }
        transfers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdateNanos > FRAGMENT_TIMEOUT_NANOS);

        FragmentAccumulator accumulator = transfers.computeIfAbsent(packet.transferId(), ignored -> new FragmentAccumulator(packet.fragmentCount()));
        byte[] complete = accumulator.add(packet, now);
        if (complete == null) {
            return;
        }
        transfers.remove(packet.transferId(), accumulator);
        if (transfers.isEmpty()) {
            incomingFragments.remove(context.getConnection(), transfers);
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(complete));
        try {
            int discriminator = buf.readUnsignedByte();
            LocalCodec<?> codec = codecs.get(discriminator);
            if (codec == null || codec.direction != expected) {
                throw new IllegalArgumentException("Invalid fragmented YSM packet discriminator: " + discriminator);
            }
            codec.dispatch(buf, context);
            if (buf.isReadable()) {
                throw new IllegalArgumentException("Fragmented YSM packet left " + buf.readableBytes() + " unread bytes");
            }
        } finally {
            buf.release();
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
            return new FragmentPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(FRAGMENT_DATA_SIZE));
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
            if (packet.fragmentCount() != fragments.length || packet.fragmentIndex() < 0 || packet.fragmentIndex() >= fragments.length) {
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

    private record LocalCodec<T>(Class<T> type, Function<FriendlyByteBuf, T> decoder,
                                 BiConsumer<T, PacketContext> handler, PacketDirection direction) {
        private void dispatch(FriendlyByteBuf buf, PacketContext context) {
            handler.accept(decoder.apply(buf), context);
        }
    }
}
