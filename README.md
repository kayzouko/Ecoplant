# EcoPlant

**EcoPlant** est une application mobile permettant d'identifier des plantes herbacées et d'évaluer leurs services écosystémiques. L'application s'appuie sur l'API de [PlantNet](https://plantnet.org/) pour l'identification des plantes et fournit des scores pour trois services écologiques clés.

## Fonctionnalités

### Identification des plantes
- Prise de photo via l'appareil mobile.
- Utilisation de l'API PlantNet pour identifier la plante.

### Scores écologiques
- Affichage graphique de scores (de 0 à 1) pour :
  - La **fixation de l'azote** dans le sol
  - L'**amélioration de la structure** du sol
  - La **capacité à retenir l’eau** dans le sol

### Historique des analyses
- Stockage local des plantes identifiées et de leurs scores.
- Possibilité d'ajouter des **notes ou observations** personnalisées.

### Géolocalisation
- Carte interactive avec géolocalisation des relevés.

### Informations complémentaires
- Accès à des **fiches descriptives** via la plateforme [Tela Botanica](https://www.tela-botanica.org/).
- **Recherche approfondie** sur l'espèce identifiée.

## Extensions Possibles
- Mode collaboratif pour ajouter de nouvelles observations.

## Technologies Utilisées
- **Langage** : Kotlin (Android)
- **API** : PlantNet
- **Base de données** : Firebase (distant)
- Autres : Fragments, Capteurs, Services, Notifications, etc.

## Comment Exécuter le Projet

```bash
git clone https://github.com/kayzouko/EcoPlant.git
```
- Ouvrez le projet dans Android Studio.

- Exécutez l'application sur un appareil physique et non sur émulateur.

## Contribution
Les contributions sont les bienvenues ! Pour contribuer :

- **Forkez** le dépôt.

- Créez une branche :
```bash
git checkout -b feature/nouvelle-fonctionnalité
```
- Commitez vos changements :
```bash
git commit -m 'Ajout d'une nouvelle fonctionnalité'
```
- Pushez la branche :
```bash
git push origin feature/nouvelle-fonctionnalité
```
- Ouvrez une **Pull Request.**

---

© 2025 EcoPlant – Tous droits réservés