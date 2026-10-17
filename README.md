# MineNorth Journal

Mod Forge 1.20.1 (`minenorthjournal`), sans dépendance. Journal papier pour la presse du serveur.

## Utilisation
- **Journal** (`journal`, 3 papiers + 1 poche d'encre = 4 journaux vierges, onglet MineNorth) : un tas porte la même édition.
- **Accroupi + clic droit** (journalistes) : rédaction. Titre du journal, jusqu'à 4 articles (titre 48 caractères, texte 700). L'enregistrement s'applique à **tout le tas tenu**, appose la date du jour et la signature (entreprise de presse). Les exemplaires sont ensuite vendables (boutiques) ou distribuables : tout le contenu est dans l'objet.
- **Clic droit** : lecture (couverture avec sommaire, puis un article par page ; molette ou `<` `>`).
- **Journalistes** : opérateurs, ou patron / employés de l'entreprise nommée dans `config/minenorth_journal.toml` (`entreprisePresse`, via le mod Entreprises ; vide = opérateurs seulement).
- Le serveur revérifie le droit et assainit les textes (pas de codes `§`, longueurs bornées).

## Images, publicités, signature
- Chaque article peut avoir une **image** (identifiant d'objet, ex. `minecraft:diamond`, ou celui d'un objet d'un autre mod ; bouton « Objet en main » = objet tenu dans l'autre main), affichée en grand en tête de page.
- Jusqu'à **3 encarts publicitaires** (onglets « Pub 1 à 3 » de la rédaction) : annonceur, texte (90 caractères), image d'objet, couleur (rouge, bleu, vert, or). Un encart s'affiche en bas de la couverture et de chaque page (ils tournent d'une page à l'autre).
- La **signature** est le **nom RP** (« Par Prénom Nom », mod Identité) suivi de l'entreprise de presse de la config ; repli sur le pseudo sans le mod Identité.

## Photos (images du serveur)
- L'admin dépose des **PNG** dans `config/minenorth_journal/images/` du serveur (nom en minuscules : lettres, chiffres, `-`, `_` ; 256 Ko et 1024 px maximum conseillés). Le dossier est créé au premier usage.
- Dans la rédaction, le bouton **« Photo suivante »** parcourt les photos disponibles (champ Image : `photo:nom`) ; une photo est utilisable dans un article **et** dans un encart publicitaire, à la place d'une image d'objet. Les clients la reçoivent du serveur à la demande (une seule fois, gardée en mémoire) ; le serveur n'accepte que les noms de fichiers du dossier.
- Les journalistes ne peuvent pas envoyer de fichier eux-mêmes : seul l'admin ajoute des photos (contrôle du contenu).

## Kiosque à journaux
- **Kiosque** (`kiosque`, 3 laines rouges + 5 planches, onglet MineNorth) : un journaliste y range ses journaux (clic droit, journaux rédigés en main ; 4 tas de 16). Les joueurs en prennent un (clic droit à main nue), gratuit ou au prix `prixKiosqueCentimes` (config, payé par carte EuroBank, versé au trésor public). Journalistes : accroupi + clic droit à main nue = tout reprendre. Les piles de journaux deviennent visibles sur le kiosque selon le stock ; le casser rend le contenu.

## Licence
**Tous droits réservés - MineNorthRP.** Voir [LICENSE](LICENSE).
