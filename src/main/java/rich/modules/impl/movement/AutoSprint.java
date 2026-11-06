package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    private static volatile long sprintSuppressUntil = 0;
    private static volatile boolean attackSprintSuppressed = false;

    @Getter
    private final BooleanSetting noReset = new BooleanSetting("Не сбрасывать спринт", "Don't reset sprint for crits")
            .setValue(false);

    public AutoSprint() {
        super("AutoSprint", ModuleCategory.MOVEMENT);
        settings(noReset);
    }

    public static void suppressSprint(long ms) {
        sprintSuppressUntil = System.currentTimeMillis() + ms;
    }

    public static boolean isSprintSuppressed() {
        return attackSprintSuppressed || System.currentTimeMillis() < sprintSuppressUntil;
    }

    public static void setAttackSprintSuppressed(boolean suppressed) {
        attackSprintSuppressed = suppressed;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null)
            return;

        processSprint();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processSprint() {
        if (isSprintSuppressed())
            return;
        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        boolean canSprint = !horizontal && mc.player.forwardSpeed > 0;

        if (sneaking)
            return;

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

}