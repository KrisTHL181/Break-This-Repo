package rip.ysm.api.network.forge;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import rip.ysm.api.network.PacketContext;

import java.util.function.Supplier;

final class PacketContextImpl implements PacketContext {

    private final Supplier<NetworkEvent.Context> contextSupplier;

    PacketContextImpl(Supplier<NetworkEvent.Context> contextSupplier) {
        this.contextSupplier = contextSupplier;
    }

    @Override
    public boolean isClientSide() {
        return contextSupplier.get().getDirection().getReceptionSide().isClient();
    }

    @Override
    public boolean isServerSide() {
        return contextSupplier.get().getDirection().getReceptionSide().isServer();
    }

    @Override
    public @Nullable ServerPlayer getSender() {
        return contextSupplier.get().getSender();
    }

    @Override
    public Connection getConnection() {
        return contextSupplier.get().getNetworkManager();
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        contextSupplier.get().enqueueWork(runnable);
    }
}
