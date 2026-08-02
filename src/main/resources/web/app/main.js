import hljs from 'highlight.js';
import './line-numbers.css';

// Pas d'import de thème highlight.js ici : la coloration syntaxique est définie
// dans web/_custom.css, pour qu'elle suive le mode clair/sombre du thème Roq.

// Le plugin highlightjs-line-numbers.js est une IIFE qui s'installe sur window.hljs.
// Il faut donc exposer hljs sur window AVANT de charger le plugin, puis l'importer
// dynamiquement (les imports statiques sont hoistés et s'exécuteraient trop tôt).
window.hljs = hljs;

import('highlightjs-line-numbers.js').then(() => {
    hljs.highlightAll();
    hljs.initLineNumbersOnLoad();
});
