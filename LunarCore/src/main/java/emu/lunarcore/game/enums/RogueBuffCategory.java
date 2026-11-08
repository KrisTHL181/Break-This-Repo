package emu.lunarcore.game.enums;

import lombok.Getter;

@Getter
public enum RogueBuffCategory {
    None (0),
    Common (1),
    Rare (2),
    Legendary (3);
    
    private final int val;

    private RogueBuffCategory(int value) {
        this.val = value;
    }
}
