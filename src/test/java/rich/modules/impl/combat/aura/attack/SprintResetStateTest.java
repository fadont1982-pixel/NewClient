package rich.modules.impl.combat.aura.attack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SprintResetStateTest {
    private final SprintResetState state = new SprintResetState();
    private final Object player = new Object();
    private final Object world = new Object();
    private final Object target = new Object();

    private void arm() {
        state.arm(player, world, target, 100);
    }

    @Test
    void startsIdle() {
        assertFalse(state.isPending());
        assertFalse(state.isReady(1000));
        assertFalse(state.isValid(player, world, target, 1000));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101})
    void waitsForTwoTicksBeforeAllowingAnAttack(int tick) {
        arm();
        assertTrue(state.isValid(player, world, target, tick));
        assertFalse(state.isReady(tick));
    }

    @ParameterizedTest
    @ValueSource(ints = {102, 103, 105})
    void waitEventuallyBecomesReady(int tick) {
        arm();
        assertTrue(state.isValid(player, world, target, tick));
        assertTrue(state.isReady(tick));
    }

    @ParameterizedTest
    @ValueSource(ints = {106, 107, 10000})
    void missedAttackCannotLeaveAnUnboundedPendingWait(int tick) {
        arm();
        assertFalse(state.isValid(player, world, target, tick));
        assertFalse(state.isReady(tick));
    }

    @Test
    void repeatedAttackAttemptsDoNotPostponeTheReadyTick() {
        arm();
        state.arm(player, world, target, 101);
        state.arm(player, world, target, 102);
        assertTrue(state.isReady(102));
        assertFalse(state.isValid(player, world, target, 106));
    }

    @Test
    void respawnCannotReuseTheOldPlayersDeadline() {
        arm();
        assertFalse(state.isValid(new Object(), world, target, 1));
        state.clear();
        state.arm(new Object(), world, target, 1);
        assertTrue(state.isReady(3));
    }

    @Test
    void changingWorldInvalidatesTheWaitEvenIfThePlayerObjectIsReused() {
        arm();
        assertFalse(state.isValid(player, new Object(), target, 102));
    }

    @Test
    void switchingToAnotherTargetInvalidatesTheWait() {
        arm();
        assertFalse(state.isValid(player, world, new Object(), 102));
    }

    @Test
    void losingTheTargetInvalidatesTheWait() {
        arm();
        assertFalse(state.isValid(player, world, null, 102));
    }

    @Test
    void disconnectingInvalidatesTheWait() {
        arm();
        assertFalse(state.isValid(null, null, null, 0));
    }

    @Test
    void tickCounterGoingBackwardsDoesNotWaitForTheOldTickNumber() {
        arm();
        assertFalse(state.isValid(player, world, target, 0));
        assertFalse(state.isReady(0));
    }

    @Test
    void tickCounterOverflowCannotTurnIntoAPrematureAttack() {
        state.arm(player, world, target, Integer.MAX_VALUE - 1);
        assertFalse(state.isReady(Integer.MIN_VALUE));
        assertFalse(state.isValid(player, world, target, Integer.MIN_VALUE));
    }

    @Test
    void cancellationClearsAllPendingState() {
        arm();
        state.clear();
        assertFalse(state.isPending());
        assertFalse(state.isReady(102));
        assertFalse(state.isValid(player, world, target, 102));
    }

    @Test
    void cancellationIsIdempotent() {
        arm();
        state.clear();
        state.clear();
        assertFalse(state.isPending());
    }

    @Test
    void attackAfterCancellationUsesANewDeadline() {
        arm();
        state.clear();
        state.arm(player, world, target, 200);
        assertFalse(state.isReady(201));
        assertTrue(state.isReady(202));
    }

    @Test
    void newAttemptAfterTimeoutDoesNotReuseAnAlreadyReadyWait() {
        arm();
        state.arm(player, world, target, 110);
        assertFalse(state.isReady(110));
        assertTrue(state.isReady(112));
    }

    @Test
    void contextIsComparedByIdentityNotEquals() {
        String oldTarget = new String("same-id");
        String newTarget = new String("same-id");
        state.arm(player, world, oldTarget, 100);
        assertFalse(state.isValid(player, world, newTarget, 102));
    }

    @Test
    void nullOwnersCannotArmAPendingWait() {
        assertThrows(NullPointerException.class, () -> state.arm(null, world, target, 100));
        assertThrows(NullPointerException.class, () -> state.arm(player, null, target, 100));
        assertThrows(NullPointerException.class, () -> state.arm(player, world, null, 100));
        assertFalse(state.isPending());
    }
}