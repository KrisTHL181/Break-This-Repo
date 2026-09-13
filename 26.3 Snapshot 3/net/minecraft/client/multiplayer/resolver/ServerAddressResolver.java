package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;

@FunctionalInterface
public interface ServerAddressResolver {
   Logger LOGGER = LogUtils.getLogger();
   ServerAddressResolver SYSTEM = address -> {
      try {
         InetAddress resolvedAddress = InetAddress.getByName(address.getHost());
         return Optional.of(ResolvedServerAddress.from(new InetSocketAddress(resolvedAddress, address.getPort())));
      } catch (UnknownHostException e) {
         LOGGER.debug("Couldn't resolve server {} address", address.getHost(), e);
         return Optional.empty();
      }
   };

   Optional<ResolvedServerAddress> resolve(ServerAddress address);
}
