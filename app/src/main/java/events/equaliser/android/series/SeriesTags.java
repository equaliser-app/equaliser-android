package events.equaliser.android.series;

/**
 * Stores the tags for a series.
 */
class SeriesTags extends SeriesRecyclerViewItem {

    private final String[] tags;

    SeriesTags(String[] tags) {
        this.tags = tags;
    }

    String[] getTags() {
        return tags;
    }
}
