#ifndef HELLSANDTILE_H__
#define HELLSANDTILE_H__

#include "Tile.h"

class HellSandTile : public Tile {
    typedef Tile super;
public:
    // ★ 三个参数：id + 纹理 + 材质
    HellSandTile(int id, int tex, const Material* material)
        : Tile(id, tex, material) {
        friction = 0.8f;
    }

    // 后续可添加 entityInside 下沉逻辑
};

#endif
