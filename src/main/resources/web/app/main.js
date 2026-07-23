import hljs from 'highlight.js';
import 'highlight.js/styles/agate.css';
import './line-numbers.css';

// Le plugin highlightjs-line-numbers.js est une IIFE qui s'installe sur window.hljs.
// Il faut donc exposer hljs sur window AVANT de charger le plugin, puis l'importer
// dynamiquement (les imports statiques sont hoistés et s'exécuteraient trop tôt).
window.hljs = hljs;

import('highlightjs-line-numbers.js').then(() => {
    hljs.highlightAll();
    hljs.initLineNumbersOnLoad();
});
