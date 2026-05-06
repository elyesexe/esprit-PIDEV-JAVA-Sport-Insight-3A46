package tn.esprit.entities;

import java.time.LocalDateTime;

public class Notification {
<<<<<<< HEAD
    public static final String TYPE_STORE_CART = "store_cart";
    public static final String TYPE_PAYMENT_SUCCESS = "payment_success";
    public static final String TYPE_PAYMENT_FAILED = "payment_failed";
    public static final String TYPE_INVOICE_READY = "invoice_ready";
    public static final String TYPE_ORDER_CREATED = "order_created";
    public static final String TYPE_ORDER_UPDATED = "order_updated";
    public static final String TYPE_ORDER_DELETED = "order_deleted";
    public static final String TYPE_ORDER_EMAIL = "order_email";
    public static final String TYPE_ORDER_EXPORT = "order_export";

    private Integer id;
    private String title;
    private String message;
    private String type;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Integer userId;
    private Integer matchId;
    private String dedupeKey;
    private String competitionCode;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamLogo;
    private String awayTeamLogo;
    private String actorName;
    private String actorImage;
    private String minuteLabel;
    private String accentTone;
=======
    private Integer id;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Integer userId;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    public Notification() {
    }

    public Notification(String message, LocalDateTime createdAt, boolean isRead, Integer userId) {
<<<<<<< HEAD
        this(null, message, null, createdAt, isRead, userId, null, null, null, null, null, null, null, null, null, null);
    }

    public Notification(
            String title,
            String message,
            String type,
            LocalDateTime createdAt,
            boolean isRead,
            Integer userId,
            Integer matchId,
            String dedupeKey,
            String competitionCode,
            String homeTeamName,
            String awayTeamName,
            String homeTeamLogo,
            String awayTeamLogo,
            String actorName,
            String minuteLabel,
            String accentTone
    ) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.userId = userId;
        this.matchId = matchId;
        this.dedupeKey = dedupeKey;
        this.competitionCode = competitionCode;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.homeTeamLogo = homeTeamLogo;
        this.awayTeamLogo = awayTeamLogo;
        this.actorName = actorName;
        this.minuteLabel = minuteLabel;
        this.accentTone = accentTone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public String getHomeTeamLogo() {
        return homeTeamLogo;
    }

    public void setHomeTeamLogo(String homeTeamLogo) {
        this.homeTeamLogo = homeTeamLogo;
    }

    public String getAwayTeamLogo() {
        return awayTeamLogo;
    }

    public void setAwayTeamLogo(String awayTeamLogo) {
        this.awayTeamLogo = awayTeamLogo;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorImage() {
        return actorImage;
    }

    public void setActorImage(String actorImage) {
        this.actorImage = actorImage;
    }

    public String getMinuteLabel() {
        return minuteLabel;
    }

    public void setMinuteLabel(String minuteLabel) {
        this.minuteLabel = minuteLabel;
    }

    public String getAccentTone() {
        return accentTone;
    }

    public void setAccentTone(String accentTone) {
        this.accentTone = accentTone;
    }

    public boolean isStoreCartType() {
        return type != null && TYPE_STORE_CART.equalsIgnoreCase(type.trim());
    }

    public boolean isStoreWorkflowType() {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        return TYPE_STORE_CART.equals(normalized)
                || TYPE_PAYMENT_SUCCESS.equals(normalized)
                || TYPE_PAYMENT_FAILED.equals(normalized)
                || TYPE_INVOICE_READY.equals(normalized);
    }

    public boolean isOrderWorkflowType() {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        return TYPE_ORDER_CREATED.equals(normalized)
                || TYPE_ORDER_UPDATED.equals(normalized)
                || TYPE_ORDER_DELETED.equals(normalized)
                || TYPE_ORDER_EMAIL.equals(normalized)
                || TYPE_ORDER_EXPORT.equals(normalized);
    }

    public boolean isWorkflowType() {
        return isStoreWorkflowType() || isOrderWorkflowType();
    }

    public boolean opensStorePayment() {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase();
        return TYPE_STORE_CART.equals(normalized) || TYPE_PAYMENT_FAILED.equals(normalized);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", createdAt=" + createdAt +
                ", isRead=" + isRead +
                ", userId=" + userId +
                ", matchId=" + matchId +
                ", dedupeKey='" + dedupeKey + '\'' +
                ", competitionCode='" + competitionCode + '\'' +
                ", homeTeamName='" + homeTeamName + '\'' +
                ", awayTeamName='" + awayTeamName + '\'' +
                ", actorName='" + actorName + '\'' +
                ", actorImage='" + actorImage + '\'' +
                ", minuteLabel='" + minuteLabel + '\'' +
                ", accentTone='" + accentTone + '\'' +
                '}';
=======
        this.message = message;
        this.createdAt = createdAt;
        this.isRead = isRead;
        this.userId = userId;
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }
}
