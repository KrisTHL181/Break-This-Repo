package net.minecraft.server.players;

import java.util.Optional;
import java.util.UUID;

public interface UserNameToIdResolver {
   void add(NameAndId nameAndId);

   Optional<NameAndId> get(String name);

   Optional<NameAndId> get(UUID id);

   void resolveOfflineUsers(boolean value);

   void save();
}
