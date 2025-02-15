#!/bin/bash

# Tester l'architecture du système
architecture=$(uname -m)

# Définir les URL des fichiers binaires pour chaque architecture
if [ "$architecture" = "armv7l" ] || [ "$architecture" = "aarch64" ]; then
    url="https://github.com/slidesk/slidesk/releases/latest/download/slidesk_linux-arm.tar.gz"
elif [ "$architecture" = "x86_64" ]; then
    url="https://github.com/slidesk/slidesk/releases/latest/download/slidesk_linux-amd.tar.gz"
else
    echo "Architecture non supportée : $architecture"
    exit 1
fi

# Télécharger le fichier binaire
wget -O slidesk.tar.gz "$url"

# Vérifier si le téléchargement a réussi
if [ $? -ne 0 ]; then
    echo "Erreur de téléchargement"
    exit 1
fi

# Décompresser le fichier tar.gz
tar -xzf slidesk.tar.gz

# Supprimer le fichier tar.gz original
rm slidesk.tar.gz

# Renommer le fichier décompressé en slidesk
for fichier in slidesk_linux*; do
    if [ -f "$fichier" ]; then
        mv "$fichier" slidesk
        break
    fi
done

# Vérifier si le renommage a réussi
if [ -f slidesk ]; then
    echo "Le fichier a été renommé avec succès"
else
    echo "Erreur de renommage"
    exit 1
fi

chmod +x ./slidesk
sudo mv slidesk /usr/bin
