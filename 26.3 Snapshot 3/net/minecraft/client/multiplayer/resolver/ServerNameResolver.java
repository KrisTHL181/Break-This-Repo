package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

public class ServerNameResolver {
   public static final ServerNameResolver DEFAULT = new ServerNameResolver(
      ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService()
   );
   private final ServerAddressResolver resolver;
   private final ServerRedirectHandler redirectHandler;
   private final AddressCheck addressCheck;

   @VisibleForTesting
   ServerNameResolver(final ServerAddressResolver resolver, final ServerRedirectHandler redirectHandler, final AddressCheck addressCheck) {
      this.resolver = resolver;
      this.redirectHandler = redirectHandler;
      this.addressCheck = addressCheck;
   }

   public Optional<ResolvedServerAddress> resolveAddress(final ServerAddress address) {
      Optional<ResolvedServerAddress> resolvedAddress = this.resolver.resolve(address);
      if ((!resolvedAddress.isPresent() || this.addressCheck.isAllowed(resolvedAddress.get())) && this.addressCheck.isAllowed(address)) {
         Optional<ServerAddress> redirectedAddress = this.redirectHandler.lookupRedirect(address);
         if (redirectedAddress.isPresent()) {
            resolvedAddress = this.resolver.resolve(redirectedAddress.get()).filter(this.addressCheck::isAllowed);
         }

         return resolvedAddress;
      } else {
         return Optional.empty();
      }
   }
}
