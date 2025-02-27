package net.satisfy.wildernature.entity.ai;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.satisfy.wildernature.entity.RaccoonEntity;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;

public class RaccoonWashingGoal extends Goal {
    private final RaccoonEntity target;
    int counter;

    public RaccoonWashingGoal(RaccoonEntity mob) {
        this.target = mob;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE, Flag.JUMP));
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }


    @Override
    public boolean canUse() {
        var r = new Random().nextFloat();
        return r < 0.01f && !target.isRaccoonRunning();
    }

    @Override
    public boolean canContinueToUse() {
        return counter > 0 && counter < 48 && !target.isRaccoonRunning();
    }

    @Override
    public void tick() {
        counter++;
    }

    public static final AttributeModifier modifier = new AttributeModifier("racoon_wash_do_not_move", -1000, AttributeModifier.Operation.ADDITION);

    @Override
    public void start() {
        counter = 0;
        Objects.requireNonNull(target.getAttribute(Attributes.MOVEMENT_SPEED)).addTransientModifier(modifier);
        target.startWash();
        super.start();
    }

    @Override
    public void stop() {
        Objects.requireNonNull(target.getAttribute(Attributes.MOVEMENT_SPEED)).removeModifier(modifier);
        target.stopWash();
        super.stop();
    }
}
