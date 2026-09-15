#include "NegativePickaxeItem.h"

NegativePickaxeItem::NegativePickaxeItem(int id, const Tier& tier)
    : PickaxeItem(id, tier) {
    setMaxDamage(100);      // 最大耐久设为100
}

// 挖掘方块不消耗耐久
bool NegativePickaxeItem::mineBlock(ItemInstance* item, int tile, int x, int y, int z) {
    return true;            // 仍然返回 true，但不去调用 hurt
}

// 攻击生物也不消耗耐久
void NegativePickaxeItem::hurtEnemy(ItemInstance* item, Mob* mob) {
    // 留空，什么也不做
}
