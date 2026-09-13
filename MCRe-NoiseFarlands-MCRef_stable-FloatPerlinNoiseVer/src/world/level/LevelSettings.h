#ifndef NET_MINECRAFT_WORLD_LEVEL__LevelSettings_H__
#define NET_MINECRAFT_WORLD_LEVEL__LevelSettings_H__

//package net.minecraft.world.level;

namespace GameType {
	const int Undefined = -1;
	const int Survival = 0;
	const int Creative = 1;

	const int Default = Creative;
}

class LevelSettings
{
public:
    LevelSettings(long seed, int gameType, bool allowCheats = false, bool useEndGenerator = false)
    :   seed(seed),
        gameType(gameType),
        allowCheats(allowCheats),
        useEndGenerator(useEndGenerator)
    {
    }
	static LevelSettings None() {
        return LevelSettings(-1, -1, false, false);
	}

    long getSeed() const {
        return seed;
    }

    int getGameType() const {
        return gameType;
    }

    bool getAllowCheats() const {
        return allowCheats;
    }

bool getUseEndGenerator() const { return useEndGenerator; }  // 🔧 新增

	//
	// Those two should actually not be here
	// @todo: Move out when we add LevelSettings.cpp :p
	//
	static int validateGameType(int gameType) {
        switch (gameType) {
		case GameType::Creative:
		case GameType::Survival:
            return gameType;
        }
        return GameType::Default;
    }

	static std::string gameTypeToString(int gameType) {
		if (gameType == GameType::Survival) return "Survival";
		if (gameType == GameType::Creative) return "Creative";
		return "Undefined";
	}

private:
    const long seed;
    const int gameType;
    const bool allowCheats;
    const bool useEndGenerator;  // 🔧 新增
};

#endif /*NET_MINECRAFT_WORLD_LEVEL__LevelSettings_H__*/
