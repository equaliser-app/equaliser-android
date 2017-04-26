package events.equaliser.android.series;

/**
 * Stores the URL of a series's image.
 */
class SeriesImage extends SeriesRecyclerViewItem {

    private final String imageUrl;

    SeriesImage(String imageUrl) {

        this.imageUrl = imageUrl;
    }

    String getImageUrl() {
        return imageUrl;
    }
}
