package rich.modules.impl.render;

import java.util.Locale;

/** Pure layout/label calculations, independent of the Minecraft renderer. */
final class NameTagLayout {
    private NameTagLayout() {
    }

    static double guiToOverlayScale(double actualGuiScale, double overlayScale) {
        return actualGuiScale / overlayScale;
    }

    static float[] columnCenters(float centerX, float[] widths, float gap) {
        float totalWidth = Math.max(0, widths.length - 1) * gap;
        for (float width : widths) {
            totalWidth += width;
        }

        float[] centers = new float[widths.length];
        float left = centerX - totalWidth / 2f;
        for (int i = 0; i < widths.length; i++) {
            centers[i] = left + widths[i] / 2f;
            left += widths[i] + gap;
        }
        return centers;
    }

    static float iconTop(float tagTop, float iconSize, float durabilityHeight, float gap) {
        float detailSpace = durabilityHeight > 0 ? durabilityHeight + gap : 0;
        return tagTop - gap - detailSpace - iconSize;
    }

    static String labelWithHealth(String name, boolean showHealth, float health) {
        return showHealth ? name + " " + formatHealth(health) : name;
    }

    static String formatHealth(float health) {
        float safeHealth = Float.isFinite(health) ? Math.max(0, health) : 0;
        String value = String.format(Locale.ROOT, "%.1f", safeHealth);
        return value.endsWith(".0") ? value.substring(0, value.length() - 2) : value;
    }

    static String durabilityText(int maxDamage, int damage) {
        if (maxDamage <= 0) {
            return "";
        }
        int remaining = maxDamage - Math.max(0, Math.min(maxDamage, damage));
        return remaining + "/" + maxDamage;
    }
}