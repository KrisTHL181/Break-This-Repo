package net.minecraft.server.players;

import java.util.Optional;
import java.util.UUID;

public interface UserNameToIdResolver {
   void add(NameAndId var1);

   Optional<NameAndId> get(String var1);

   Optional<NameAndId> get(UUID var1);

   void resolveOfflineUsers(boolean var1);

   void save();
}
