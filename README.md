# MineNorth Journal

Mod Forge 1.20.1 (`minenorthjournal`), sans dépendance. Journal papier pour la presse du serveur.

## Utilisation
- **Journal** (`journal`, 3 papiers + 1 poche d'encre = 4 journaux vierges, onglet MineNorth) : un tas porte la même édition.
- **Accroupi + clic droit** (journalistes) : rédaction. Titre du journal, jusqu'à 4 articles (titre 48 caractères, texte 700). L'enregistrement s'applique à **tout le tas tenu**, appose la date du jour et la signature (entreprise de presse). Les exemplaires sont ensuite vendables (boutiques) ou distribuables : tout le contenu est dans l'objet.
- **Clic droit** : lecture (couverture avec sommaire, puis un article par page ; molette ou `<` `>`).
- **Journalistes** : opérateurs, ou patron / employés de l'entreprise nommée dans `config/minenorth_journal.toml` (`entreprisePresse`, via le mod Entreprises ; vide = opérateurs seulement).
- Le serveur revérifie le droit et assainit les textes (pas de codes `§`, longueurs bornées).

## Licence
**Tous droits réservés - MineNorthRP.** Voir [LICENSE](LICENSE).
