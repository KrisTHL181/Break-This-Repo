CXX ?= c++
CXXFLAGS ?= -std=c++11

CONDIMENTS := chives butter pepper salt tabasco tomatoes onion \
              sour_cream crumbled_bacon grated_cheese ketchup
TARGETS := potato $(CONDIMENTS) oven

OVEN_BROKEN_CHANCE ?= 20

define BUILD_CONDIMENT
	@if [ $$(( $$(date +%s%N) % 100 )) -lt 3 ]; then \
		$(CXX) $(CXXFLAGS) -DCONDIMENT_NAME='"$(1)"' $(2) -DCONDIMENT_EXPIRED condiment.cpp -o $@; \
	else \
		$(CXX) $(CXXFLAGS) -DCONDIMENT_NAME='"$(1)"' $(2) condiment.cpp -o $@; \
	fi
endef

.PHONY: all clean potato $(CONDIMENTS) oven

all: $(TARGETS)

potato: potato.cpp
	@if [ $$(( ($$$$ * 7 + 13) % 100 )) -lt 30 ]; then \
		$(CXX) $(CXXFLAGS) -DPOTATO_SPROUTING $< -o $@; \
	else \
		$(CXX) $(CXXFLAGS) $< -o $@; \
	fi

oven: oven.cpp
	@if [ $$(( $$(date +%s%N) % 100 )) -lt $(OVEN_BROKEN_CHANCE) ]; then \
		$(CXX) $(CXXFLAGS) -DOVEN_BROKEN $< -o $@; \
	else \
		$(CXX) $(CXXFLAGS) $< -o $@; \
	fi

chives: condiment.cpp
	$(call BUILD_CONDIMENT,chives,-DCONDIMENT_DELICIOUS)

butter: condiment.cpp
	$(call BUILD_CONDIMENT,butter,-DCONDIMENT_DELICIOUS)

pepper: condiment.cpp
	$(call BUILD_CONDIMENT,pepper,)

salt: condiment.cpp
	$(call BUILD_CONDIMENT,salt,-DCONDIMENT_DELICIOUS)

tabasco: condiment.cpp
	$(call BUILD_CONDIMENT,tabasco,)

tomatoes: condiment.cpp
	$(call BUILD_CONDIMENT,tomatoes,-DCONDIMENT_DELICIOUS)

onion: condiment.cpp
	$(call BUILD_CONDIMENT,onion,-DCONDIMENT_DELICIOUS)

sour_cream: condiment.cpp
	$(call BUILD_CONDIMENT,sour cream,-DCONDIMENT_DELICIOUS)

crumbled_bacon: condiment.cpp
	$(call BUILD_CONDIMENT,crumbled bacon,-DCONDIMENT_DELICIOUS)

grated_cheese: condiment.cpp
	$(call BUILD_CONDIMENT,grated cheese,-DCONDIMENT_DELICIOUS)

ketchup: condiment.cpp
	$(call BUILD_CONDIMENT,ketchup,)

clean:
	rm -f $(TARGETS)
