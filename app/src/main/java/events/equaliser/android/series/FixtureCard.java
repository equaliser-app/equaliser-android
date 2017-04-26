package events.equaliser.android.series;

import java.util.ArrayList;
import java.util.Date;

/**
 * Stores data about an fixture to be displayed in a CardView.
 */
class FixtureCard extends SeriesRecyclerViewItem {

    private final String venue;
    private final Date dateTime;
    private final ArrayList<FixtureTier> tiers;

    FixtureCard(String venue, Date dateTime, ArrayList<FixtureTier> tiers) {
        this.venue = venue;
        this.dateTime = dateTime;
        this.tiers = tiers;
    }

    String getVenue() {
        return venue;
    }

    Date getDateTime() {
        return dateTime;
    }

    ArrayList<FixtureTier> getTiers() {
        return tiers;
    }
}
