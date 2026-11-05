package rich.util.modules;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.api.EventManager;
import rich.events.impl.HotBarScrollEvent;
import rich.events.impl.KeyEvent;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.settingsrender.BindComponent;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleSwitcher implements IMinecraft {
    List<ModuleStructure> moduleStructures;

    public ModuleSwitcher(List<ModuleStructure> moduleStructures, EventManager eventManager) {
        this.moduleStructures = moduleStructures;
        eventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.currentScreen != null) return;
        for (ModuleStructure moduleStructure : moduleStructures) {
            if (event.key() != moduleStructure.getKey()) continue;
            try {
                if (moduleStructure.getType() == 0 && event.type() == InputUtil.Type.MOUSE && event.action() == 1) {
                    moduleStructure.switchState();
                } else if (moduleStructure.getType() == 1 && event.type() == InputUtil.Type.KEYSYM && event.action() == 1) {
                    moduleStructure.switchState();
                } else if (moduleStructure.getType() == 2) {
                    if (moduleStructure.getKey() == BindComponent.MIDDLE_MOUSE_BIND
                            && event.type() == InputUtil.Type.MOUSE
                            && event.key() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                            && event.action() == 1) {
                        moduleStructure.switchState();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent event) {
        if (mc.currentScreen != null) return;
        for (ModuleStructure moduleStructure : moduleStructures) {
            if (moduleStructure.getType() != 2) continue;
            int key = moduleStructure.getKey();
            boolean matches = false;
            if (key == BindComponent.SCROLL_UP_BIND && event.getVertical() > 0) {
                matches = true;
            } else if (key == BindComponent.SCROLL_DOWN_BIND && event.getVertical() < 0) {
                matches = true;
            }
            if (matches) {
                try {
                    event.setCancelled(true);
                    moduleStructure.switchState();
                } catch (Exception ignored) {
                }
            }
        }
    }
}