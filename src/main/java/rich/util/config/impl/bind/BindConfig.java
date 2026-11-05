package rich.util.config.impl.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import rich.util.config.impl.consolelogger.Logger;
import rich.screens.clickgui.impl.settingsrender.BindComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BindConfig {
    private static BindConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    @Getter
    private int BindKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    @Getter
    private int BindType = 1;

    private BindConfig() {
        Path configDir = Paths.get("Rich", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("Bind.json");
        load();
    }

    public static BindConfig getInstance() {
        if (instance == null) {
            instance = new BindConfig();
        }
        return instance;
    }

    public void setKey(int key) {
        this.BindKey = key;
        this.BindType = (key < 8) ? 0 : (key >= 1000 ? 2 : 1);
    }

    public void setBindType(int type) {
        this.BindType = type;
    }

    public void setKeyAndSave(int key) {
        setKey(key);
        save();
    }

    public void setKeyAndSave(int key, int type) {
        this.BindKey = key;
        this.BindType = type;
        save();
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("BindKey", BindKey);
            obj.addProperty("BindType", BindType);
            Files.writeString(configPath, gson.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("BindConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("BindKey")) {
                BindKey = obj.get("BindKey").getAsInt();
            }
            if (obj.has("BindType")) {
                BindType = obj.get("BindType").getAsInt();
            } else {
                BindType = (BindKey < 8) ? 0 : (BindKey >= 1000 ? 2 : 1);
            }
            Logger.success("BindConfig: Bind.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("BindConfig: Load failed! " + e.getMessage());
        }
    }
}