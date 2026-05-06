package tn.esprit.entities;

public class Equipe {
    private Integer id;
    private String nom;
    private String coach;
    private String adresse;
    private String telephone;
    private String email;
    private String image;
    private Long externalApiId;
    private String externalSource;
    private String competitionCode;
<<<<<<< HEAD
    private Long apiFootballId;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0

    public Equipe() {
    }

    public Equipe(String nom, String coach, String adresse, String telephone, String email, String image) {
        this.nom = nom;
        this.coach = coach;
        this.adresse = adresse;
        this.telephone = telephone;
        this.email = email;
        this.image = image;
    }

    public Equipe(Integer id, String nom, String coach, String adresse, String telephone, String email, String image) {
        this.id = id;
        this.nom = nom;
        this.coach = coach;
        this.adresse = adresse;
        this.telephone = telephone;
        this.email = email;
        this.image = image;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getExternalApiId() {
        return externalApiId;
    }

    public void setExternalApiId(Long externalApiId) {
        this.externalApiId = externalApiId;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }

    public void setCompetitionCode(String competitionCode) {
        this.competitionCode = competitionCode;
    }

<<<<<<< HEAD
    public Long getApiFootballId() {
        return apiFootballId;
    }

    public void setApiFootballId(Long apiFootballId) {
        this.apiFootballId = apiFootballId;
    }

=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    @Override
    public String toString() {
        return "Equipe{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", coach='" + coach + '\'' +
                ", adresse='" + adresse + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", image='" + image + '\'' +
                ", externalApiId=" + externalApiId +
                ", externalSource='" + externalSource + '\'' +
                ", competitionCode='" + competitionCode + '\'' +
<<<<<<< HEAD
                ", apiFootballId=" + apiFootballId +
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
                '}';
    }
}
