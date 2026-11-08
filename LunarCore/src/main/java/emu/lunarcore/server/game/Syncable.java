package emu.lunarcore.server.game;

import emu.lunarcore.proto.PlayerSyncScNotifyOuterClass.PlayerSyncScNotify;

public interface Syncable {
    
    public void onSync(PlayerSyncScNotify proto);
    
}
