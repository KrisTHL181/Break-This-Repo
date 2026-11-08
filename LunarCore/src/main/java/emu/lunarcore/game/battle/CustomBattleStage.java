package emu.lunarcore.game.battle;

import java.util.ArrayList;
import java.util.List;

import emu.lunarcore.game.enums.StageType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class CustomBattleStage implements BattleStage {
    private int id;
    private List<IntList> monsterWaves;
    
    public CustomBattleStage(int id) {
        this.id = id;
        this.monsterWaves = new ArrayList<>();
    }

    @Override
    public StageType getStageType() {
        return StageType.Mainline;
    }
    
    public void addMonster(int monsterId, boolean startNewWave) {
        if (this.monsterWaves.size() == 0 || startNewWave) {
            IntList wave = new IntArrayList();
            wave.add(monsterId);
            
            this.monsterWaves.add(wave);
        } else {
            IntList wave = this.monsterWaves.get(this.monsterWaves.size() - 1);
            
            if (wave.size() < 5) {
                wave.add(monsterId);
            } else {
                this.addMonster(monsterId, true);
            }
        }
    }
}
