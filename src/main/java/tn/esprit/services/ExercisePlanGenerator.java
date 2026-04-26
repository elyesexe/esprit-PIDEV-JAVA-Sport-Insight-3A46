package tn.esprit.services;

import tn.esprit.services.PerformanceAnalyticsService;

public class ExercisePlanGenerator {
    
    public static String generatePlan(String exerciseType, PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        boolean highPriority = false;
        StringBuilder plan = new StringBuilder();
        
        switch(exerciseType) {
            case "cardio":
                highPriority = stats.avgPhysique() < 14;
                plan.append(String.format("🏃 PROGRAMME CARDIO %s\n\n", highPriority ? "INTENSIF" : "MAINTIEN"));
                plan.append(String.format("👤 Joueur: %s\n", playerName));
                plan.append(String.format("📊 Niveau Physique: %.1f/20\n\n", stats.avgPhysique()));
                
                if (highPriority) {
                    plan.append("⚠️ PRIORITÉ HAUTE - Amélioration cardio nécessaire\n\n");
                    plan.append("📅 SEMAINE 1-2: Base Aérobie\n");
                    plan.append("• Lundi: Course continue 30 min (70% FCmax)\n");
                    plan.append("• Mercredi: HIIT 20 min (30s sprint / 90s repos)\n");
                    plan.append("• Vendredi: Fartlek 25 min (variations de rythme)\n");
                    plan.append("• Samedi: Course longue 40 min (65% FCmax)\n\n");
                    plan.append("📅 SEMAINE 3-4: Intensification\n");
                    plan.append("• Lundi: Course continue 35 min (75% FCmax)\n");
                    plan.append("• Mercredi: HIIT 25 min (40s sprint / 80s repos)\n");
                    plan.append("• Vendredi: Intervalles 30 min (2 min rapide / 2 min lent)\n");
                    plan.append("• Samedi: Course longue 45 min (70% FCmax)\n\n");
                    plan.append("🎯 OBJECTIFS:\n");
                    plan.append("• Améliorer VO2max de 10%\n");
                    plan.append("• Réduire temps au 1000m de 30 secondes\n");
                    plan.append("• Augmenter endurance générale\n\n");
                } else {
                    plan.append("✅ Niveau correct - Programme de maintien\n\n");
                    plan.append("📅 Programme Hebdomadaire:\n");
                    plan.append("• 2x Course continue 25 min (70% FCmax)\n");
                    plan.append("• 1x HIIT 15 min (30s sprint / 90s repos)\n");
                    plan.append("• 1x Récupération active (vélo, natation)\n\n");
                }
                plan.append("💡 CONSEILS:\n");
                plan.append("• Échauffement 10 min obligatoire\n");
                plan.append("• Hydratation: 500ml avant, 250ml toutes les 15 min\n");
                plan.append("• Récupération active après chaque séance\n");
                break;
                
            case "strength":
                highPriority = stats.avgPhysique() < 14;
                plan.append(String.format("💪 PROGRAMME MUSCULATION %s\n\n", highPriority ? "INTENSIF" : "MAINTIEN"));
                plan.append(String.format("👤 Joueur: %s\n", playerName));
                plan.append(String.format("📊 Niveau Physique: %.1f/20\n\n", stats.avgPhysique()));
                
                if (highPriority) {
                    plan.append("⚠️ PRIORITÉ HAUTE - Renforcement musculaire requis\n\n");
                    plan.append("📅 LUNDI & JEUDI: Bas du Corps\n");
                    plan.append("• Squats: 4x10 (70% 1RM)\n");
                    plan.append("• Fentes alternées: 3x12 par jambe\n");
                    plan.append("• Leg Press: 3x15\n");
                    plan.append("• Mollets debout: 4x20\n");
                    plan.append("• Ischio-jambiers: 3x12\n\n");
                    plan.append("📅 MARDI & VENDREDI: Haut du Corps + Core\n");
                    plan.append("• Développé couché: 4x10\n");
                    plan.append("• Tractions: 3x max\n");
                    plan.append("• Rowing: 3x12\n");
                    plan.append("• Gainage: 4x60s\n");
                    plan.append("• Russian twists: 3x30\n\n");
                    plan.append("🎯 OBJECTIFS:\n");
                    plan.append("• +15% force explosive\n");
                    plan.append("• +20% puissance de frappe\n");
                    plan.append("• Meilleure protection contre blessures\n\n");
                } else {
                    plan.append("✅ Niveau correct - Programme de maintien\n\n");
                    plan.append("📅 2x par semaine:\n");
                    plan.append("• Circuit training 30 min\n");
                    plan.append("• Exercices au poids du corps\n");
                    plan.append("• Focus: Force fonctionnelle\n\n");
                }
                plan.append("💡 PROGRESSION:\n");
                plan.append("• Semaine 1-2: 70% 1RM\n");
                plan.append("• Semaine 3-4: 75% 1RM\n");
                plan.append("• Repos 90s entre séries\n");
                break;
                
            case "technical":
                highPriority = stats.avgTechnique() < 14;
                plan.append(String.format("⚽ PROGRAMME TECHNIQUE %s\n\n", highPriority ? "INTENSIF" : "MAINTIEN"));
                plan.append(String.format("👤 Joueur: %s\n", playerName));
                plan.append(String.format("📊 Niveau Technique: %.1f/20\n\n", stats.avgTechnique()));
                
                if (highPriority) {
                    plan.append("⚠️ PRIORITÉ HAUTE - Travail technique essentiel\n\n");
                    plan.append("📅 LUNDI: Contrôle & Passes\n");
                    plan.append("• Contrôle orienté: 100 répétitions\n");
                    plan.append("• Passes courtes: 200 répétitions (90% précision)\n");
                    plan.append("• Passes longues: 50 répétitions (75% précision)\n");
                    plan.append("• Jeu à 2 touches: 20 min\n\n");
                    plan.append("📅 MERCREDI: Dribbles & Conduite\n");
                    plan.append("• Slalom: 10x parcours\n");
                    plan.append("• Changements de direction: 50 répétitions\n");
                    plan.append("• Feintes: 30 répétitions par type\n");
                    plan.append("• 1v1: 15 min\n\n");
                    plan.append("📅 VENDREDI: Frappes & Finition\n");
                    plan.append("• Frappes de volée: 30 répétitions\n");
                    plan.append("• Frappes enroulées: 30 répétitions\n");
                    plan.append("• Têtes: 20 répétitions\n");
                    plan.append("• Finitions dans surface: 40 répétitions\n\n");
                    plan.append("🎯 OBJECTIFS:\n");
                    plan.append("• Précision passes: 75% → 90%\n");
                    plan.append("• Réussite dribbles: +30%\n");
                    plan.append("• Efficacité devant but: +25%\n\n");
                } else {
                    plan.append("✅ Niveau correct - Programme de maintien\n\n");
                    plan.append("📅 2x par semaine:\n");
                    plan.append("• Jonglage: 10 min\n");
                    plan.append("• Passes: 100 répétitions\n");
                    plan.append("• Frappes: 20 répétitions\n\n");
                }
                plan.append("💡 CONSEIL:\n");
                plan.append("• Filmez-vous pour analyser vos gestes\n");
                break;
                
            case "tactical":
                highPriority = stats.avgTactique() < 14;
                plan.append(String.format("🎯 PROGRAMME TACTIQUE %s\n\n", highPriority ? "INTENSIF" : "MAINTIEN"));
                plan.append(String.format("👤 Joueur: %s\n", playerName));
                plan.append(String.format("📊 Niveau Tactique: %.1f/20\n\n", stats.avgTactique()));
                
                if (highPriority) {
                    plan.append("⚠️ PRIORITÉ HAUTE - Compréhension du jeu à développer\n\n");
                    plan.append("📅 MARDI: Positionnement Défensif\n");
                    plan.append("• Analyse vidéo: 30 min\n");
                    plan.append("• Exercices de placement: 45 min\n");
                    plan.append("• Jeux réduits 5v5: 30 min\n");
                    plan.append("• Débriefing: 15 min\n\n");
                    plan.append("📅 JEUDI: Jeu Offensif\n");
                    plan.append("• Mouvements sans ballon: 30 min\n");
                    plan.append("• Appels de balle: 20 min\n");
                    plan.append("• Combinaisons: 40 min\n");
                    plan.append("• Match 8v8: 30 min\n\n");
                    plan.append("📅 SAMEDI: Transitions\n");
                    plan.append("• Récupération → Attaque: 30 min\n");
                    plan.append("• Perte de balle → Repli: 30 min\n");
                    plan.append("• Match 11v11: 60 min\n\n");
                    plan.append("🎯 OBJECTIFS:\n");
                    plan.append("• Meilleure lecture du jeu\n");
                    plan.append("• Anticipation +40%\n");
                    plan.append("• Décisions plus rapides\n\n");
                } else {
                    plan.append("✅ Niveau correct - Programme de maintien\n\n");
                    plan.append("📅 1x par semaine:\n");
                    plan.append("• Analyse vidéo: 20 min\n");
                    plan.append("• Jeux réduits: 30 min\n");
                    plan.append("• Discussion tactique: 10 min\n\n");
                }
                plan.append("💡 CONSEIL:\n");
                plan.append("• Étudiez les matchs professionnels\n");
                break;
                
            case "recovery":
                plan.append("🧘 PROGRAMME RÉCUPÉRATION & MOBILITÉ\n\n");
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("💡 Essentiel pour prévenir les blessures\n\n");
                plan.append("📅 QUOTIDIEN (15 min):\n");
                plan.append("• Étirements dynamiques matin\n");
                plan.append("• Foam rolling soir\n");
                plan.append("• Respiration profonde\n\n");
                plan.append("📅 POST-ENTRAÎNEMENT (20 min):\n");
                plan.append("• Étirements statiques\n");
                plan.append("• Automassages\n");
                plan.append("• Bain froid (optionnel)\n\n");
                plan.append("📅 JOUR DE REPOS (45 min):\n");
                plan.append("• Yoga football-specific\n");
                plan.append("• Mobilité articulaire\n");
                plan.append("• Méditation 10 min\n\n");
                plan.append("🎯 OBJECTIFS:\n");
                plan.append("• Prévenir blessures\n");
                plan.append("• Améliorer flexibilité\n");
                plan.append("• Optimiser récupération\n\n");
                plan.append("💡 ESSENTIELS:\n");
                plan.append("• Sommeil: 8-9h par nuit\n");
                plan.append("• Hydratation: 3L par jour\n");
                plan.append("• Massage professionnel: 1x/mois\n");
                break;
                
            case "match":
                plan.append("🏆 MATCHS SIMULÉS & COMPÉTITION\n\n");
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("🎯 Application de vos entraînements\n\n");
                plan.append("📅 MERCREDI: Match Réduit\n");
                plan.append("• Format: 7v7 ou 8v8\n");
                plan.append("• Durée: 2x30 min\n");
                plan.append("• Focus: Application tactique\n");
                plan.append("• Intensité: 85%\n\n");
                plan.append("📅 SAMEDI: Match 11v11\n");
                plan.append("• Format: Match complet\n");
                plan.append("• Durée: 2x45 min\n");
                plan.append("• Conditions réelles\n");
                plan.append("• Intensité: 100%\n\n");
                plan.append("🎯 OBJECTIFS:\n");
                plan.append("• Appliquer entraînements\n");
                plan.append("• Tester nouvelles compétences\n");
                plan.append("• Développer mentalité compétitive\n\n");
                plan.append("💡 PRÉPARATION:\n");
                plan.append("• Échauffement complet 30 min\n");
                plan.append("• Visualisation mentale\n");
                plan.append("• Nutrition pré-match optimale\n\n");
                plan.append("📊 POST-MATCH:\n");
                plan.append("• Auto-évaluation\n");
                plan.append("• Analyse vidéo\n");
                plan.append("• Points à améliorer\n");
                break;
        }
        
        return plan.toString();
    }
}
