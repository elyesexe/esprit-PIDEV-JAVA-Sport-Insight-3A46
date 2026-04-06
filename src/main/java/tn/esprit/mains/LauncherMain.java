package tn.esprit.mains;

import java.util.Scanner;

public class LauncherMain {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            System.out.println("\n========================================");
            System.out.println("      WELCOME TO SPORT INSIGHT");
            System.out.println("========================================");
            System.out.println("1. Match module");
            System.out.println("2. Product module");
            System.out.println("3. Entrainement module");
            System.out.println("4. Annonce module");
            System.out.println("5. Sponsor module");
            System.out.println("6. User module");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            int choice = parseInt(SCANNER.nextLine());
            try {
                switch (choice) {
                    case 1 -> MatchMain.main(new String[0]);
                    case 2 -> ProductMain.main(new String[0]);
                    case 3 -> EntrainementMain.main(new String[0]);
                    case 4 -> AnnonceMain.main(new String[0]);
                    case 5 -> SponsorMain.main(new String[0]);
                    case 6 -> UserMain.main(new String[0]);
                    case 0 -> {
                        System.out.println("Application closed.");
                        running = false;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Module failed: " + e.getMessage());
            }
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
