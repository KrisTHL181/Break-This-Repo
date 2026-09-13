#ifndef NET_MINECRAFT_CLIENT_PLAYER__LocalPlayer_H__
#define NET_MINECRAFT_CLIENT_PLAYER__LocalPlayer_H__

//package net.minecraft.client.player;

#include "input/IMoveInput.h"
#include "../../util/SmoothFloat.h"
#include "../../world/entity/player/Player.h"
#include "../../util/WorldOrigin.h"

class Minecraft;
class Stat;
class CompoundTag;
class CThread;   // <-- 添加这一行

class LocalPlayer: public Player
{
	typedef Player super;
public:
	LocalPlayer(Minecraft* minecraft, Level* level, const std::string& username, int dimension, bool isCreative);
	~LocalPlayer();

	void _init();
	virtual void reset();

	void tick();
    void move(double xa, double ya, double za);

WorldOrigin m_origin;
const WorldOrigin& getWorldOrigin() const { return m_origin; }

// ====== BigWorldCoordinate 绝对位置 — 真无限 ======
BigWorldCoordinate m_bigAbsX{0};
BigWorldCoordinate m_bigAbsY{0};
BigWorldCoordinate m_bigAbsZ{0};

// 覆写 Big 访问器 (Entity 基类虚函数)
virtual BigWorldCoordinate getBigAbsX() const override { return m_bigAbsX; }
virtual BigWorldCoordinate getBigAbsY() const override { return m_bigAbsY; }
virtual BigWorldCoordinate getBigAbsZ() const override { return m_bigAbsZ; }

// 覆写 storeAbsolutePosition — 双写 Big + double 缓存
virtual void storeAbsolutePosition(const BigWorldCoordinate& bx,
                                    const BigWorldCoordinate& by,
                                    const BigWorldCoordinate& bz) override {
    m_bigAbsX = bx;
    m_bigAbsY = by;
    m_bigAbsZ = bz;
    this->x = bx.convert_to<double>();
    this->y = by.convert_to<double>();
    this->z = bz.convert_to<double>();
}

// 覆写 setPos — 保证 Big 同步
virtual void setPos(double x, double y, double z) override {
    double ox = m_origin.originX().convert_to<double>();
    double oy = m_origin.originY().convert_to<double>();
    double oz = m_origin.originZ().convert_to<double>();
    
    m_bigAbsX = m_origin.originX() + BigWorldCoordinate(x - ox);
    m_bigAbsY = m_origin.originY() + BigWorldCoordinate(y - oy);
    m_bigAbsZ = m_origin.originZ() + BigWorldCoordinate(z - oz);
    
    // 🔧 teleport 后重新对齐 origin
    m_origin.tickBig(m_bigAbsX, m_bigAbsY, m_bigAbsZ);
    
    this->x = x; this->y = y; this->z = z;
    float w = bbWidth / 2; float h = bbHeight;
    bb.set(x - w, y - heightOffset + ySlideOffset, z - w,
           x + w, y - heightOffset + ySlideOffset + h, z + w);
}

// 覆写 moveTo — 传送时保证 Big 同步
virtual void moveTo(double x, double y, double z, float yRot, float xRot) override {
    this->xOld = this->xo = this->x = x;
    this->yOld = this->yo = this->y = y + heightOffset;
    this->zOld = this->zo = this->z = z;
    this->yRot = this->yRotO = yRot;
    this->xRot = this->xRotO = xRot;
    
    double ox = m_origin.originX().convert_to<double>();
    double oy = m_origin.originY().convert_to<double>();
    double oz = m_origin.originZ().convert_to<double>();
    m_bigAbsX = m_origin.originX() + BigWorldCoordinate(x - ox);
    m_bigAbsY = m_origin.originY() + BigWorldCoordinate(y + heightOffset - oy);
    m_bigAbsZ = m_origin.originZ() + BigWorldCoordinate(z - oz);
    
    m_origin.tickBig(m_bigAbsX, m_bigAbsY, m_bigAbsZ);  // 🔧 对齐 origin
    
    this->setPos(x, y, z);
}

// 精确传送 (Big 参数)
void setPosBig(const BigWorldCoordinate& bx, const BigWorldCoordinate& by, const BigWorldCoordinate& bz) {
    m_bigAbsX = bx; m_bigAbsY = by; m_bigAbsZ = bz;
    double dx = bx.convert_to<double>();
    double dy = by.convert_to<double>();
    double dz = bz.convert_to<double>();
    this->x = dx; this->y = dy; this->z = dz;
    float w = bbWidth / 2; float h = bbHeight;
    bb.set(dx - w, dy - heightOffset + ySlideOffset, dz - w,
           dx + w, dy - heightOffset + ySlideOffset + h, dz + w);
}

// 精度保护：给 Entity::move() 提供 local 帧原点
    virtual double getLocalFrameOriginX() const override { return m_origin.originX().convert_to<double>(); }
    virtual double getLocalFrameOriginY() const override { return m_origin.originY().convert_to<double>(); }
    virtual double getLocalFrameOriginZ() const override { return m_origin.originZ().convert_to<double>(); }

virtual BigWorldCoordinate getLocalFrameOriginBigX() const override { return m_origin.originX(); }
virtual BigWorldCoordinate getLocalFrameOriginBigY() const override { return m_origin.originY(); }
virtual BigWorldCoordinate getLocalFrameOriginBigZ() const override { return m_origin.originZ(); }

virtual bool isLocalPlayer() const override { return true; }

    void aiStep();
    void updateAi();

	void setKey(int eventKey, bool eventKeyState);
    void releaseAllKeys();

    void addAdditonalSaveData(CompoundTag* entityTag);
    void readAdditionalSaveData(CompoundTag* entityTag);

    void closeContainer();

	void drop(ItemInstance* item, bool randomly);
    void take(Entity* e, int orgCount);

	void startCrafting(int x, int y, int z, int tableSize);
	void startStonecutting(int x, int y, int z);

	void openContainer(ChestTileEntity* container);
	void openFurnace(FurnaceTileEntity* e);

    bool isSneaking();

    void actuallyHurt(int dmg);
    void hurtTo(int newHealth);
	void die(Entity* source);

    void respawn();

    void animateRespawn() {}
	float getFieldOfViewModifier();
	void chat(const std::string& message) {}
    void displayClientMessage(const std::string& messageId);

    void awardStat(Stat* stat, int count) {
        //minecraft->stats.award(stat, count);
        //minecraft->achievementPopup.popup("Achievement get!", stat.name);
    }
	void causeFallDamage( float distance );

	virtual int startSleepInBed(int x, int y, int z);
	virtual void stopSleepInBed(bool forcefulWakeUp, bool updateLevelList, bool saveRespawnPoint);

	void swing();
	virtual void openTextEdit( TileEntity* tileEntity );
	virtual float getWalkingSpeedModifier();
private:
    CThread* m_skinThread;
    CThread* m_capeThread;
	void calculateFlight(float xa, float ya, float za);
	bool isSolidTile(int x, int y, int z);
	void updateArmorTypeHash();
public:
	IMoveInput* input;
	bool autoJumpEnabled;
protected:
	Minecraft* minecraft;
	int jumpTriggerTime;
	int ascendTriggerTime;
	int descendTriggerTime;
	bool ascending, descending;
private:
    // local player fly
    // -----------------------
    float flyX, flyY, flyZ;

    double sentX, sentY, sentZ;
    float sentRotX, sentRotY;
    // smooth camera settings
    SmoothFloat smoothFlyX;
    SmoothFloat smoothFlyY;
    SmoothFloat smoothFlyZ;

	int autoJumpTime;

	int sentInventoryItemId;
	int sentInventoryItemData;

	int armorTypeHash;

	// sprinting
	bool sprinting;
	int  sprintDoubleTapTimer;
	bool prevForwardHeld;
public:
	void setSprinting(bool sprint) { sprinting = sprint; }
};

#endif /*NET_MINECRAFT_CLIENT_PLAYER__LocalPlayer_H__*/
