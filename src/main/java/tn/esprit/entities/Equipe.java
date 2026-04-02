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
}
