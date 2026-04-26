package tn.esprit.services;

import tn.esprit.services.PerformanceAnalyticsService;

public class MealPlanGenerator {
    
    public static String generatePlan(String mealType, int totalCalories, PerformanceAnalyticsService.PerformanceStats stats, String playerName) {
        StringBuilder plan = new StringBuilder();
        int mealCalories = 0;
        
        switch(mealType) {
            case "breakfast":
                mealCalories = (int)(totalCalories * 0.25);
                plan.append(String.format("🌅 PETIT-DÉJEUNER (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n", playerName));
                plan.append(String.format("📊 Calories totales/jour: %d kcal\n\n", totalCalories));
                plan.append("⏰ TIMING: 7h-8h, 2-3h avant entraînement\n");
                plan.append("🎯 OBJECTIF: Énergie pour la journée\n\n");
                plan.append("🥣 OPTION 1: Classique Énergétique\n");
                plan.append("• 80g flocons d'avoine\n");
                plan.append("• 250ml lait demi-écrémé\n");
                plan.append("• 1 banane\n");
                plan.append("• 15g amandes\n");
                plan.append("• 1 cuillère miel\n");
                plan.append("• 2 œufs brouillés\n\n");
                plan.append("🥞 OPTION 2: Pancakes Protéinés\n");
                plan.append("• 3 pancakes (avoine + banane + œufs)\n");
                plan.append("• 20g beurre de cacahuète\n");
                plan.append("• Fruits rouges\n");
                plan.append("• Yaourt grec 150g\n\n");
                plan.append("🍳 OPTION 3: Continental Sportif\n");
                plan.append("• 2 tranches pain complet\n");
                plan.append("• 2 œufs pochés\n");
                plan.append("• Avocat 1/2\n");
                plan.append("• Tomates cerises\n");
                plan.append("• Jus d'orange frais\n\n");
                plan.append("💡 HYDRATATION: 500ml eau + thé vert\n");
                break;
                
            case "preworkout":
                mealCalories = (int)(totalCalories * 0.10);
                plan.append(String.format("⚡ PRÉ-ENTRAÎNEMENT (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("⏰ TIMING: 30-45 min avant entraînement\n");
                plan.append("🎯 OBJECTIF: Boost d'énergie rapide\n\n");
                plan.append("🍌 OPTION 1: Rapide\n");
                plan.append("• 1 banane mûre\n");
                plan.append("• 10 amandes\n");
                plan.append("• 1 carré chocolat noir\n\n");
                plan.append("🥤 OPTION 2: Liquide\n");
                plan.append("• Smoothie: banane + dattes + lait amande\n");
                plan.append("• 250ml\n\n");
                plan.append("🍎 OPTION 3: Léger\n");
                plan.append("• 1 pomme\n");
                plan.append("• 15g beurre d'amande\n\n");
                plan.append("💡 CONSEIL: Évitez fibres et graisses lourdes\n");
                break;
                
            case "postworkout":
                mealCalories = (int)(totalCalories * 0.15);
                plan.append(String.format("💪 POST-ENTRAÎNEMENT (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("⏰ TIMING: Dans les 30 min après effort\n");
                plan.append("🎯 OBJECTIF: Récupération musculaire optimale\n\n");
                plan.append("⚡ IMMÉDIAT (dans les 30 min):\n");
                plan.append("• Shaker protéines: 30g whey\n");
                plan.append("• 1 banane\n");
                plan.append("• 250ml eau de coco\n\n");
                plan.append("🍽️ REPAS COMPLET (1h après):\n");
                plan.append("• 150g poulet grillé\n");
                plan.append("• 200g riz basmati\n");
                plan.append("• Légumes vapeur\n");
                plan.append("• 1 cuillère huile olive\n\n");
                plan.append("🥗 OPTION VÉGÉTARIENNE:\n");
                plan.append("• 150g tofu\n");
                plan.append("• 200g quinoa\n");
                plan.append("• Légumes variés\n");
                plan.append("• Avocat 1/2\n\n");
                plan.append("💡 RATIO: 3:1 (Glucides:Protéines)\n");
                plan.append("💡 Fenêtre anabolique: 30-60 min\n");
                break;
                
            case "lunch":
                mealCalories = (int)(totalCalories * 0.30);
                plan.append(String.format("🍽️ DÉJEUNER (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("⏰ TIMING: 12h-13h\n");
                plan.append("🎯 OBJECTIF: Repas principal équilibré\n\n");
                plan.append("🥩 OPTION 1: Viande Rouge\n");
                plan.append("• 180g steak haché 5%\n");
                plan.append("• 250g patates douces\n");
                plan.append("• Salade verte\n");
                plan.append("• Vinaigrette légère\n");
                plan.append("• 1 fruit\n\n");
                plan.append("🐟 OPTION 2: Poisson\n");
                plan.append("• 200g saumon grillé\n");
                plan.append("• 200g riz complet\n");
                plan.append("• Brocolis vapeur\n");
                plan.append("• 1 cuillère huile olive\n");
                plan.append("• Yaourt nature\n\n");
                plan.append("🍝 OPTION 3: Pâtes Complètes\n");
                plan.append("• 150g pâtes complètes\n");
                plan.append("• 150g poulet\n");
                plan.append("• Sauce tomate maison\n");
                plan.append("• Légumes grillés\n");
                plan.append("• Parmesan 20g\n\n");
                plan.append("💡 TOUJOURS INCLURE:\n");
                plan.append("• Protéines: 40g\n");
                plan.append("• Glucides complexes: 80g\n");
                plan.append("• Légumes: 200g\n");
                plan.append("• Bonnes graisses: 15g\n\n");
                plan.append("💡 Mastiquez lentement (20 min minimum)\n");
                break;
                
            case "snack":
                mealCalories = (int)(totalCalories * 0.10);
                plan.append(String.format("🍎 COLLATION (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("⏰ TIMING: 16h-17h\n");
                plan.append("🎯 OBJECTIF: Maintenir l'énergie\n\n");
                plan.append("🥜 OPTION 1: Mix Énergétique\n");
                plan.append("• 30g noix mélangées\n");
                plan.append("• 2 dattes\n");
                plan.append("• 1 pomme\n\n");
                plan.append("🥛 OPTION 2: Protéinée\n");
                plan.append("• Yaourt grec 150g\n");
                plan.append("• 15g miel\n");
                plan.append("• Fruits rouges\n");
                plan.append("• Granola 20g\n\n");
                plan.append("🍫 OPTION 3: Gourmande\n");
                plan.append("• 2 carrés chocolat noir 85%\n");
                plan.append("• 1 banane\n");
                plan.append("• 10 amandes\n\n");
                plan.append("🥤 BOISSON:\n");
                plan.append("• Thé vert\n");
                plan.append("• Eau citronnée\n\n");
                plan.append("💡 Évitez sucres raffinés\n");
                break;
                
            case "dinner":
                mealCalories = (int)(totalCalories * 0.25);
                plan.append(String.format("🌙 DÎNER (%d kcal)\n\n", mealCalories));
                plan.append(String.format("👤 Joueur: %s\n\n", playerName));
                plan.append("⏰ TIMING: 19h-20h, 3h avant coucher\n");
                plan.append("🎯 OBJECTIF: Récupération nocturne\n\n");
                plan.append("🍗 OPTION 1: Volaille\n");
                plan.append("• 180g poulet rôti\n");
                plan.append("• 150g quinoa\n");
                plan.append("• Légumes rôtis variés\n");
                plan.append("• Salade verte\n\n");
                plan.append("🐟 OPTION 2: Poisson Blanc\n");
                plan.append("• 200g cabillaud vapeur\n");
                plan.append("• 200g légumes vapeur\n");
                plan.append("• 100g riz basmati\n");
                plan.append("• Sauce citron\n\n");
                plan.append("🥚 OPTION 3: Omelette\n");
                plan.append("• 3 œufs entiers\n");
                plan.append("• Légumes sautés\n");
                plan.append("• 2 tranches pain complet\n");
                plan.append("• Salade composée\n\n");
                plan.append("💡 PRINCIPES:\n");
                plan.append("• Protéines: 35g\n");
                plan.append("• Glucides: 50g (moins que midi)\n");
                plan.append("• Légumes: 300g\n");
                plan.append("• Digestion facile\n\n");
                plan.append("💡 Évitez graisses lourdes le soir\n");
                break;
        }
        
        return plan.toString();
    }
}
