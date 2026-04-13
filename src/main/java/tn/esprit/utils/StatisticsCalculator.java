package tn.esprit.utils;

import tn.esprit.entities.Sponsor;
import tn.esprit.entities.ContratSponsor;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticsCalculator {

    // Tri des sponsors
    public static List<Sponsor> sortSponsorsByName(List<Sponsor> sponsors) {
        return sponsors.stream()
                .sorted(Comparator.comparing(Sponsor::getNom))
                .collect(Collectors.toList());
    }

    public static List<Sponsor> sortSponsorsByBudget(List<Sponsor> sponsors, boolean ascending) {
        return sponsors.stream()
                .sorted(ascending 
                    ? Comparator.comparingDouble(Sponsor::getBudget)
                    : Comparator.comparingDouble(Sponsor::getBudget).reversed())
                .collect(Collectors.toList());
    }

    // Filtrage des contrats actifs (non expirés)
    public static List<ContratSponsor> filterActiveContrats(List<ContratSponsor> contrats) {
        LocalDate today = LocalDate.now();
        return contrats.stream()
                .filter(c -> c.getDateDebut().isBefore(today) || c.getDateDebut().isEqual(today))
                .filter(c -> c.getDateFin().isAfter(today) || c.getDateFin().isEqual(today))
                .collect(Collectors.toList());
    }

    // Calculer le montant total des contrats
    public static double calculateTotalMontant(List<ContratSponsor> contrats) {
        return contrats.stream()
                .mapToDouble(ContratSponsor::getMontant)
                .sum();
    }

    // Calculer la moyenne des montants
    public static double calculateAverageMontant(List<ContratSponsor> contrats) {
        if (contrats.isEmpty()) return 0;
        return calculateTotalMontant(contrats) / contrats.size();
    }

    // Nombre de contrats actifs
    public static long countActiveContrats(List<ContratSponsor> contrats) {
        return filterActiveContrats(contrats).size();
    }

    // Montant total des sponsors (budget)
    public static double calculateTotalBudget(List<Sponsor> sponsors) {
        return sponsors.stream()
                .mapToDouble(Sponsor::getBudget)
                .sum();
    }

    // Budget moyen
    public static double calculateAverageBudget(List<Sponsor> sponsors) {
        if (sponsors.isEmpty()) return 0;
        return calculateTotalBudget(sponsors) / sponsors.size();
    }

    // Sponsor avec le plus grand budget
    public static Sponsor getTopBudgetSponsor(List<Sponsor> sponsors) {
        return sponsors.stream()
                .max(Comparator.comparingDouble(Sponsor::getBudget))
                .orElse(null);
    }

    // Contrats par statut
    public static long countContratsByStatut(List<ContratSponsor> contrats, String statut) {
        return contrats.stream()
                .filter(c -> c.getStatut().equalsIgnoreCase(statut))
                .count();
    }

    // Contrats expirés
    public static List<ContratSponsor> getExpiredContrats(List<ContratSponsor> contrats) {
        LocalDate today = LocalDate.now();
        return contrats.stream()
                .filter(c -> c.getDateFin().isBefore(today))
                .collect(Collectors.toList());
    }
}
