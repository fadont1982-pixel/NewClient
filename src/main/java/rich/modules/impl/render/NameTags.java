package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.joml.Vector4d;
import rich.events.api.EventHandler;
import rich.events.impl.DrawEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.util.Instance;
import rich.util.math.Projection;
import rich.util.modules.esp.RwPrefix;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.repository.friend.FriendUtils;
import rich.modules.impl.player.NameProtect;
import rich.modules.impl.combat.AntiBot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NameTags extends ModuleStructure {

    public static NameTags getInstance() {
        return Instance.get(NameTags.class);
    }

    static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    static final float ICON_SIZE = 8f;
    static final float DETAIL_SIZE = 4f;
    static final float ITEM_GAP = 3f;

    MultiSelectSetting displaySetting = new MultiSelectSetting("Настройки", "Что отображать в неймтеге")
            .value("HP", "Armor", "Durability", "Main Hand", "Off Hand", "Count", "Enchants")
            .selected("HP", "Armor", "Durability", "Main Hand", "Off Hand", "Count", "Enchants");

    public NameTags() {
        super("NameTags", "NameTags", ModuleCategory.RENDER);
        settings(displaySetting);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (mc.world == null || mc.player == null || mc.gameRenderer == null) return;
        var camera = mc.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) return;

        DrawContext context = e.getDrawContext();
        float tickDelta = e.getPartialTicks();
        float size = 5.5f;
        double guiToOverlay = NameTagLayout.guiToOverlayScale(
                mc.getWindow().getScaleFactor(), Render2D.getFixedGuiScale());

        // Read the current world directly: a tick cache can retain players after a world switch.
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player || player.isRemoved()) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;
            if (camera.getCameraPos().squaredDistanceTo(player.getBoundingBox().getCenter()) < 1) continue;

            Vector4d vec4d = Projection.getVector4D(player, tickDelta);
            if (Projection.cantSee(vec4d)) continue;

            // Projection uses the actual GUI scale; the custom font/rect pipelines always use 2.
            vec4d.mul(guiToOverlay);
            drawNameTag(context, player, FriendUtils.isFriend(player),
                    Projection.centerX(vec4d), vec4d.y - 2, size, vec4d);
        }
    }

    private void drawNameTag(DrawContext context, PlayerEntity player, boolean friend, double centerX, double startY, float size, Vector4d vec) {
        StringBuilder extraInfo = new StringBuilder();
        if (friend) extraInfo.append("[Friend] ");
        AntiBot antiBot = AntiBot.getInstance();
        if (antiBot != null && antiBot.isState() && antiBot.isBot(player)) {
            extraInfo.append("[BOT] ");
        }

        var displayText = player.getDisplayName();
        String displayName = displayText == null ? player.getName().getString() : displayText.getString();
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
        String namePart = parsed.name.isBlank() ? player.getName().getString() : parsed.name;
        if (friend) {
            NameProtect nameProtect = Instance.get(NameProtect.class);
            if (nameProtect != null && nameProtect.isState() && nameProtect.getFriendsSetting().isValue()) {
                String custom = RwPrefix.stripFormatting(nameProtect.getNameSetting().getText());
                if (!custom.isBlank()) {
                    namePart = custom;
                }
            }
        }
        // Mask only the name, not the health suffix.
        namePart = NameTagLayout.labelWithHealth(namePart, displaySetting.isSelected("HP"), getHealth(player));
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
            drawArmor(context, player, (float) centerX, posY - 1.25f);
        }
        if (displaySetting.isSelected("Main Hand") || displaySetting.isSelected("Off Hand")) {
            drawHands(player, vec, size);
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, float centerX, float tagTop) {
        List<ArmorEntry> items = new ArrayList<>(ARMOR_SLOTS.length);
        boolean hasDurability = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;

            String durability = displaySetting.isSelected("Durability") && stack.isDamageable()
                    ? NameTagLayout.durabilityText(stack.getMaxDamage(), stack.getDamage()) : "";
            List<String> enchantments = displaySetting.isSelected("Enchants")
                    ? getEnchantments(stack) : List.of();

            float width = Math.max(ICON_SIZE, Fonts.TEST.getWidth(durability, DETAIL_SIZE));
            for (String enchantment : enchantments) {
                width = Math.max(width, Fonts.TEST.getWidth(enchantment, DETAIL_SIZE));
            }
            items.add(new ArmorEntry(stack, durability, enchantments, width));
            hasDurability |= !durability.isEmpty();
        }
        if (items.isEmpty()) return;

        float[] widths = new float[items.size()];
        for (int i = 0; i < items.size(); i++) {
            widths[i] = items.get(i).width();
        }
        float[] centers = NameTagLayout.columnCenters(centerX, widths, ITEM_GAP);
        float detailHeight = Fonts.TEST.getHeight(DETAIL_SIZE);
        float posY = NameTagLayout.iconTop(tagTop, ICON_SIZE,
                hasDurability ? detailHeight : 0, ITEM_GAP);

        for (int i = 0; i < items.size(); i++) {
            ArmorEntry item = items.get(i);
            float itemCenter = centers[i];
            drawArmorItem(context, item.stack(), itemCenter - ICON_SIZE / 2f, posY);

            if (!item.durability().isEmpty()) {
                Fonts.TEST.drawCentered(item.durability(), itemCenter, posY + ICON_SIZE + ITEM_GAP,
                        DETAIL_SIZE, 0xFFFFFFFF);
            }

            float enchantY = posY - ITEM_GAP - item.enchantments().size() * detailHeight;
            for (String enchantment : item.enchantments()) {
                Fonts.TEST.drawCentered(enchantment, itemCenter, enchantY, DETAIL_SIZE, 0xFFAAAAFF);
                enchantY += detailHeight;
            }
        }
    }

    private void drawArmorItem(DrawContext context, ItemStack stack, float x, float y) {
        float overlayToGui = (float) (Render2D.getFixedGuiScale() / mc.getWindow().getScaleFactor());
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        try {
            // Translate AND scale in overlay coordinates, using the effective (not requested) GUI scale.
            matrices.scale(overlayToGui, overlayToGui);
            matrices.translate(x, y);
            matrices.scale(ICON_SIZE / 16f, ICON_SIZE / 16f);
            context.drawItem(stack, 0, 0);
        } finally {
            matrices.popMatrix();
        }
    }

    private List<String> getEnchantments(ItemStack stack) {
        var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return List.of();

        return enchantments.getEnchantments().stream()
                .sorted(Comparator.comparing(entry -> entry.getIdAsString()))
                .map(entry -> {
                    String id = entry.getIdAsString();
                    String name = id.substring(id.lastIndexOf(':') + 1);
                    String abbreviation = name.isEmpty() ? "?" : name.substring(0, 1);
                    return abbreviation + enchantments.getLevel(entry);
                })
                .toList();
    }

    private record ArmorEntry(ItemStack stack, String durability, List<String> enchantments, float width) {
    }

    private void drawHands(PlayerEntity player, Vector4d vec, float size) {
        double posY = vec.w;

        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        if (displaySetting.isSelected("Main Hand") && !mainHand.isEmpty()) {
            String text = mainHand.getName().getString();
            if (displaySetting.isSelected("Count") && mainHand.getCount() > 1) {
                text += " x" + mainHand.getCount();
            }
            posY += Fonts.TEST.getHeight(size) + 4;
            drawText(text, Projection.centerX(vec), posY, size);
        }

        if (displaySetting.isSelected("Off Hand") && !offHand.isEmpty()) {
            String text = offHand.getName().getString();
            if (displaySetting.isSelected("Count") && offHand.getCount() > 1) {
                text += " x" + offHand.getCount();
            }
            posY += Fonts.TEST.getHeight(size) + 4;
            drawText(text, Projection.centerX(vec), posY, size);
        }
    }

    private void drawText(String text, double startX, double startY, float size) {
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
                if (!donItem.isBlank()) {
                    String sphere = donItem.startsWith("sphere-") ? donItem.substring(7) : donItem;
                    return sphere.isBlank() ? "" : " [" + sphere.toUpperCase(Locale.ROOT) + "]";
                }
            }
        }
        return "";
    }

    private float getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

}