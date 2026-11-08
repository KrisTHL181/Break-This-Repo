package net.minecraft.client.multiplayer;

import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;

public record TransferState(Map<Identifier, byte[]> cookies, Map<UUID, PlayerInfo> seenPlayers, boolean seenInsecureChatWarning) {
}
