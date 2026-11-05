package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import rich.events.api.EventHandler;
import rich.events.impl.DrawEvent;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldLoadEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.ColorUtil;
import rich.util.Instance;
import rich.util.math.Projection;
import rich.util.modules.esp.RwPrefix;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;
import rich.util.repository.friend.FriendUtils;
import rich.modules.impl.player.NameProtect;
import rich.modules.impl.combat.AntiBot;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameTags extends ModuleStructure {

    public static NameTags getInstance() {
        return Instance.get(NameTags.class);
    }

    List<PlayerEntity> players = new ArrayList<>();

    MultiSelectSetting displaySetting = new MultiSelectSetting("Настройки", "Что отображать в неймтеге")
            .value("HP", "Armor", "Durability", "Main Hand", "Off Hand", "Count", "Enchants")
            .selected("HP", "Armor", "Durability", "Main Hand", "Off Hand", "Count", "Enchants");

    public NameTags() {
        super("NameTags", "NameTags", ModuleCategory.RENDER);
        settings(displaySetting);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                    .forEach(players::add);
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        float tickDelta = e.getPartialTicks();
        float size = 5.5f;

        for (PlayerEntity player : players) {
            if (player == null || player == mc.player) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;

            Vector4d vec4d = Projection.getVector4D(player, tickDelta);
            float distance = (float) mc.gameRenderer.getCamera().getCameraPos().distanceTo(player.getBoundingBox().getCenter());
            boolean friend = FriendUtils.isFriend(player);

            if (distance < 1) continue;
            if (Projection.cantSee(vec4d)) continue;

            drawNameTag(context, player, friend, Projection.centerX(vec4d), vec4d.y - 2, size, vec4d);
        }
    }

    private void drawNameTag(DrawContext context, PlayerEntity player, boolean friend, double centerX, double startY, float size, Vector4d vec) {
        StringBuilder extraInfo = new StringBuilder();
        if (friend) extraInfo.append("[Friend] ");
        if (AntiBot.getInstance() != null && AntiBot.getInstance().isState() && AntiBot.getInstance().isBot(player)) {
            extraInfo.append("[BOT] ");
        }

        String displayName = player.getDisplayName().getString();
        RwPrefix.ParsedName parsed = RwPrefix.parseDisplayName(displayName);

        String sphere = "";
        ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
        if (offHandStack.getItem().equals(Items.PLAYER_HEAD) || offHandStack.getItem().equals(Items.TOTEM_OF_UNDYING)) {
            sphere = getSphere(offHandStack);
        }

        String prefixPart = "";
        if (!parsed.prefix.isEmpty()) {
            prefixPart = parsed.prefix + " ";
        }
        String namePart = parsed.name;
        if (displaySetting.isSelected("HP")) {
            namePart = namePart + " " + getHealthString(getHealth(player));
        }
        if (friend) {
            try {
                NameProtect nameProtect = Instance.get(NameProtect.class);
                if (nameProtect != null && nameProtect.isState() && nameProtect.getFriendsSetting().isValue()) {
                    String custom = nameProtect.getNameSetting().getText();
                    if (custom != null && !custom.isEmpty()) {
                        namePart = custom;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        String clanPart = !parsed.clan.isEmpty() ? " " + parsed.clan : "";
        String spherePart = sphere;
        String extraPart = extraInfo.toString();

        float extraWidth = extraPart.isEmpty() ? 0 : Fonts.TEST.getWidth(extraPart, size);
        float prefixWidth = prefixPart.isEmpty() ? 0 : Fonts.TEST.getWidth(prefixPart, size);
        float nameWidth = Fonts.TEST.getWidth(namePart, size);
        float clanWidth = clanPart.isEmpty() ? 0 : Fonts.TEST.getWidth(clanPart, size);
        float sphereWidth = spherePart.isEmpty() ? 0 : Fonts.TEST.getWidth(spherePart, size);

        float totalWidth = extraWidth + prefixWidth + nameWidth + clanWidth + sphereWidth;
        float height = Fonts.TEST.getHeight(size);

        float posX = (float) centerX - totalWidth / 2;
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1.25f, totalWidth + 8, height + 2, 0x80000000, 2f);

        float drawX = posX;

        if (!extraPart.isEmpty()) {
            Fonts.TEST.draw(extraPart, drawX, posY, size, friend ? 0xFF00FF00 : 0xFFFF5555);
            drawX += extraWidth;
        }

        if (!prefixPart.isEmpty()) {
            Fonts.TEST.draw(prefixPart, drawX, posY, size, 0xFF888888);
            drawX += prefixWidth;
        }

        Fonts.TEST.draw(namePart, drawX, posY, size, 0xFFFFFFFF);
        drawX += nameWidth;

        if (!clanPart.isEmpty()) {
            Fonts.TEST.draw(clanPart, drawX, posY, size, 0xFF888888);
            drawX += clanWidth;
        }

        if (!spherePart.isEmpty()) {
            Fonts.TEST.draw(spherePart, drawX, posY, size, 0xFF888888);
        }

        if (displaySetting.isSelected("Armor")) {
            drawArmor(context, player, vec);
        }
        if (displaySetting.isSelected("Main Hand") || displaySetting.isSelected("Off Hand")) {
            drawHands(context, player, vec, size);
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        float posX = (float) (Projection.centerX(vec) - items.size() * 4.5f);
        float posY = (float) (vec.y - 20);
        float offset = 0;

        for (ItemStack stack : items) {
            ItemRender.drawItemWithContext(context, stack, posX + offset, posY, 0.5F, 1.0F);

            if (displaySetting.isSelected("Durability")) {
                int maxDamage = stack.getMaxDamage();
                if (maxDamage > 0) {
                    int remaining = maxDamage - stack.getDamage();
                    String durText = remaining + "/" + maxDamage;
                    float textWidth = Fonts.TEST.getWidth(durText, 4f);
                    Fonts.TEST.draw(durText, posX + offset + 8 - textWidth / 2, posY + 10, 4f, 0xFFFFFFFF);
                }
            }

            if (displaySetting.isSelected("Enchants")) {
                var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
                if (enchants != null && !enchants.isEmpty()) {
                    StringBuilder enchText = new StringBuilder();
                    for (var entry : enchants.getEnchantments()) {
                        String id = entry.getIdAsString();
                        if (id != null) {
                            String shortName = id.substring(id.lastIndexOf(':') + 1);
                            if (!shortName.isEmpty()) {
                                enchText.append(shortName.charAt(0));
                            }
                        }
                        enchText.append(enchants.getLevel(entry));
                        enchText.append(" ");
                    }
                    String text = enchText.toString().trim();
                    if (!text.isEmpty()) {
                        float textWidth = Fonts.TEST.getWidth(text, 4f);
                        Fonts.TEST.draw(text, posX + offset + 8 - textWidth / 2, posY - 4, 4f, 0xFFAAAAFF);
                    }
                }
            }

            offset += 11;
        }
    }

    private void drawHands(DrawContext context, PlayerEntity player, Vector4d vec, float size) {
        double posY = vec.w;

        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        boolean hasHandSelection = displaySetting.isSelected("Main Hand") || displaySetting.isSelected("Off Hand");
        boolean showMain = !hasHandSelection || displaySetting.isSelected("Main Hand");
        boolean showOff = !hasHandSelection || displaySetting.isSelected("Off Hand");

        if (showMain && !mainHand.isEmpty()) {
            String text = mainHand.getName().getString();
            if (displaySetting.isSelected("Count") && mainHand.getCount() > 1) {
                text += " x" + mainHand.getCount();
            }
            posY += Fonts.TEST.getHeight(size) / 2.0 + 6;
            drawText(context, text, Projection.centerX(vec), posY, size);
        }

        if (showOff && !offHand.isEmpty()) {
            String text = offHand.getName().getString();
            if (displaySetting.isSelected("Count") && offHand.getCount() > 1) {
                text += " x" + offHand.getCount();
            }
            posY += Fonts.TEST.getHeight(size) / 2.0 + 6;
            drawText(context, text, Projection.centerX(vec), posY, size);
        }
    }

    private void drawText(DrawContext context, String text, double startX, double startY, float size) {
        String cleanText = RwPrefix.stripFormatting(text);
        float width = Fonts.TEST.getWidth(cleanText, size);
        float height = Fonts.TEST.getHeight(size);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1, width + 8, height + 2, 0x80000000, 2f);
        Fonts.TEST.draw(cleanText, posX, posY, size, 0xFFFFFFFF);
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (Network.isFunTime() && component != null) {
            net.minecraft.nbt.NbtCompound compound = component.copyNbt();
            int tslevel = compound.getInt("tslevel").orElse(0);
            if (tslevel != 0) {
                String donItem = compound.getString("don-item").orElse("");
                return " [" + donItem.replace("sphere-", "").toUpperCase() + "]";
            }
        }
        return "";
    }

    private float getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
    }

    private int getFriendColor() {
        return 0xFF00FF00;
    }
}