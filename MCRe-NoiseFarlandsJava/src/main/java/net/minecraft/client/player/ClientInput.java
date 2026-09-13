package net.minecraft.client.player;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientInput {
    public Input keyPresses = Input.EMPTY;
    protected Vec2 moveVector = Vec2.ZERO;

    public void tick() {
    }

    public Vec2 getMoveVector() {
        return this.moveVector;
    }

    public boolean hasForwardImpulse() {
        return this.moveVector.y > 1.0E-5F;
    }

    public void makeJump() {
        this.keyPresses = new Input(
            this.keyPresses.forward(),
            this.keyPresses.backward(),
            this.keyPresses.left(),
            this.keyPresses.right(),
            true,
            this.keyPresses.shift(),
            this.keyPresses.sprint()
        );
    }
}