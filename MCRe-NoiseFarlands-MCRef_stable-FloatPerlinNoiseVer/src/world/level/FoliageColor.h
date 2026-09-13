#ifndef NET_MINECRAFT_WORLD_LEVEL__FoliageColor_H__
#define NET_MINECRAFT_WORLD_LEVEL__FoliageColor_H__

class FoliageColor
{
public:
    static int getEndGrassColor()    { return 0x8EB971; }
    static int getEndFoliageColor()  { return 0x71A74D; }
    static int getEndDeadColor()     { return 0xA17448; }
    // Beta 1.7.3 草木色调
    static int getEvergreenColor()  { return 0x6A9C4A; }  // 原 0x619961
    static int getBirchColor()      { return 0x9CCE6C; }  // 原 0x80a755
    static int getDefaultColor()    { return 0x7EC850; }  // 原 0x48b518

private:
    // 防止实例化
    FoliageColor() {}
};

#endif
