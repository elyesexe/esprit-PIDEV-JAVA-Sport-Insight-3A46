package tn.esprit.services;

import tn.esprit.entities.ContratSponsor;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Sponsor;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SponsoringWorkspaceService {
    private final SponsorService sponsorService;
    private final ContratSponsorService contratSponsorService;
    private final EquipeService equipeService;

    public SponsoringWorkspaceService() throws SQLException {
        this(new SponsorService(), new ContratSponsorService(), new EquipeService());
    }

    SponsoringWorkspaceService(
            SponsorService sponsorService,
            ContratSponsorService contratSponsorService,
            EquipeService equipeService
    ) {
        this.sponsorService = sponsorService;
        this.contratSponsorService = contratSponsorService;
        this.equipeService = equipeService;
    }

    public SponsoringSnapshot loadSnapshot() throws SQLException {
        List<Sponsor> sponsors = new ArrayList<>(sponsorService.getAll());
        sponsors.sort(Comparator
                .comparing(Sponsor::getBudget)
                .reversed()
                .thenComparing(sponsor -> emptyIfNull(sponsor.getNom()), String.CASE_INSENSITIVE_ORDER));

        List<Equipe> equipes = new ArrayList<>(equipeService.getAll());
        equipes.sort(Comparator.comparing(equipe -> emptyIfNull(equipe.getNom()), String.CASE_INSENSITIVE_ORDER));

        Map<Integer, Sponsor> sponsorsById = sponsors.stream()
                .filter(sponsor -> sponsor.getId() != null)
                .collect(Collectors.toMap(
                        Sponsor::getId,
                        sponsor -> sponsor,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Integer, Equipe> equipesById = equipes.stream()
                .filter(equipe -> equipe.getId() != null)
                .collect(Collectors.toMap(
                        Equipe::getId,
                        equipe -> equipe,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ContratSponsor> contrats = new ArrayList<>(contratSponsorService.getAll());
<<<<<<< HEAD
        List<ContratSponsor> newlyExpiredContracts = refreshExpiredContracts(contrats);
=======
        refreshExpiredContracts(contrats);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        contrats.sort(Comparator
                .comparing(ContratSponsor::getDateDebut, Comparator.nullsLast(LocalDate::compareTo))
                .reversed()
                .thenComparing(ContratSponsor::getId, Comparator.nullsLast(Integer::compareTo)));

        SponsoringStats stats = computeStats(sponsors, contrats);
<<<<<<< HEAD
        return new SponsoringSnapshot(sponsors, contrats, sponsorsById, equipesById, stats, newlyExpiredContracts);
=======
        return new SponsoringSnapshot(sponsors, contrats, sponsorsById, equipesById, stats);
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    public boolean isExpired(ContratSponsor contratSponsor) {
        return contratSponsor != null
                && contratSponsor.getDateFin() != null
                && contratSponsor.getDateFin().isBefore(LocalDate.now());
    }

    public String resolveContractStatus(ContratSponsor contratSponsor) {
        if (contratSponsor == null) {
            return "Unknown";
        }
        if (isExpired(contratSponsor)) {
            return "Expired";
        }
        String rawStatus = emptyIfNull(contratSponsor.getStatut());
        return rawStatus.isBlank() ? "Active" : humanizeStatus(rawStatus);
    }

    public String resolvePaymentStatus(ContratSponsor contratSponsor) {
        if (contratSponsor == null) {
            return "Pending";
        }
        String rawStatus = emptyIfNull(contratSponsor.getStatutPaiement());
        return rawStatus.isBlank() ? "Pending" : humanizeStatus(rawStatus);
    }

    public String humanizeStatus(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] words = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

<<<<<<< HEAD
    private List<ContratSponsor> refreshExpiredContracts(List<ContratSponsor> contrats) throws SQLException {
        List<ContratSponsor> newlyExpiredContracts = new ArrayList<>();
=======
    private void refreshExpiredContracts(List<ContratSponsor> contrats) throws SQLException {
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
        for (ContratSponsor contrat : contrats) {
            if (!isExpired(contrat)) {
                continue;
            }

            boolean dirty = false;
<<<<<<< HEAD
            boolean wasAlreadyNotified = contrat.isNotified();
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            if (!equalsIgnoreCase(contrat.getStatut(), "EXPIRED") && !equalsIgnoreCase(contrat.getStatut(), "EXPIRE")) {
                contrat.setStatut("EXPIRED");
                dirty = true;
            }
            if (!contrat.isNotified()) {
                contrat.setNotified(true);
                dirty = true;
            }
<<<<<<< HEAD
            if (!wasAlreadyNotified) {
                newlyExpiredContracts.add(contrat);
            }
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
            if (dirty) {
                contratSponsorService.update(contrat);
            }
        }
<<<<<<< HEAD
        return newlyExpiredContracts;
=======
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    }

    private SponsoringStats computeStats(List<Sponsor> sponsors, List<ContratSponsor> contrats) {
        int totalSponsors = sponsors.size();
        int totalContrats = contrats.size();
        long activeContracts = contrats.stream().filter(contrat -> !isExpired(contrat)).count();
        long expiredContracts = contrats.stream().filter(this::isExpired).count();
        double totalBudget = sponsors.stream().mapToDouble(Sponsor::getBudget).sum();
        double totalContractAmount = contrats.stream().mapToDouble(ContratSponsor::getMontant).sum();
        double averageContractAmount = totalContrats == 0 ? 0.0 : totalContractAmount / totalContrats;

        Map<String, Long> contractStatusBreakdown = contrats.stream()
                .collect(Collectors.groupingBy(
                        contrat -> resolveContractStatus(contrat),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> paymentBreakdown = contrats.stream()
                .collect(Collectors.groupingBy(
                        contrat -> resolvePaymentStatus(contrat),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<SponsorBudgetPoint> topSponsors = sponsors.stream()
                .sorted(Comparator.comparingDouble(Sponsor::getBudget).reversed())
                .limit(5)
                .map(sponsor -> new SponsorBudgetPoint(
                        defaultSponsorName(sponsor),
                        sponsor.getBudget()
                ))
                .toList();

        return new SponsoringStats(
                totalSponsors,
                totalContrats,
                activeContracts,
                expiredContracts,
                totalBudget,
                totalContractAmount,
                averageContractAmount,
                contractStatusBreakdown,
                paymentBreakdown,
                topSponsors
        );
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return Objects.equals(
                left == null ? null : left.trim().toLowerCase(Locale.ROOT),
                right == null ? null : right.trim().toLowerCase(Locale.ROOT)
        );
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultSponsorName(Sponsor sponsor) {
        String name = sponsor == null ? null : sponsor.getNom();
        return name == null || name.isBlank() ? "Sponsor" : name.trim();
    }

    public record SponsoringSnapshot(
            List<Sponsor> sponsors,
            List<ContratSponsor> contrats,
            Map<Integer, Sponsor> sponsorsById,
            Map<Integer, Equipe> equipesById,
<<<<<<< HEAD
            SponsoringStats stats,
            List<ContratSponsor> newlyExpiredContracts
=======
            SponsoringStats stats
>>>>>>> 37457458daa1c0c7108e6ba4ed1ba88a98cda5f0
    ) {
        public Sponsor sponsorOf(ContratSponsor contrat) {
            if (contrat == null || contrat.getSponsorId() == null) {
                return null;
            }
            return sponsorsById.get(contrat.getSponsorId());
        }

        public Equipe equipeOf(ContratSponsor contrat) {
            if (contrat == null || contrat.getEquipeId() == null) {
                return null;
            }
            return equipesById.get(contrat.getEquipeId());
        }
    }

    public record SponsoringStats(
            int totalSponsors,
            int totalContrats,
            long activeContracts,
            long expiredContracts,
            double totalBudget,
            double totalContractAmount,
            double averageContractAmount,
            Map<String, Long> contractStatusBreakdown,
            Map<String, Long> paymentBreakdown,
            List<SponsorBudgetPoint> topSponsors
    ) {
    }

    public record SponsorBudgetPoint(String label, double value) {
    }
}
