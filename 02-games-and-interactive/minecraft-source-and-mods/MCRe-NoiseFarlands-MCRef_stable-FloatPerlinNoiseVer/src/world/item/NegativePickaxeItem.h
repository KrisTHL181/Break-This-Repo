#ifndef NEGATIVEPICKAXEITEM_H
#define NEGATIVEPICKAXEITEM_H

#include "PickaxeItem.h"

class NegativePickaxeItem : public PickaxeItem {
    typedef PickaxeItem super;
public:
    NegativePickaxeItem(int id, const Item::Tier& tier);
    bool mineBlock(ItemInstance* item, int tile, int x, int y, int z) override;
    void hurtEnemy(ItemInstance* item, Mob* mob) override;
};

#endif
