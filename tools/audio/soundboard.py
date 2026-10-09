# -*- coding: utf-8 -*-
"""
Mesa de sonido de ParDos: UNA página HTML con todo el audio del juego para escucharlo en la computadora (sin teléfono).

    python tools/audio/soundboard.py          # genera tools/audio/soundboard.html y lo abre con el navegador predeterminado

Qué trae la página:
  - los 93 efectos, agrupados, con su volumen en el juego y dónde suenan (los que no se usan en ningún sitio salen marcados);
  - las notas de fusión de cada "voz" (una por efecto de fusión de la tienda) y un simulador de jugada (varias fusiones + combo)
    que suena igual que en el juego;
  - "escenas" que encadenan sonidos como lo hace el juego (victoria, derrota, cofre épico, jefe, racha…);
  - el mezclador de la música adaptativa: 5 piezas x 3 capas, con los presets de intensidad del juego.

El .html es autocontenido (lleva los audios dentro, unos 9 MB): se puede mandar por correo o abrir sin internet. No se sube al repo.
"""
import base64
import json
import os
import re
import sys
import webbrowser

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
RAW = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'raw')
SFX_KT = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'example', 'pardos', 'audio', 'Sfx.kt')
CODE = os.path.join(ROOT, 'app', 'src', 'main', 'java')
OUT = os.path.join(HERE, 'soundboard.html')

VOICES = [
    ('marimba', 'Marimba', 'Clásico (el efecto de fusión por defecto)'),
    ('kalimba', 'Kalimba', 'Chispas'),
    ('bloop', 'Burbujas', 'Burbujas'),
    ('koto', 'Koto', 'Pétalos'),
    ('musicbox', 'Caja de música', 'Corazones'),
    ('xylophone', 'Xilófono', 'Confeti'),
    ('glass', 'Cristal', 'Estrellas'),
    ('ripple', 'Ondas', 'Ondas'),
    ('zap', 'Rayo', 'Rayo'),
    ('firebell', 'Campana de fuego', 'Fuegos artificiales'),
]
MUSIC_SETS = [
    ('zen', 'Zen', 'Capítulos 1-3 y modos sueltos · Do mayor, cálido'),
    ('dusk', 'Atardecer', 'Capítulos 4-6 · Re menor, con swing'),
    ('deep', 'Profundo', 'Capítulos 7-9 y la Torre · Mi menor, aventura'),
    ('boss', 'Jefe', 'Niveles jefe · La menor, tenso'),
    ('halloween', 'Halloween', 'Noche de brujas (menús y partidas) · oscuro y juguetón'),
]

# nombre en Sfx.kt -> (grupo, qué es / cuándo suena)
DOC = {
    'UI_TAP': ('Menús y botones', 'Toque en cualquier botón o tarjeta (cada vez con una ligera variación)'),
    'UI_TAP2': ('Menús y botones', 'Toque alternativo (de reserva)'),
    'UI_BACK': ('Menús y botones', 'Botón de volver'),
    'UI_OPEN': ('Menús y botones', 'Entrar a una pantalla más profunda'),
    'UI_CLOSE': ('Menús y botones', 'Salir de una pantalla'),
    'UI_ON': ('Menús y botones', 'Encender un interruptor o el sonido'),
    'UI_OFF': ('Menús y botones', 'Apagar un interruptor · también al perder una estrella en vivo'),
    'UI_TAB': ('Menús y botones', 'Cambiar de pestaña (tienda y álbum)'),
    'UI_ERROR': ('Menús y botones', 'Algo no se puede (p. ej. el anuncio todavía no está listo)'),
    'UI_LOCKED': ('Menús y botones', 'Tocar un nivel bloqueado del mapa'),
    'UI_WHOOSH': ('Menús y botones', 'Transición entre pantallas'),
    'UI_NOTIFY': ('Menús y botones', 'Aviso (de reserva)'),
    'UI_CONFIRM': ('Menús y botones', 'Una compra con monedas o gemas salió bien'),
    'UI_TICK': ('Menús y botones', 'Tic suave (cuentas atrás)'),
    'UI_POPUP': ('Menús y botones', 'Aparece un diálogo o un premio'),
    'SWIPE': ('Movimiento y fusiones', 'Deslizar el tablero (se oye un poco a izquierda o derecha según la dirección)'),
    'SPAWN': ('Movimiento y fusiones', 'Nace una ficha nueva'),
    'MERGE_SUB': ('Movimiento y fusiones', 'Golpe grave al fusionar fichas grandes (desde 64)'),
    'MERGE_SPARKLE': ('Movimiento y fusiones', 'Destello al fusionar fichas muy grandes (desde 256)'),
    'COMBO_SPARK': ('Combos y racha', 'Chispa en cada fusión encadenada (sube de tono con el combo)'),
    'COMBO_1': ('Combos y racha', 'Combo de 3'),
    'COMBO_2': ('Combos y racha', 'Combo de 5'),
    'COMBO_3': ('Combos y racha', 'Combo de 8'),
    'COMBO_4': ('Combos y racha', 'Combo de 12'),
    'FLOW_IN': ('Combos y racha', 'Entras en FLOW (racha máxima)'),
    'FLOW_TIER': ('Combos y racha', 'Subes un escalón de racha (suena dos veces, más agudo la segunda)'),
    'FLOW_OUT': ('Combos y racha', 'Se corta la racha (un soplido suave)'),
    'MILESTONE_SMALL': ('Combos y racha', 'Primera ficha de 128 o 256 del nivel'),
    'MILESTONE_MID': ('Combos y racha', 'Primera ficha de 512 o 1024 del nivel'),
    'MILESTONE_BIG': ('Combos y racha', 'Primera ficha de 2048 o más'),
    'GOAL_PING': ('Combos y racha', 'La meta se acerca: 50 %, 75 % y 90 % (cada vez más agudo)'),
    'GOAL_DONE': ('Combos y racha', 'Se cumple la meta de una fase'),
    'STAR_1': ('Victoria y derrota', 'Primera estrella del resumen'),
    'STAR_2': ('Victoria y derrota', 'Segunda estrella del resumen'),
    'STAR_3': ('Victoria y derrota', 'Tercera estrella del resumen'),
    'WIN': ('Victoria y derrota', 'Victoria'),
    'WIN_BIG': ('Victoria y derrota', 'Victoria con 3 estrellas'),
    'FAIL': ('Victoria y derrota', 'Derrota'),
    'NEAR_MISS': ('Victoria y derrota', 'Perdiste por muy poco (un suspiro suave tras la derrota)'),
    'NEW_BEST': ('Victoria y derrota', 'Nuevo récord personal al repetir un nivel'),
    'TICK_WARN': ('Victoria y derrota', 'Quedan 2 o 3 jugadas'),
    'HEARTBEAT': ('Victoria y derrota', 'Queda la última jugada'),
    'COUNTDOWN': ('Victoria y derrota', 'Cuenta atrás (de reserva)'),
    'GO': ('Victoria y derrota', '¡Ya! (de reserva)'),
    'UNDO': ('Poderes y reglas', 'Deshacer la última jugada'),
    'HAMMER': ('Poderes y reglas', 'Escoba: quitas una ficha'),
    'SHUFFLE': ('Poderes y reglas', 'Barajar (de reserva)'),
    'MAGIC': ('Poderes y reglas', 'Fusión manual, "ayuda divina" y evolución espontánea de una ficha'),
    'FREEZE': ('Poderes y reglas', 'Tiempo extra'),
    'REVIVE': ('Poderes y reglas', 'Segunda oportunidad al perder'),
    'BOARD_CLEAR': ('Poderes y reglas', 'Limpiar: el tablero se despeja (el poder de espera)'),
    'STONE_BUMP': ('Poderes y reglas', 'Choque contra una piedra (de reserva)'),
    'BLOCKED': ('Poderes y reglas', 'Un deslizamiento que no mueve nada, o una dirección prohibida'),
    'STORM_WARN': ('Poderes y reglas', 'Aviso de tormenta (de reserva)'),
    'STORM_HIT': ('Poderes y reglas', 'Caen o se van piedras en un nivel de tormenta'),
    'BOSS_INTRO': ('Jefes y torre', 'Empieza un nivel jefe'),
    'BOSS_PHASE': ('Jefes y torre', 'El jefe pasa a su segunda fase'),
    'BOSS_WIN': ('Jefes y torre', 'Jefe vencido'),
    'TOWER_FLOOR': ('Jefes y torre', 'Piso de la torre superado'),
    'HEART_LOST': ('Jefes y torre', 'Pierdes un corazón en la torre'),
    'COIN': ('Monedas y progreso', 'Una moneda (también cuenta las monedas del resumen)'),
    'COINS': ('Monedas y progreso', 'Varias monedas juntas (duplicar, ruleta)'),
    'GEM': ('Monedas y progreso', 'Una gema'),
    'XP_TICK': ('Monedas y progreso', 'Tic de la cuenta de monedas del resumen (sube de tono)'),
    'CLAIM': ('Monedas y progreso', 'Reclamar una misión diaria o el premio de la liga'),
    'LEVEL_UP': ('Monedas y progreso', 'Subes de nivel de jugador'),
    'RANK_UP': ('Monedas y progreso', 'Subes de rango de prestigio'),
    'PLATINUM': ('Monedas y progreso', 'Platino o álbum completo'),
    'ACHIEVEMENT': ('Monedas y progreso', 'Logro desbloqueado'),
    'MISSION_DONE': ('Monedas y progreso', 'Misión semanal reclamada'),
    'DAILY': ('Monedas y progreso', 'Recompensa diaria'),
    'STREAK': ('Monedas y progreso', 'Hito de racha de victorias (con su premio)'),
    'GIFT': ('Monedas y progreso', 'Cofre gratis y regalo de regreso'),
    'SEASON_TIER': ('Monedas y progreso', 'Premio del pase de temporada'),
    'REWARD_BIG': ('Monedas y progreso', 'Premio grande (capítulo, serie, semana completa…)'),
    'CHEST_SHAKE': ('Cofres y cartas', 'El cofre se sacude antes de abrirse'),
    'CHEST_OPEN': ('Cofres y cartas', 'El cofre se abre'),
    'CARD_FLIP': ('Cofres y cartas', 'Una carta se voltea'),
    'CARD_COMMON': ('Cofres y cartas', 'Carta común'),
    'CARD_RARE': ('Cofres y cartas', 'Carta rara'),
    'CARD_EPIC': ('Cofres y cartas', 'Carta épica'),
    'CARD_LEGEND': ('Cofres y cartas', 'Carta legendaria'),
    'CARD_NEW': ('Cofres y cartas', 'La carta es nueva en tu álbum'),
    'FOIL': ('Cofres y cartas', 'Una carta se vuelve Brillante'),
    'SELL': ('Cofres y cartas', 'Vender una repetida'),
    'TRADE_SEND': ('Cofres y cartas', 'Envías una propuesta de intercambio'),
    'TRADE_DONE': ('Cofres y cartas', 'Intercambio completado'),
    'WHEEL_TICK': ('Ruleta, hucha y compras', 'Tic de la ruleta al girar'),
    'WHEEL_STOP': ('Ruleta, hucha y compras', 'La ruleta se detiene'),
    'WHEEL_WIN': ('Ruleta, hucha y compras', 'Premio de la ruleta'),
    'PIGGY': ('Ruleta, hucha y compras', 'Se rompe la hucha'),
    'PURCHASE': ('Ruleta, hucha y compras', 'Compra real completada'),
    'AD_REWARD': ('Ruleta, hucha y compras', 'El anuncio con premio se vio completo'),
}

GROUP_ORDER = [
    'Menús y botones', 'Movimiento y fusiones', 'Combos y racha', 'Victoria y derrota', 'Poderes y reglas',
    'Jefes y torre', 'Monedas y progreso', 'Cofres y cartas', 'Ruleta, hucha y compras',
]

# escenas: lo que el juego encadena de verdad (t en milisegundos; g = volumen; r = velocidad/tono)
SCENES = [
    ('Victoria con 3 estrellas', 'Fanfarria, las tres estrellas una a una, cuenta de monedas y récord',
     [('win_big', 0, 1, 1), ('star_1', 350, .95, 1), ('star_2', 680, .95, 1), ('star_3', 1010, 1, 1)]
     + [('xp_tick', 1250 + 48 * i, .55, .9 + .05 * (i + 1)) for i in range(12)]
     + [('coin', 1830, .9, 1), ('new_best', 2000, 1, 1)]),
    ('Derrota y casi', 'Sonido de derrota y el suspiro de "casi lo logras"',
     [('fail', 0, .8, 1), ('near_miss', 700, .7, 1)]),
    ('Racha hasta FLOW', 'Calma → en racha → imparable → ¡FLOW! → se corta',
     [('flow_tier', 0, .8, .92), ('flow_tier', 1200, .95, 1.06), ('flow_in', 2400, 1, 1), ('flow_out', 5200, .6, 1)]),
    ('Cofre épico', 'El cofre se sacude, se abre y salen cartas de rareza creciente',
     [('chest_shake', 0, .9, 1), ('chest_open', 500, 1, 1)]
     + [x for i, (r, nuevo) in enumerate([('card_common', True), ('card_rare', True), ('card_epic', True), ('card_legend', True)])
        for x in ([('card_flip', 1100 + 700 * i, .7, 1), (r, 1190 + 700 * i, 1, 1)] + ([('card_new', 1520 + 700 * i, .9, 1)] if nuevo else []))]),
    ('Jefe', 'Entrada del jefe, segunda fase y victoria',
     [('boss_intro', 0, 1, 1), ('boss_phase', 2500, 1, 1), ('boss_win', 5000, 1, 1)]),
    ('Menús', 'Entrar, cambiar de pestaña, tocar, volver',
     [('ui_open', 0, .8, 1), ('ui_tab', 450, .8, 1), ('ui_tap', 800, .9, 1), ('ui_confirm', 1200, .9, 1), ('coin', 1320, .9, 1), ('ui_back', 1900, .85, 1)]),
    ('Premios', 'Subir de nivel, logro, premio grande y gemas',
     [('level_up', 0, 1, 1), ('coins', 1500, .95, 1), ('gem', 1900, .95, 1), ('achievement', 2800, 1, 1), ('reward_big', 4200, 1, 1)]),
    ('Ruleta', 'Giro con tics cada vez más lentos, parada y premio',
     [('wheel_tick', t, .6, 1) for t in (0, 90, 190, 300, 430, 590, 790, 1040, 1350, 1730)] + [('wheel_stop', 2150, .9, 1), ('wheel_win', 2450, 1, 1)]),
]


def parse_sfx():
    src = open(SFX_KT, encoding='utf-8').read()
    out = []
    for m in re.finditer(r'^\s+([A-Z_0-9]+)\("([a-z_0-9]+)",\s*([0-9.]+)f?(?:,\s*(\d+))?(?:,\s*(\d+))?\)', src, re.M):
        out.append({'id': m.group(1), 'file': m.group(2), 'gain': float(m.group(3))})
    return out


def usage():
    use = {}
    for d, _, files in os.walk(CODE):
        for f in files:
            if not f.endswith('.kt'):
                continue
            text = open(os.path.join(d, f), encoding='utf-8').read()
            for m in re.finditer(r'Sfx\.([A-Z_0-9]+)', text):
                use.setdefault(m.group(1), set()).add(f[:-3])
    return use


def b64(path):
    with open(path, 'rb') as fh:
        return base64.b64encode(fh.read()).decode('ascii')


def main():
    sfx = parse_sfx()
    used = usage()
    cards = []
    data = {}
    for e in sfx:
        path = os.path.join(RAW, e['file'] + '.ogg')
        if not os.path.exists(path):
            print('falta', path)
            continue
        group, desc = DOC.get(e['id'], ('Otros', e['id']))
        cards.append({'id': e['id'], 'file': e['file'], 'gain': e['gain'], 'group': group, 'desc': desc, 'used': e['id'] in used})
        data[e['file']] = b64(path)
    for v, _, _ in VOICES:
        for n in range(1, 13):
            name = 'mg_%s_%02d' % (v, n)
            data[name] = b64(os.path.join(RAW, name + '.ogg'))
    music = {}
    for s, _, _ in MUSIC_SETS:
        music[s] = {layer: b64(os.path.join(RAW, 'music_%s_%s.ogg' % (s, layer))) for layer in ('base', 'groove', 'lead')}

    page = TEMPLATE
    page = page.replace('/*CARDS*/[]', json.dumps(cards, ensure_ascii=False))
    page = page.replace('/*GROUPS*/[]', json.dumps(GROUP_ORDER, ensure_ascii=False))
    page = page.replace('/*VOICES*/[]', json.dumps([{'id': v, 'name': n, 'fx': f} for v, n, f in VOICES], ensure_ascii=False))
    page = page.replace('/*SETS*/[]', json.dumps([{'id': s, 'name': n, 'desc': d} for s, n, d in MUSIC_SETS], ensure_ascii=False))
    page = page.replace('/*SCENES*/[]', json.dumps([{'name': n, 'desc': d, 'steps': st} for n, d, st in SCENES], ensure_ascii=False))
    page = page.replace('/*DATA*/{}', json.dumps(data))
    page = page.replace('/*MUSIC*/{}', json.dumps(music))
    with open(OUT, 'w', encoding='utf-8') as fh:
        fh.write(page)
    print('listo: %s (%.1f MB), %d efectos, %d sin usar' % (OUT, os.path.getsize(OUT) / 1e6, len(cards), sum(1 for c in cards if not c['used'])))
    if '--no-open' not in sys.argv:
        webbrowser.open('file:///' + OUT.replace('\\', '/'))


TEMPLATE = r'''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mesa de sonido · ParDos</title>
<style>
:root { --navy:#3D405B; --sage:#6B9E86; --terra:#E07A5F; --gold:#E0A93B; --sand:#F2CC8F; --paper:#F3EFE6; --cream:#FFFBF5; --violet:#6C63FF; --gem:#4E8FA6; }
* { box-sizing: border-box; }
body { margin:0; font-family: system-ui, "Segoe UI", Roboto, sans-serif; background: var(--paper); color: var(--navy); }
header { position: sticky; top:0; z-index:10; background: var(--navy); color:#fff; padding: 10px 18px; display:flex; flex-wrap:wrap; gap:12px 20px; align-items:center; box-shadow: 0 2px 12px #0003; }
header h1 { font-size: 18px; margin:0; letter-spacing:.5px; }
header label { font-size:12px; display:flex; align-items:center; gap:8px; }
header input[type=range] { width: 160px; accent-color: var(--sand); }
button { font: inherit; cursor:pointer; border:0; border-radius: 12px; padding: 8px 14px; background: var(--sage); color:#fff; font-weight:700; box-shadow: 0 3px 0 #0003; }
button:active { transform: translateY(2px); box-shadow:none; }
button.ghost { background: transparent; color:#fff; border:1px solid #fff6; box-shadow:none; }
button.alt { background: var(--violet); }
button.warn { background: var(--terra); }
nav { display:flex; flex-wrap:wrap; gap:8px; padding: 14px 18px 0; }
nav button { background:#fff; color: var(--navy); box-shadow: 0 3px 0 #cdb89480; }
nav button.on { background: var(--navy); color:#fff; }
main { padding: 14px 18px 60px; max-width: 1180px; margin: 0 auto; }
section { display:none; }
section.on { display:block; }
h2 { font-size: 13px; letter-spacing: 2.5px; text-transform: uppercase; color: #3D405B99; margin: 26px 0 10px; }
p.hint { font-size: 13px; color:#3D405Bb0; margin: 4px 0 12px; max-width: 800px; line-height:1.45; }
.grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(255px, 1fr)); gap: 10px; }
.card { background:#fff; border-radius: 16px; padding: 10px 12px; display:flex; gap:10px; align-items:center; box-shadow: 0 3px 0 #cdb89466; }
.card.unused { opacity:.62; }
.card .t { flex:1; min-width:0; }
.card b { display:block; font-size: 13px; }
.card small { display:block; font-size: 11px; color:#3D405B99; line-height: 1.3; }
.card .tag { font-size:10px; color: var(--terra); font-weight:700; }
.play { width:38px; height:38px; border-radius:50%; padding:0; display:grid; place-items:center; font-size:15px; flex:none; }
.keys { display:flex; flex-wrap:wrap; gap:8px; margin: 10px 0 14px; }
.key { min-width: 64px; text-align:center; background:#fff; color: var(--navy); box-shadow: 0 3px 0 #cdb89480; }
.key small { display:block; font-size:10px; opacity:.6; font-weight:600; }
.panel { background:#fff; border-radius: 18px; padding: 14px 16px; margin: 10px 0; box-shadow: 0 3px 0 #cdb89466; }
.row { display:flex; flex-wrap:wrap; gap:12px 18px; align-items:center; }
.row label { font-size: 12px; display:flex; align-items:center; gap:6px; }
select { font: inherit; padding: 6px 10px; border-radius: 10px; border:1px solid #3D405B33; background: var(--cream); }
.chips { display:flex; flex-wrap:wrap; gap:6px; }
.chips label { background: var(--cream); border:1px solid #3D405B22; border-radius: 999px; padding: 4px 10px; font-size: 12px; cursor:pointer; }
.layer { display:grid; grid-template-columns: 90px 1fr 46px; gap: 10px; align-items:center; margin: 8px 0; font-size: 13px; }
.layer input { width:100%; accent-color: var(--sage); }
.sets { display:flex; flex-wrap:wrap; gap:8px; margin-bottom: 10px; }
.sets button { background:#fff; color: var(--navy); box-shadow: 0 3px 0 #cdb89480; }
.sets button.on { background: var(--violet); color:#fff; }
#status { font-size: 12px; opacity:.75; }
.note { font-size:12px; color:#3D405B99; margin-top: 30px; line-height:1.5; }
</style>
</head>
<body>
<header>
  <h1>🎧 Mesa de sonido · ParDos</h1>
  <label>Volumen <input id="vol" type="range" min="0" max="100" value="80"></label>
  <button class="ghost" onclick="stopAll()">Detener todo</button>
  <span id="status"></span>
</header>
<nav id="tabs"></nav>
<main>
  <section id="s-sfx"></section>
  <section id="s-merge"></section>
  <section id="s-scenes"></section>
  <section id="s-music"></section>
  <p class="note">Los audios son exactamente los del juego (<code>app/src/main/res/raw</code>). Aquí no suena el volumen de Ajustes del teléfono ni la mezcla del altavoz:
  si algo te parece fuerte, flojo o repetitivo, dime su nombre y lo ajusto (cada efecto tiene un valor de importancia en <code>tools/audio/sfx.py</code>).</p>
</main>
<script>
const CARDS = /*CARDS*/[];
const GROUPS = /*GROUPS*/[];
const VOICES = /*VOICES*/[];
const SETS = /*SETS*/[];
const SCENES = /*SCENES*/[];
const DATA = /*DATA*/{};
const MUSIC = /*MUSIC*/{};

const $ = (s, r = document) => r.querySelector(s);
let ctx = null, master = null;
const cache = {};
const live = new Set();

function ensure() {
  if (!ctx) {
    ctx = new (window.AudioContext || window.webkitAudioContext)();
    master = ctx.createGain();
    master.gain.value = $('#vol').value / 100;
    master.connect(ctx.destination);
  }
  if (ctx.state === 'suspended') ctx.resume();
  return ctx;
}
$('#vol').addEventListener('input', e => { if (master) master.gain.setTargetAtTime(e.target.value / 100, ctx.currentTime, 0.03); });

function toBuf(b64) {
  const bin = atob(b64), bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}
async function buf(key, b64) {
  if (!cache[key]) cache[key] = await ensure().decodeAudioData(toBuf(b64));
  return cache[key];
}
async function sfxBuf(name) { return buf('s:' + name, DATA[name]); }

function fire(buffer, { gain = 1, rate = 1, when = 0, pan = 0 } = {}) {
  const c = ensure();
  const src = c.createBufferSource();
  src.buffer = buffer;
  src.playbackRate.value = rate;
  const g = c.createGain();
  g.gain.value = gain;
  src.connect(g);
  let tail = g;
  if (c.createStereoPanner) { const p = c.createStereoPanner(); p.pan.value = pan; g.connect(p); tail = p; }
  tail.connect(master);
  src.start(c.currentTime + when);
  live.add(src);
  src.onended = () => live.delete(src);
  return src;
}
async function play(name, o = {}) { const b = await sfxBuf(name); return fire(b, o); }

// Programa una lista [nombre, ms, volumen, tono] ya decodificada, para que los tiempos sean exactos
async function schedule(steps, gainScale = 1) {
  ensure();
  const names = [...new Set(steps.map(s => s[0]))];
  await Promise.all(names.map(sfxBuf));
  const base = 0.06;
  steps.forEach(([n, ms, g, r]) => fire(cache['s:' + n], { gain: g * gainScale, rate: r || 1, when: base + ms / 1000 }));
}
function stopAll() {
  live.forEach(s => { try { s.stop(); } catch (e) {} });
  live.clear();
  stopMusic(0.1);
}

// ---------------------------------------------------------------- pestañas
const TABS = [['s-sfx', '🔔 Efectos'], ['s-merge', '🎹 Fusiones y combos'], ['s-scenes', '🎬 Escenas'], ['s-music', '🎼 Música']];
TABS.forEach(([id, label], i) => {
  const b = document.createElement('button');
  b.textContent = label;
  b.onclick = () => { TABS.forEach(([x]) => { $('#' + x).classList.toggle('on', x === id); }); [...$('#tabs').children].forEach((c, j) => c.classList.toggle('on', j === i)); };
  $('#tabs').appendChild(b);
});
$('#tabs').children[0].click();

// ---------------------------------------------------------------- efectos
(function () {
  const root = $('#s-sfx');
  root.innerHTML = '<p class="hint">Toca ▶ para oír cada efecto con el volumen relativo que tiene en el juego. Los atenuados no suenan en ningún sitio todavía (están de reserva).</p>';
  GROUPS.forEach(g => {
    const items = CARDS.filter(c => c.group === g);
    if (!items.length) return;
    root.insertAdjacentHTML('beforeend', '<h2>' + g + ' · ' + items.length + '</h2>');
    const grid = document.createElement('div'); grid.className = 'grid';
    items.forEach(c => {
      const d = document.createElement('div'); d.className = 'card' + (c.used ? '' : ' unused');
      d.innerHTML = '<button class="play">▶</button><div class="t"><b>' + c.file + '</b><small>' + c.desc + '</small>' +
        '<small class="tag">' + (c.used ? '' : 'sin usar todavía · ') + 'volumen en el juego ' + Math.round(c.gain * 100) + ' %</small></div>';
      d.querySelector('button').onclick = () => play(c.file, { gain: c.gain });
      grid.appendChild(d);
    });
    root.appendChild(grid);
  });
})();

// ---------------------------------------------------------------- fusiones: notas, voces y simulador de jugada
const LEVEL_VALUES = [2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096];
const lv = n => String(n).padStart(2, '0');
(function () {
  const root = $('#s-merge');
  root.innerHTML = `
    <p class="hint">Cada fusión es una nota de la escala pentatónica: cuanto más grande la ficha, más aguda. La <b>voz</b> sigue al efecto de fusión que tengas equipado en la tienda.</p>
    <div class="panel"><div class="row">
      <label>Voz <select id="voice"></select></label>
      <button id="scale">▶ Escala completa (2 → 4096)</button>
    </div><div class="keys" id="keys"></div></div>
    <h2>Simulador de jugada</h2>
    <p class="hint">Marca las fichas que se forman en una misma jugada (suenan en arpegio, de la más chica a la más grande) y el combo que llevas. Así suena en el juego.</p>
    <div class="panel">
      <div class="chips" id="vals"></div>
      <div class="row" style="margin-top:12px">
        <label>Combo <select id="combo"></select></label>
        <button id="sim" class="alt">▶ Simular jugada</button>
        <button id="chain" class="alt">▶ Cadena de combos (2 → 12)</button>
      </div>
    </div>`;
  const sel = $('#voice', root);
  VOICES.forEach(v => sel.insertAdjacentHTML('beforeend', '<option value="' + v.id + '">' + v.name + ' (' + v.fx + ')</option>'));
  const keys = $('#keys', root);
  LEVEL_VALUES.forEach((v, i) => {
    const b = document.createElement('button'); b.className = 'key'; b.innerHTML = v + '<small>nota ' + (i + 1) + '</small>';
    b.onclick = () => play('mg_' + sel.value + '_' + lv(i + 1), { gain: 0.95 });
    keys.appendChild(b);
  });
  $('#scale', root).onclick = () => schedule(LEVEL_VALUES.map((_, i) => ['mg_' + sel.value + '_' + lv(i + 1), i * 190, .95, 1]));
  const vals = $('#vals', root);
  [2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048].forEach((v, i) => {
    vals.insertAdjacentHTML('beforeend', '<label><input type="checkbox" value="' + v + '"' + (i === 3 || i === 6 ? ' checked' : '') + '> ' + v + '</label>');
  });
  const combo = $('#combo', root);
  for (let i = 0; i <= 12; i++) combo.insertAdjacentHTML('beforeend', '<option value="' + i + '"' + (i === 3 ? ' selected' : '') + '>' + (i < 2 ? 'sin combo' : 'x' + i) + '</option>');
  // misma lógica que GameAudio.merges() y GameAudio.combo()
  const levelOf = v => Math.min(12, Math.max(1, Math.round(Math.log2(Math.max(v, 2)))));
  function mergeSteps(values, comboN, t0 = 0) {
    const steps = [];
    if (!values.length) return steps;
    const levels = values.map(levelOf).sort((a, b) => a - b);
    const distinct = [...new Set(levels)].slice(-5);
    distinct.forEach((l, i) => {
      const count = levels.filter(x => x === l).length;
      const vol = Math.min(1, 0.78 + 0.035 * l + 0.04 * i + (count > 1 ? 0.08 : 0));
      steps.push(['mg_' + sel.value + '_' + lv(l), t0 + i * 55, vol * 0.95, 1]);
    });
    const top = levels[levels.length - 1];
    if (top >= 6) steps.push(['merge_sub', t0, Math.min(1, (top - 5) / 7 + 0.35) * 0.9, 1]);
    if (top >= 8) steps.push(['merge_sparkle', t0, 0.7 * 0.8, 1]);
    if (comboN >= 2) {
      steps.push(['combo_spark', t0, (0.55 + 0.03 * Math.min(comboN, 10)) * 0.75, 1 + 0.035 * Math.min(comboN, 12)]);
      const big = { 3: ['combo_1', .9], 5: ['combo_2', .95], 8: ['combo_3', 1], 12: ['combo_4', 1] }[comboN];
      if (big) steps.push([big[0], t0 + 120, big[1], 1]);
    }
    return steps;
  }
  $('#sim', root).onclick = () => {
    const values = [...vals.querySelectorAll('input:checked')].map(i => +i.value);
    schedule(mergeSteps(values, +combo.value));
  };
  $('#chain', root).onclick = () => {
    let steps = [];
    for (let c = 2; c <= 12; c++) steps = steps.concat(mergeSteps([2 << (c % 6), 4 << (c % 5)], c, (c - 2) * 420));
    schedule(steps);
  };
})();

// ---------------------------------------------------------------- escenas
(function () {
  const root = $('#s-scenes');
  root.innerHTML = '<p class="hint">Cadenas de sonidos con los mismos tiempos que usa el juego, para juzgar el conjunto y no solo cada pieza.</p>';
  const grid = document.createElement('div'); grid.className = 'grid';
  SCENES.forEach(s => {
    const d = document.createElement('div'); d.className = 'card';
    d.innerHTML = '<button class="play alt">▶</button><div class="t"><b>' + s.name + '</b><small>' + s.desc + '</small></div>';
    d.querySelector('button').onclick = () => schedule(s.steps);
    grid.appendChild(d);
  });
  root.appendChild(grid);
})();

// ---------------------------------------------------------------- música adaptativa
let musicNodes = null;
const PRESETS = [['Calma', [1, 0, 0]], ['En racha', [1, .6, 0]], ['Imparable', [1, .9, .55]], ['¡FLOW!', [1, 1, .9]]];
const LAYERS = [['base', 'Base'], ['groove', 'Ritmo'], ['lead', 'Melodía']];
let currentSet = SETS[0].id;
(function () {
  const root = $('#s-music');
  root.innerHTML = `
    <p class="hint">Cinco piezas, cada una en tres capas sincronizadas. En el juego las capas entran y salen según el ritmo de tus jugadas (los presets de abajo son los mismos). Las piezas son bucles: no tienen final.</p>
    <div class="sets" id="sets"></div>
    <div class="panel">
      <div id="setdesc" class="hint" style="margin-top:0"></div>
      <div id="layers"></div>
      <div class="row" style="margin-top:12px" id="presets"></div>
      <div class="row" style="margin-top:12px"><button id="mplay" class="alt">▶ Tocar</button><button id="mstop" class="warn">■ Parar</button></div>
    </div>`;
  const sets = $('#sets', root);
  SETS.forEach(s => {
    const b = document.createElement('button'); b.textContent = s.name; b.dataset.id = s.id;
    b.onclick = () => { currentSet = s.id; refreshSets(); if (musicNodes) startMusic(); };
    sets.appendChild(b);
  });
  function refreshSets() {
    [...sets.children].forEach(b => b.classList.toggle('on', b.dataset.id === currentSet));
    $('#setdesc', root).textContent = SETS.find(s => s.id === currentSet).desc;
  }
  refreshSets();
  const layers = $('#layers', root);
  LAYERS.forEach(([id, label], i) => {
    layers.insertAdjacentHTML('beforeend', '<div class="layer"><span>' + label + '</span><input type="range" min="0" max="100" value="' + (i === 0 ? 100 : 0) + '" id="L' + i + '"><span id="LV' + i + '">' + (i === 0 ? 100 : 0) + '</span></div>');
    $('#L' + i, root).addEventListener('input', applyLayers);
  });
  const presets = $('#presets', root);
  PRESETS.forEach(([name, g]) => {
    const b = document.createElement('button'); b.textContent = name; b.className = 'ghost'; b.style.color = '#3D405B'; b.style.borderColor = '#3D405B44';
    b.onclick = () => { g.forEach((v, i) => { $('#L' + i, root).value = Math.round(v * 100); }); applyLayers(); };
    presets.appendChild(b);
  });
  $('#mplay', root).onclick = startMusic;
  $('#mstop', root).onclick = () => stopMusic(0.6);
})();
function applyLayers() {
  LAYERS.forEach((_, i) => {
    const v = +$('#L' + i).value;
    $('#LV' + i).textContent = v;
    if (musicNodes) musicNodes.gains[i].gain.setTargetAtTime(v / 100, ctx.currentTime, 0.45);
  });
}
async function startMusic() {
  const c = ensure();
  stopMusic(0.5);
  $('#status').textContent = 'cargando música…';
  const set = currentSet;
  const bufs = await Promise.all(LAYERS.map(([id]) => buf('m:' + set + ':' + id, MUSIC[set][id])));
  if (set !== currentSet) return;
  const out = c.createGain(); out.gain.value = 0; out.connect(master);
  out.gain.setTargetAtTime(1, c.currentTime, 0.4);
  const when = c.currentTime + 0.1;
  const gains = [], sources = [];
  bufs.forEach((b, i) => {
    const g = c.createGain(); g.gain.value = +$('#L' + i).value / 100; g.connect(out);
    const s = c.createBufferSource(); s.buffer = b; s.loop = true; s.connect(g); s.start(when);
    gains.push(g); sources.push(s);
  });
  musicNodes = { out, gains, sources, set };
  $('#status').textContent = 'sonando: ' + SETS.find(s => s.id === set).name;
}
function stopMusic(fade) {
  if (!musicNodes) return;
  const { out, sources } = musicNodes;
  out.gain.setTargetAtTime(0, ctx.currentTime, Math.max(0.03, fade / 3));
  // tras el fundido se cortan las fuentes (si no, los bucles seguirían corriendo en silencio)
  sources.forEach(s => { try { s.stop(ctx.currentTime + fade + 0.5); } catch (e) {} });
  setTimeout(() => { try { out.disconnect(); } catch (e) {} }, fade * 1000 + 700);
  musicNodes = null;
  $('#status').textContent = '';
}
</script>
</body>
</html>
'''

if __name__ == '__main__':
    main()
