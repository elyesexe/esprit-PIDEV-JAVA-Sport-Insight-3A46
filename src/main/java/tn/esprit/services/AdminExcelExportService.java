package tn.esprit.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Joueur;
import tn.esprit.entities.Matchs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class AdminExcelExportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    public void exportEquipes(Path targetPath, List<Equipe> equipes, Function<Equipe, String> competitionResolver) throws IOException {
        try (XSSFWorkbook workbook = createWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Equipes");
            int rowIndex = 0;
            rowIndex = writeHeader(sheet, rowIndex, workbook, "ID", "Equipe", "Coach", "Competition", "Source", "Logo");
            for (Equipe equipe : equipes) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row,
                        valueOf(equipe.getId()),
                        text(equipe.getNom()),
                        text(equipe.getCoach()),
                        competitionResolver == null ? "-" : text(competitionResolver.apply(equipe)),
                        text(equipe.getExternalSource()),
                        text(equipe.getImage())
                );
            }
            autosize(sheet, 6);
            saveWorkbook(workbook, targetPath);
        }
    }

    public void exportJoueurs(Path targetPath, List<Joueur> joueurs, Function<Integer, String> equipeNameResolver) throws IOException {
        try (XSSFWorkbook workbook = createWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Joueurs");
            int rowIndex = 0;
            rowIndex = writeHeader(sheet, rowIndex, workbook,
                    "ID", "Nom", "Prenom", "Equipe", "Numero", "Naissance", "Position", "Nationalite", "Image");
            for (Joueur joueur : joueurs) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row,
                        valueOf(joueur.getId()),
                        text(joueur.getNom()),
                        text(joueur.getPrenom()),
                        equipeNameResolver == null ? "-" : text(equipeNameResolver.apply(joueur.getEquipeId())),
                        joueur.getNumero() > 0 ? String.valueOf(joueur.getNumero()) : "-",
                        formatDate(joueur.getDateNaissance()),
                        text(joueur.getPosition()),
                        text(joueur.getNationalite()),
                        text(joueur.getImage())
                );
            }
            autosize(sheet, 9);
            saveWorkbook(workbook, targetPath);
        }
    }

    public void exportMatchs(
            Path targetPath,
            List<Matchs> matchs,
            Function<Integer, String> equipeNameResolver,
            Function<String, String> competitionResolver
    ) throws IOException {
        try (XSSFWorkbook workbook = createWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Matchs");
            int rowIndex = 0;
            rowIndex = writeHeader(sheet, rowIndex, workbook,
                    "ID", "Reference", "Date", "Heure", "Domicile", "Exterieur", "Score", "Statut", "Competition", "Lieu", "Type");
            for (Matchs match : matchs) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row,
                        valueOf(match.getId()),
                        text(match.getIdMatch()),
                        formatDate(match.getDateMatch()),
                        formatTime(match.getHeureDebut()),
                        equipeNameResolver == null ? "-" : text(equipeNameResolver.apply(match.getEquipeDomicileId())),
                        equipeNameResolver == null ? "-" : text(equipeNameResolver.apply(match.getEquipeExterieurId())),
                        buildScore(match),
                        text(match.getStatut()),
                        competitionResolver == null ? "-" : text(competitionResolver.apply(match.getCompetitionCode())),
                        text(match.getLieu()),
                        text(match.getType())
                );
            }
            autosize(sheet, 11);
            saveWorkbook(workbook, targetPath);
        }
    }

    private XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    private int writeHeader(XSSFSheet sheet, int rowIndex, XSSFWorkbook workbook, String... headers) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return rowIndex;
    }

    private void writeRow(Row row, String... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void autosize(XSSFSheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void saveWorkbook(XSSFWorkbook workbook, Path targetPath) throws IOException {
        if (targetPath == null) {
            throw new IOException("No destination file was selected.");
        }
        Path parent = targetPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
            workbook.write(outputStream);
        }
    }

    private String buildScore(Matchs match) {
        if (match == null || match.getScoreEquipeDomicile() == null || match.getScoreEquipeExterieur() == null) {
            return "-";
        }
        return match.getScoreEquipeDomicile() + " - " + match.getScoreEquipeExterieur();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : TIME_FORMATTER.format(time);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String valueOf(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
