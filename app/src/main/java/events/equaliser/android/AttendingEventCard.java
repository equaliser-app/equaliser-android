package events.equaliser.android;

/**
 * Stores data about an event to be displayed in a CardView.
 */
class AttendingEventCard {

    enum Status {
        WAITING_TICKETS("In waiting list"),
        WAITING_PAYMENT("Waiting for payment"),
        WAITING_PAYEE("Waiting for payment from %s"),
        COMPLETE("Order complete"),
        EXPIRED("Offer expired");

        private final String text;

        Status(String text) {
            this.text = text;
        }

        String getText() {
            return text;
        }
    }

    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final Status status;
    private final String payee;

    AttendingEventCard(String title, String subtitle, String imageUrl, Status status, String payee) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.subtitle = subtitle;
        this.status = status;
        this.payee = payee;
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

    Status getStatus() {
        return status;
    }

    String getPayee() {
        return payee;
    }
}
