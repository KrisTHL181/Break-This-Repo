package net.minecraft.client.multiplayer.resolver;

import java.net.InetSocketAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvedServerAddress {
    String getHostName();

    String getHostIp();

    int getPort();

    InetSocketAddress asInetSocketAddress();

    static ResolvedServerAddress from(final InetSocketAddress address) {
        return new ResolvedServerAddress() {
            @Override
            public String getHostName() {
                return address.getAddress().getHostName();
            }

            @Override
            public String getHostIp() {
                return address.getAddress().getHostAddress();
            }

            @Override
            public int getPort() {
                return address.getPort();
            }

            @Override
            public InetSocketAddress asInetSocketAddress() {
                return address;
            }
        };
    }
}