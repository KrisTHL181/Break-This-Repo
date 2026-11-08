package emu.lunarcore.server.http.handlers;

import org.jetbrains.annotations.NotNull;

import emu.lunarcore.GameConstants;
import emu.lunarcore.LunarCore;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class StatusServerHandler implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        int playerCount = LunarCore.getGameServer().getPlayerCount();
        int maxPlayers = LunarCore.getConfig().getServerOptions().maxPlayers;
        String version = GameConstants.VERSION;
        String responseJson = String.format(
                "{\"retcode\":0,\"status\":{\"playerCount\":%d,\"maxPlayers\":%d,\"version\":\"%s\"}}",
                playerCount, maxPlayers, version
        );
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result(responseJson);
    }
}
