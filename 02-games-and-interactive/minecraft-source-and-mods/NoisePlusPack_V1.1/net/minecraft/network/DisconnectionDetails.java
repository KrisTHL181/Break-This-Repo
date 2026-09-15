package net.minecraft.network;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record DisconnectionDetails(Component reason, Optional<Path> report, Optional<URI> bugReportLink) {
   public DisconnectionDetails(Component p_345232_) {
      this(p_345232_, Optional.empty(), Optional.empty());
   }
}
