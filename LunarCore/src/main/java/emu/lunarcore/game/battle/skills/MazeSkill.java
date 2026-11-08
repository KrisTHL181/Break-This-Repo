package emu.lunarcore.game.battle.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import emu.lunarcore.data.excel.AvatarExcel;
import emu.lunarcore.game.avatar.GameAvatar;
import emu.lunarcore.game.scene.entity.GameEntity;
import emu.lunarcore.proto.MotionInfoOuterClass.MotionInfo;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MazeSkill {
    private int id;
    private int index;
    private List<MazeSkillAction> castActions;
    private List<MazeSkillAction> attackActions;
    private Set<String> adventureModifiers;
    
    @Setter private boolean triggerBattle;
    
    public MazeSkill(AvatarExcel excel, int index) {
        this.id = excel.getAvatarID();
        this.index = index;
        this.triggerBattle = true;
        this.castActions = new ArrayList<>();
        this.attackActions = new ArrayList<>();
    }
    
    public void addAdventureModifier(String modifier) {
        if (modifier == null) return;
        
        if (this.adventureModifiers == null) {
            this.adventureModifiers = new ObjectOpenHashSet<>();
        }
        
        this.adventureModifiers.add(modifier);
    }
    
    public boolean hasAdventureModifier(String modifier) {
        return this.adventureModifiers != null && this.adventureModifiers.contains(modifier);
    }
    
    /**
     * Triggered when player casts a skill
     */
    public void onCast(GameAvatar caster, MotionInfo castPosition) {
        if (this.getCastActions().size() == 0) return;
        
        for (var action : this.getCastActions()) {
            action.onCast(caster, castPosition);
        }
    }
    
    /** 
     * Triggered when player casts a skill and it hits entities
     */
    public void onCastHit(GameAvatar caster, List<? extends GameEntity> entities) {
        if (this.getAttackActions().size() == 0) return;
        
        for (var action : this.getAttackActions()) {
            action.onCastHit(caster, entities);
        }
    }
    
    /**
     * Triggered when player attacks an enemy
     */
    public void onAttack(GameAvatar caster, List<? extends GameEntity> targets) {
        if (this.getAttackActions().size() == 0) return;
        
        for (var action : this.getAttackActions()) {
            action.onAttack(caster, targets);
        }
    }
}
