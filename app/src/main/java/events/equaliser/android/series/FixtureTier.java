package events.equaliser.android.series;

import java.math.BigDecimal;

/**
 * Stores data about a tier to be displayed in a CardView.
 */
class FixtureTier {

    private final String tier;
    private final BigDecimal price;
    private final boolean available;

    FixtureTier(String tier, BigDecimal price, boolean available) {
        this.tier = tier;
        this.price = price;
        this.available = available;
    }

    String getTier() {
        return tier;
    }

    BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }
}
