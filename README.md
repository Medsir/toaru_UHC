---

# ⚡ Project Academy City : UHC Toaru

Bienvenue sur le dépôt officiel du projet **UHC Toaru**. Ce projet vise à recréer l'univers de *A Certain Magical Index / Scientific Railgun* dans Minecraft via un serveur Paper.

## 📌 Concept

Un mode de jeu **UHC (Ultra Hardcore)** où chaque joueur incarne un personnage de l'œuvre (Esper ou Magicien).

* **Objectif :** Être le dernier survivant.
* **Gameplay :** Systèmes d'AIM (Espers), Mana (Magiciens), et objets 3D personnalisés via Resource Pack.

---

## 🏗️ Structure du Projet

Le projet est divisé en deux parties principales :

* `/plugin` : Code source Java (API Paper 1.20.1). Utilise **Maven** pour la gestion des dépendances.
* `/resourcepack` : Modèles JSON (Custom Model Data), textures et sons personnalisés.

---

## 🛠️ Configuration Technique

* **Version Minecraft :** 1.20.1
* **Moteur :** [PaperMC](https://papermc.io/)
* **Java :** JDK 17+
* **Build Tool :** Maven

---

## 🚀 Guide de Démarrage (Développeurs)

### 1. Installation de l'environnement

1. Installez **IntelliJ IDEA** (Community ou Ultimate).
2. Clonez ce dépôt Git.
3. Dans IntelliJ, faites `File > Open` et sélectionnez le fichier **`plugin/pom.xml`**.
4. Cliquez sur **"Open as Project"** et acceptez l'importation Maven.

### 2. Compilation (Générer le .jar)

Pour tester le plugin sur un serveur :

1. Ouvrez l'onglet **Maven** à droite dans IntelliJ.
2. Allez dans `Lifecycle`.
3. Double-cliquez sur **`clean`**, puis sur **`package`**.
4. Le fichier `.jar` généré se trouvera dans le dossier `plugin/target/`.

---

## 📋 Roadmap (État d'avancement)

* [x] Initialisation du projet Maven et API Paper.
* [x] Création de la classe principale (`ToaruMain`).
* [x] Système d'événements pour les pouvoirs (En cours...).
* [x] Création du système de rôles (Academy City List).
* [ ] Modélisation du ressource pack.
* [ ] Ajustements d'équilibrage

---

## 👥 L'Équipe

* **Medsir** : Lead Developer / Designer Resource Pack & Équilibrage.
* **Fryzim** : Gameplay & Powers Developer.
* **Raion & Slyra** : Simple spectators

---

> *"Dans cette ville, le talent ne se mesure pas à ce que l'on possède, mais à ce que l'on est prêt à calculer."*

---
