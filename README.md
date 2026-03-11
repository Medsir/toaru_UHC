
# ⚡ Project Academy City : UHC Toaru

Bienvenue sur le dépôt officiel du projet **UHC Toaru**. Ce projet vise à recréer l'univers de *A Certain Magical Index / Scientific Railgun* dans Minecraft via un serveur Spigot ultra-compétitif et fidèle à l'anime.

## 📌 Concept

Un mode de jeu **UHC (Ultra Hardcore)** où chaque joueur incarne un personnage de l'œuvre (Esper ou Magicien).

* **Objectif :** Être le dernier survivant dans la Cité Scolaire (ou une map générée).
* **Gameplay :** Systèmes d'AIM (Espers), Mana (Magiciens), et objets 3D personnalisés.

---

## 🏗️ Structure du Dépôt

* `/plugin` : Code source Java (Bukkit/Spigot API).
* `/resourcepack` : Textures, modèles JSON (Custom Model Data) et sons.
* `/docs` : Documentation des rôles et équilibrage des pouvoirs.

---

## 🛠️ Stack Technique

* **Version :** Minecraft 1.20.1 (ou version choisie).
* **Moteur :** [Spigot / Paper API](https://papermc.io/software/paper).
* **Build Tool :** Maven (ou Gradle).
* **Assets :** Blockbench pour les modèles 3D.

---

## 🚀 Roadmap (Développement Incrémental)

### Phase 1 : Infrastructure (Core) 🔴

* [ ] Setup du système de phases (Attente, Minage, PvP, Finale).
* [ ] Gestion des bordures du monde (WorldBorder).
* [ ] Système de distribution automatique des rôles.

### Phase 2 : Système de Pouvoirs 🔵

* [ ] Création de la classe abstraite `Power`.
* [ ] Gestion de la barre d'AIM (Espers) et du Mana (Magiciens).
* [ ] Implémentation du **Imagine Breaker** (Annulation d'effets).

### Phase 3 : Immersion (Resource Pack) 🟢

* [ ] Intégration des modèles 3D (Pièce de monnaie de Mikoto, Sabre de Kanzaki).
* [ ] Customisation de l'interface (HUD).
* [ ] Sound design (SFX originaux de l'anime).

---

## 👥 L'Équipe

* **[Nom Ami A]** : Lead Developer (Core Engine & Git).
* **[Nom Ami B]** : Gameplay Dev (Rôles & Capacités).
* **[Nom Ami C]** : Asset Designer (Texture Pack & Équilibrage).

---

## 📝 Installation pour le Dev

1. Cloner le repo : `git clone https://github.com/votre-user/uhc-toaru.git`
2. Importer le dossier `/plugin` dans votre IDE (IntelliJ recommandé).
3. Installer les dépendances via Maven.
4. Compiler : `mvn clean package`.

---

> **Note :** "Celui qui possède l'illusion sera celui qui gagnera." — *Imagine Breaker.*

---
