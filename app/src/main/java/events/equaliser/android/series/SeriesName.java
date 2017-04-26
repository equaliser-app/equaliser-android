package events.equaliser.android.series;

/**
 * Stores a series's name.
 */
class SeriesName extends SeriesRecyclerViewItem {

    private final String name;

    SeriesName(String name) {

        this.name = name;
    }

    public String getName() {
        return name;
    }
}
