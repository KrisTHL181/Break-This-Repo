#ifndef NET_MINECRAFT_CLIENT_RENDERER__DistanceChunkSorter_H__
#define NET_MINECRAFT_CLIENT_RENDERER__DistanceChunkSorter_H__

//package net.minecraft.client.renderer;

#include "../../world/entity/Entity.h"
#include "Chunk.h"

class DistanceChunkSorter
{
    Entity* player;
    double playerX, playerY, playerZ;   // 原为 float

public:
    DistanceChunkSorter(const Entity* player) {
        playerX = player->x;
        playerY = player->y;
        playerZ = player->z;
    }
    bool operator()(const Chunk* a, const Chunk* b) const {
        // 用 double 计算距离平方，避免精度丢失
        double dA = a->distanceToSqr(playerX, playerY, playerZ);
        double dB = b->distanceToSqr(playerX, playerY, playerZ);
        return dA < dB;
	}
};

#endif /*NET_MINECRAFT_CLIENT_RENDERER__DistanceChunkSorter_H__*/
