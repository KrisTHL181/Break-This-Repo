#include "DoorTile.h"
#include "LevelEvent.h"
#include "../Level.h"
#include "../../item/Item.h"
#include "../../entity/player/Player.h"

DoorTile::DoorTile(int id, const Material* material)
:	super(id, material)
{
	tex = 1 + 6 * 16;
	if (material == Material::metal) tex++;

	float r = 0.5f;
	float h = 1.0f;
	super::setShape(0.5f - r, 0, 0.5f - r, 0.5f + r, h, 0.5f + r);
}

int DoorTile::getTexture(LevelSource* level, int64_t x, int64_t y, int64_t z, int face) {
	if (face == 0 || face == 1) return tex;

	int compositeData = getCompositeData(level, x, y, z);
	int texture = tex;
	if ((compositeData & C_IS_UPPER_MASK) != 0) texture -= 16;

	int dir = compositeData & C_DIR_MASK;
	bool isOpen = (compositeData & C_OPEN_MASK) != 0;
	if (isOpen) {
		if (dir == 0 && face == 2) texture = -texture;
		else if (dir == 1 && face == 5) texture = -texture;
		else if (dir == 2 && face == 3) texture = -texture;
		else if (dir == 3 && face == 4) texture = -texture;
	} else {
		if (dir == 0 && face == 5) texture = -texture;
		else if (dir == 1 && face == 3) texture = -texture;
		else if (dir == 2 && face == 4) texture = -texture;
		else if (dir == 3 && face == 2) texture = -texture;
		if ((compositeData & C_RIGHT_HINGE_MASK) != 0) texture = -texture;
	}

	return texture;
}

bool DoorTile::blocksLight() {
	return false;
}

bool DoorTile::isSolidRender() {
	return false;
}

bool DoorTile::isCubeShaped() {
	return false;
}

int DoorTile::getRenderShape() {
	return Tile::SHAPE_DOOR;
}

int DoorTile::getRenderLayer() {
	return Tile::RENDERLAYER_ALPHATEST;
}

AABB DoorTile::getTileAABB(Level* level, int64_t x, int64_t y, int64_t z) {
	updateShape(level, x, y, z);
	return super::getTileAABB(level, x, y, z);
}

AABB* DoorTile::getAABB(Level* level, int64_t x, int64_t y, int64_t z) {
	updateShape(level, x, y, z);
	return super::getAABB(level, x, y, z);
}

void DoorTile::updateShape(LevelSource* level, int64_t x, int64_t y, int64_t z) {
	setShape(getCompositeData(level, x, y, z));
}

void DoorTile::setShape(int compositeData) {
	float r = 3 / 16.0f;
	super::setShape(0, 0, 0, 1, 2, 1);
	int dir = compositeData & C_DIR_MASK;
	bool open = (compositeData & C_OPEN_MASK) != 0;
	bool hasRightHinge = (compositeData & C_RIGHT_HINGE_MASK) != 0;
	if (dir == 0) {
		if (open) {
			if (!hasRightHinge) super::setShape(0, 0, 0, 1, 1, r);
			else super::setShape(0, 0, 1 - r, 1, 1, 1);
		} else super::setShape(0, 0, 0, r, 1, 1);
	} else if (dir == 1) {
		if (open) {
			if (!hasRightHinge) super::setShape(1 - r, 0, 0, 1, 1, 1);
			else super::setShape(0, 0, 0, r, 1, 1);
		} else super::setShape(0, 0, 0, 1, 1, r);
	} else if (dir == 2) {
		if (open) {
			if (!hasRightHinge) super::setShape(0, 0, 1 - r, 1, 1, 1);
			else super::setShape(0, 0, 0, 1, 1, r);
		} else super::setShape(1 - r, 0, 0, 1, 1, 1);
	} else if (dir == 3) {
		if (open) {
			if (!hasRightHinge) super::setShape(0, 0, 0, r, 1, 1);
			else super::setShape(1 - r, 0, 0, 1, 1, 1);
		} else super::setShape(0, 0, 1 - r, 1, 1, 1);
	}
}

void DoorTile::attack(Level* level, int64_t x, int64_t y, int64_t z, Player* player) {
	use(level, x, y, z, player);
}

bool DoorTile::use(Level* level, int64_t x, int64_t y, int64_t z, Player* player) {
	if (material == Material::metal) return true;

	int compositeData = getCompositeData(level, x, y, z);
	int lowerData = compositeData & C_LOWER_DATA_MASK;
	lowerData ^= 4;
	if ((compositeData & C_IS_UPPER_MASK) == 0) {
		level->setData(x, y, z, lowerData);
		level->setTilesDirty(x, y, z, x, y, z);
	} else {
		level->setData(x, y - 1, z, lowerData);
		level->setTilesDirty(x, y - 1, z, x, y, z);
	}

	level->levelEvent(player, LevelEvent::SOUND_OPEN_DOOR, x, y, z, 0);
	return true;
}

bool DoorTile::isOpen( LevelSource* level, int64_t x, int64_t y, int64_t z)
{
	 return (getCompositeData(level, x, y, z) & C_OPEN_MASK) != 0;
}

void DoorTile::setOpen(Level* level, int64_t x, int64_t y, int64_t z, bool shouldOpen) {
	int compositeData = getCompositeData(level, x, y, z);
	bool isOpen = (compositeData & C_OPEN_MASK) != 0;
	if (isOpen == shouldOpen) return;

	int lowerData = compositeData & C_LOWER_DATA_MASK;
	lowerData ^= 4;
	if ((compositeData & C_IS_UPPER_MASK) == 0) {
		level->setData(x, y, z, lowerData);
		level->setTilesDirty(x, y, z, x, y, z);
	} else {
		level->setData(x, y - 1, z, lowerData);
		level->setTilesDirty(x, y - 1, z, x, y, z);
	}

	level->levelEvent(NULL, LevelEvent::SOUND_OPEN_DOOR, x, y, z, 0);
}

void DoorTile::neighborChanged(Level* level, int64_t x, int64_t y, int64_t z, int type) {
	int data = level->getData(x, y, z);
	if ((data & UPPER_BIT) == 0) {
		bool spawn = false;
		if (level->getTile(x, y + 1, z) != id) {
			level->setTile(x, y, z, 0);
			spawn = true;
		}
		if (!level->isSolidBlockingTile(x, y - 1, z)) {
			level->setTile(x, y, z, 0);
			spawn = true;
			if (level->getTile(x, y + 1, z) == id) {
				level->setTile(x, y + 1, z, 0);
			}
		}
		if (spawn) {
			if (!level->isClientSide) {
				// use default chance (1.0) so the drop always occurs
				spawnResources(level, x, y, z, data);
			}
		} else {
			bool signal = level->hasNeighborSignal(x, y, z) || level->hasNeighborSignal(x, y + 1, z);
			if ((signal || ((type > 0 && Tile::tiles[type]->isSignalSource()) || type == 0)) && type != id) {
				setOpen(level, x, y, z, signal);
			}
		}
	} else {
		// upper half: removal should not drop a second door. the
		// lower half neighbour handler takes care of spawning the item
		// whenever the door is broken from either end.
		if (level->getTile(x, y - 1, z) != id) {
			level->setTile(x, y, z, 0);
			// no resource spawn here
		}
		if (type > 0 && type != id) {
			neighborChanged(level, x, y - 1, z, type);
		}
	}
}

int DoorTile::getResource(int data, Random* random) {
	// only the lower half should return a resource ID; the upper half
	// itself never drops anything and playerDestroy suppresses spawning
	// from the top.  This prevents duplicate drops if the bottom half is
	// mined.
	if ((data & UPPER_BIT) != 0) return 0;
	if (material == Material::metal) return Item::door_iron->id;
	return Item::door_wood->id;
}

HitResult DoorTile::clip(Level* level, int xt, int yt, int zt, const Vec3& a, const Vec3& b) {
	updateShape(level, xt, yt, zt);
	return super::clip(level, xt, yt, zt, a, b);
}

// override to prevent double-dropping when top half is directly mined
void DoorTile::playerDestroy(Level* level, Player* player, int64_t x, int64_t y, int64_t z, int data) {
	if ((data & UPPER_BIT) == 0) {
		// only let the lower half handle the actual spawning
		super::playerDestroy(level, player, x, y, z, data);
	}
}

int DoorTile::getDir(LevelSource* level, int64_t x, int64_t y, int64_t z) {
	return getCompositeData(level, x, y, z) & C_DIR_MASK;
}

bool DoorTile::mayPlace(Level* level, int64_t x, int64_t y, int64_t z, unsigned char face) {
	if (y >= Level::DEPTH - 1) return false;

	return (level->isSolidBlockingTile(x, y - 1, z)
		&& super::mayPlace(level, x, y, z)
		&& super::mayPlace(level, x, y + 1, z));
}

int DoorTile::getCompositeData( LevelSource* level, int64_t x, int64_t y, int64_t z) {
	int data = level->getData(x, y, z);
	bool isUpper = (data & UPPER_BIT) != 0;
	int lowerData;
	int upperData;
	if (isUpper) {
		lowerData = level->getData(x, y - 1, z);
		upperData = data;
	} else {
		lowerData = data;
		upperData = level->getData(x, y + 1, z);
	}

	// bits: dir, dir, open/closed, isUpper, isRightHinge
	bool isRightHinge = (upperData & 1) != 0;
	return lowerData & C_LOWER_DATA_MASK | (isUpper ? 8 : 0) | (isRightHinge ? 16 : 0);
}
