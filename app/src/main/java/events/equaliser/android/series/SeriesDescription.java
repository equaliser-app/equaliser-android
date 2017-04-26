package events.equaliser.android.series;

/**
 * Stores the description of a series.
 */
class SeriesDescription extends SeriesRecyclerViewItem {

    private final String description;

    SeriesDescription(String description) {
        this.description = description;
    }

    String getDescription() {
        return description;
    }
}
