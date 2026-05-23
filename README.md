# SMA - Allocation de Restaurants

## Description

Ce projet réalise une simulation d’un **Système Multi-Agents (SMA)** pour un problème d’allocation de ressources dans le contexte de réservation de restaurants.

Le système modélise :

* des agents personnes cherchant à obtenir une réservation ;
* des agents restaurants gérant les places disponibles ;
* une phase de délibération entre agents avant réservation ;
* un mécanisme de réessai en cas de refus ;
* une interface graphique Swing permettant de configurer et lancer la simulation.

---

# Environnement technique

* **Langage :** Java
* **Plateforme SMA :** JADE
* **Interface graphique :** Swing
* **IDE utilisé :** Eclipse

---

# Structure du projet

```bash
src/   -> fichiers source Java
bin/   -> fichiers compilés .class
libs/  -> bibliothèques nécessaires au projet
```

Autres fichiers :

* `.classpath`
* `.project`
* `APDescription.txt`
* `MTPs-Main-Container.txt`
* `MTPs-SMA-Resto.txt`

---

# Bibliothèques utilisées

* `jade.jar`
* `flatlaf-3.7.1.jar`

---

# Classes principales

| Classe                  | Description                       |
| ----------------------- | --------------------------------- |
| Main.java               | point d’entrée du système SMA     |
| AgentPersonne.java      | agent représentant une personne   |
| AgentRestaurant.java    | agent représentant un restaurant  |
| SimulationUI.java       | interface graphique de simulation |
| SimulationLauncher.java | lancement de la simulation        |
| SimulationConfig.java   | paramètres de simulation          |
| SimulationResult.java   | affichage des résultats           |

---

# Prérequis

* Java JDK installé
* Eclipse IDE
* Bibliothèques du dossier `libs` ajoutées au Build Path

---

# Ouverture du projet dans Eclipse

1. Ouvrir Eclipse
2. Importer le projet comme projet Java existant
3. Vérifier que le dossier `libs` contient :

   * `jade.jar`
   * `flatlaf-3.7.1.jar`
4. Ajouter les bibliothèques au Build Path
5. Vérifier que les dossiers `src` et `bin` sont reconnus

---

# Exécution

## 1. Exécution directe du système multi-agents

Lancer :

```bash
Main.java
```

## 2. Exécution via l’interface graphique

Lancer :

```bash
SimulationUI.java
```

---

# Fonctionnement général

1. L’utilisateur saisit les paramètres de simulation
2. Les agents restaurants et agents personnes sont créés
3. Chaque agent personne effectue un choix initial
4. Une phase de délibération peut avoir lieu avec d’autres agents
5. Une demande de réservation est envoyée à un restaurant
6. Le restaurant répond par `CONFIRM` ou `REFUSE`
7. En cas de refus, l’agent réessaie avec un autre restaurant
8. Les résultats sont observables via :

   * les logs ;
   * l’interface graphique ;
   * le Sniffer JADE

---

# Concepts SMA utilisés

* Agents autonomes
* Communication FIPA-ACL
* Coordination distribuée
* Délibération entre agents
* Gestion de ressources
* Comportement de troupeau (herding)
* Systèmes adaptatifs

---

# Modélisation

Le projet utilise plusieurs méthodologies :

* UML / AUML
* AGR / AALAADIN
* GAIA
* AEIO

---

# Auteur

Projet réalisé dans le cadre du Master Intelligence Artificielle et Sciences des Données.
