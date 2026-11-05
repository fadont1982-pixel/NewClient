package rich.modules.impl.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NameTagLayoutTest {
    @ParameterizedTest
    @ValueSource(doubles = {1, 2, 3, 4, 6})
    void projectedPositionKeepsTheSamePixelPositionAtEveryGuiScale(double actualScale) {
        double guiPosition = 142.25;
        double overlayPosition = guiPosition * NameTagLayout.guiToOverlayScale(actualScale, 2);
        assertEquals(guiPosition * actualScale, overlayPosition * 2, 0.000001);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 2, 3, 4, 6})
    void itemAndCustomFontUseTheSamePixelPosition(double actualScale) {
        double overlayPosition = 213.5;
        double itemGuiPosition = overlayPosition / NameTagLayout.guiToOverlayScale(actualScale, 2);
        assertEquals(overlayPosition * 2, itemGuiPosition * actualScale, 0.000001);
    }

    @Test
    void emptyArmorRowHasNoColumns() {
        assertArrayEquals(new float[0], NameTagLayout.columnCenters(100, new float[0], 3));
    }

    @Test
    void oneArmorItemIsCentered() {
        assertArrayEquals(new float[]{100}, NameTagLayout.columnCenters(100, new float[]{8}, 3));
    }

    @Test
    void fourIconsAreCenteredUsingTheirRealWidthAndGaps() {
        assertArrayEquals(new float[]{83.5f, 94.5f, 105.5f, 116.5f},
                NameTagLayout.columnCenters(100, new float[]{8, 8, 8, 8}, 3));
    }

    @Test
    void longDurabilityLabelsDoNotOverlapAdjacentColumns() {
        float[] widths = {8, 28, 13, 31};
        float[] centers = NameTagLayout.columnCenters(100, widths, 3);
        for (int i = 1; i < centers.length; i++) {
            float previousRight = centers[i - 1] + widths[i - 1] / 2f;
            float currentLeft = centers[i] - widths[i] / 2f;
            assertEquals(3, currentLeft - previousRight, 0.0001f);
        }
        float left = centers[0] - widths[0] / 2f;
        float right = centers[3] + widths[3] / 2f;
        assertEquals(100, (left + right) / 2f);
    }

    @Test
    void armorDoesNotOverlapTheNameWhenDurabilityIsHidden() {
        float iconTop = NameTagLayout.iconTop(100, 8, 0, 3);
        assertEquals(89, iconTop);
        assertEquals(97, iconTop + 8);
    }

    @Test
    void durabilityHasItsOwnSpaceBetweenTheIconAndTheName() {
        float iconTop = NameTagLayout.iconTop(100, 8, 5, 3);
        float durabilityBottom = iconTop + 8 + 3 + 5;
        assertEquals(97, durabilityBottom);
    }

    @Test
    void protectedFriendNameKeepsItsHealthSuffix() {
        assertEquals("Protected 24.5", NameTagLayout.labelWithHealth("Protected", true, 24.5f));
    }

    @Test
    void disabledHealthDoesNotAddAnySuffix() {
        assertEquals("Protected", NameTagLayout.labelWithHealth("Protected", false, 20));
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "20, 20", "20.5, 20.5", "100.04, 100", "-3, 0"})
    void healthFormattingIsStable(float health, String expected) {
        assertEquals(expected, NameTagLayout.formatHealth(health));
    }

    @Test
    void healthDoesNotDependOnTheOperatingSystemLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("19.5", NameTagLayout.formatHealth(19.5f));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void invalidHealthIsNotRenderedAsNanOrInfinity() {
        assertEquals("0", NameTagLayout.formatHealth(Float.NaN));
        assertEquals("0", NameTagLayout.formatHealth(Float.POSITIVE_INFINITY));
        assertEquals("0", NameTagLayout.formatHealth(Float.NEGATIVE_INFINITY));
    }

    @ParameterizedTest
    @CsvSource({"100, 0, 100/100", "100, 25, 75/100", "100, 100, 0/100",
            "100, 200, 0/100", "100, -50, 100/100"})
    void durabilityIsClampedToTheValidRange(int maxDamage, int damage, String expected) {
        assertEquals(expected, NameTagLayout.durabilityText(maxDamage, damage));
    }

    @Test
    void undamageableItemsHaveNoDurabilityLabel() {
        assertEquals("", NameTagLayout.durabilityText(0, 0));
        assertEquals("", NameTagLayout.durabilityText(-1, 0));
    }
}