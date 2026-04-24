package tn.esprit.services.football;

public record YouTubeVideo(
        String videoId,
        String title,
        String channelTitle
) {
    public YouTubeVideo {
        videoId = sanitize(videoId);
        title = sanitize(title);
        channelTitle = sanitize(channelTitle);
    }

    public String getEmbedUrl() {
        return "https://www.youtube.com/embed/" + videoId
                + "?autoplay=1&rel=0&modestbranding=1&playsinline=1&origin=https://www.youtube.com";
    }

    public String getWatchUrl() {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public String getInAppWatchUrl() {
        return getWatchUrl() + "&autoplay=1";
    }

    @Override
    public String toString() {
        if (channelTitle == null) {
            return title == null ? videoId : title;
        }
        return (title == null ? videoId : title) + " - " + channelTitle;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
