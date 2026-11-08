package emu.lunarcore.game.battle;

import java.util.List;

import emu.lunarcore.game.enums.StageType;
import it.unimi.dsi.fastutil.ints.IntList;

public interface BattleStage {
    
    public int getId();
    
    public StageType getStageType();
    
    public List<IntList> getMonsterWaves();
    
}
