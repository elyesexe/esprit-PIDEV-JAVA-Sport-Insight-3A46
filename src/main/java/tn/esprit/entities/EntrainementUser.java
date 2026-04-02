package tn.esprit.entities;

public class EntrainementUser {
    private Integer entrainementId;
    private Integer userId;

    public EntrainementUser() {
    }

    public EntrainementUser(Integer entrainementId, Integer userId) {
        this.entrainementId = entrainementId;
        this.userId = userId;
    }
}
