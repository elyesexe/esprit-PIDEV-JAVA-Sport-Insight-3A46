package tn.esprit.entities;

public class Equipe {
    private Integer id;
    private String nom;
    private String coach;
    private String adresse;
    private String telephone;
    private String email;
    private String image;

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

    // Getters and Setters
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

    @Override
    public String toString() {
        return nom; // For ComboBox display
    }
}
