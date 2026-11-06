package rich.modules.impl.combat.aura.attack;

import java.util.Objects;

/** A bounded, session-owned wait; it must never carry a tick deadline into another player/world. */
final class SprintResetState {
    static final int WAIT_TICKS = 2;
    static final int MAX_PENDING_TICKS = 6;

    private Object player;
    private Object world;
    private Object target;
    private int startedTick;

    void arm(Object player, Object world, Object target, int tick) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(world);
        Objects.requireNonNull(target);
        if (isValid(player, world, target, tick)) {
            return; // Repeated attempts must not move the deadline forward every tick.
        }
        this.player = player;
        this.world = world;
        this.target = target;
        startedTick = tick;
    }

    boolean isPending() {
        return player != null;
    }

    boolean isValid(Object player, Object world, Object target, int tick) {
        long elapsed = (long) tick - startedTick;
        return isPending() && this.player == player && this.world == world && this.target == target
                && elapsed >= 0 && elapsed < MAX_PENDING_TICKS;
    }

    boolean isReady(int tick) {
        long elapsed = (long) tick - startedTick;
        return isPending() && elapsed >= WAIT_TICKS && elapsed < MAX_PENDING_TICKS;
    }

    void clear() {
        player = null;
        world = null;
        target = null;
        startedTick = 0;
    }
}