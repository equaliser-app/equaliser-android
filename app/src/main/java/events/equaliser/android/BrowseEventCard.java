package events.equaliser.android;

/**
 * Stores data about an event to be displayed in a CardView.
 */
class BrowseEventCard {

    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final int eventId;

    BrowseEventCard(String title, String subtitle, String imageUrl, int eventId) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.subtitle = subtitle;
        this.eventId = eventId;
    }

    String getTitle() {
        return title;
    }

    String getSubtitle() {
        return subtitle;
    }

    String getImageUrl() {
        return imageUrl;
    }

    int getEventId() {
        return eventId;
    }
}
