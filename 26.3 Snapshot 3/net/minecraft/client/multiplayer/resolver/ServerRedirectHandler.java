package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;

@FunctionalInterface
public interface ServerRedirectHandler {
   Logger LOGGER = LogUtils.getLogger();
   ServerRedirectHandler EMPTY = originalAddress -> Optional.empty();

   Optional<ServerAddress> lookupRedirect(ServerAddress originalAddress);

   static ServerRedirectHandler createDnsSrvRedirectHandler() {
      DirContext context;
      try {
         String dnsContextClass = "com.sun.jndi.dns.DnsContextFactory";
         Class.forName("com.sun.jndi.dns.DnsContextFactory");
         Hashtable<String, String> env = new Hashtable<>();
         env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
         env.put("java.naming.provider.url", "dns:");
         env.put("com.sun.jndi.dns.timeout.retries", "1");
         context = new InitialDirContext(env);
      } catch (Throwable e) {
         LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", e);
         return EMPTY;
      }

      return originalAddress -> {
         if (originalAddress.getPort() == 25565) {
            try {
               Attributes attributes = context.getAttributes("_minecraft._tcp." + originalAddress.getHost(), new String[]{"SRV"});
               Attribute srvAttribute = attributes.get("srv");
               if (srvAttribute != null) {
                  String[] arguments = srvAttribute.get().toString().split(" ", 4);
                  return Optional.of(new ServerAddress(arguments[3], ServerAddress.parsePort(arguments[2])));
               }
            } catch (Throwable var5) {
            }
         }

         return Optional.empty();
      };
   }
}
