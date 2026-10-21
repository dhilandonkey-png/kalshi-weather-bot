package weatherbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import weatherbot.kalshi.Market;
import weatherbot.kalshi.StrikeType;

class TemperatureModelTest {

    private final TemperatureModel model = new TemperatureModel(3.0, 0.0);

    static Market between(double floor, double cap) {
        return new Market("T" + floor, "KXHIGHNY-26OCT22", "", StrikeType.BETWEEN, floor, cap, 0, 0, 0, 0, "open", "");
    }

    static Market greater(double floor) {
        return new Market("G" + floor, "KXHIGHNY-26OCT22", "", StrikeType.GREATER, floor, null, 0, 0, 0, 0, "open", "");
    }

    static Market less(double cap) {
        return new Market("L" + cap, "KXHIGHNY-26OCT22", "", StrikeType.LESS, null, cap, 0, 0, 0, 0, "open", "");
    }

    @Test
    void normalCdfMatchesKnownValues() {
        assertEquals(0.5, Normal.cdf(0), 1e-7);
        assertEquals(0.841345, Normal.cdf(1), 1e-6);
        assertEquals(0.022750, Normal.cdf(-2), 1e-6);
    }

    @Test
    void aRangeCenteredOnTheForecastIsTheMostLikely() {
        double centered = model.probabilityYes(between(71, 72), 71.5);
        double offBy4 = model.probabilityYes(between(75, 76), 71.5);
        assertTrue(centered > offBy4);
        // 71–72° covers 70.5 to 72.5, which is ±1° around 71.5, or ±1/3 of a standard deviation.
        assertEquals(Normal.cdf(1.0 / 3) - Normal.cdf(-1.0 / 3), centered, 1e-9);
    }

    @Test
    void wholeDegreeBoundariesUseHalfDegrees() {
        // "Above 75" means 76 or more, so it starts at 75.5. With the forecast at 75.5, that's exactly half.
        assertEquals(0.5, model.probabilityYes(greater(75), 75.5), 1e-9);
        // "Below 68" means 67 or less, so it ends at 67.5.
        assertEquals(0.5, model.probabilityYes(less(68), 67.5), 1e-9);
    }

    @Test
    void aFullSetOfRangesAddsUpToOne() {
        // Kalshi lists a ladder like: below 66, 66–67, 68–69, ..., 76–77, above 77.
        List<Market> ladder = new ArrayList<>();
        ladder.add(less(66));
        for (int lo = 66; lo <= 76; lo += 2) {
            ladder.add(between(lo, lo + 1));
        }
        ladder.add(greater(77));

        for (double forecast : new double[] {58, 66, 71.3, 77.5, 90}) {
            double total = ladder.stream().mapToDouble(m -> model.probabilityYes(m, forecast)).sum();
            assertEquals(1.0, total, 1e-6, "forecast " + forecast);
        }
    }

    @Test
    void biasShiftsTheForecast() {
        var warmBias = new TemperatureModel(3.0, 2.0);
        assertEquals(model.probabilityYes(between(73, 74), 73.5), warmBias.probabilityYes(between(73, 74), 71.5), 1e-12);
    }

    @Test
    void sigmaMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new TemperatureModel(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TemperatureModel(Double.NaN, 0));
    }
}
