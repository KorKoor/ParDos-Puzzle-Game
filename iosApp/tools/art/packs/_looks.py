"""Paletas de los avatares (sacadas de AvatarArt.kt de Android). Ranuras: head, light, dark, inner, bg1, bg2, shirt."""
SLOTS = ["head", "light", "dark", "inner", "bg1", "bg2", "shirt"]

ANIMALS = ['FOX', 'CAT', 'PANDA', 'BUNNY', 'BEAR', 'FROG', 'OWL', 'PENGUIN', 'KOALA', 'RACCOON', 'CHICK', 'UNICORN', 'DRAGON', 'AXOLOTL', 'CAPYBARA', 'TIGER', 'LION', 'WOLF', 'SHEEP', 'HEDGEHOG', 'TURTLE', 'PIG', 'MONKEY', 'HAMSTER', 'DEER', 'COW', 'DUCK', 'BAT', 'GHOST', 'PUMPKIN', 'ROBOT', 'ALIEN', 'DINO', 'PHOENIX']

BASE = {
    'FOX': ['#F29A4A', '#FFF2DC', '#5C3A21', '#F7C79A', '#FFE6C9', '#FFCB9A', '#6B9E86'],
    'CAT': ['#F4C48E', '#FFF0DC', '#8A5A2B', '#FFB3C1', '#E3EEFF', '#C9DCFF', '#E5576B'],
    'PANDA': ['#FFFFFF', '#F1F1F4', '#34353F', '#FFB3C1', '#D9F2E3', '#B5E3C8', '#6B9E86'],
    'BUNNY': ['#F8F0F8', '#FFFFFF', '#8A6A7A', '#FFB3C9', '#FFE0EE', '#FFC4DD', '#8E7CC3'],
    'BEAR': ['#BE8A52', '#F1D3A8', '#4F3320', '#E2B98C', '#F6E3CC', '#EBC9A0', '#E07A5F'],
    'FROG': ['#7FCB7A', '#F1F8D8', '#2F6B3A', '#B8E5A0', '#D9F5CF', '#B2E8A2', '#E0A93B'],
    'OWL': ['#B58B5C', '#F1DDB8', '#4A3322', '#D9B88A', '#E7DDF5', '#CDBBEB', '#6C63FF'],
    'PENGUIN': ['#3E4A6B', '#FFFFFF', '#232B45', '#F2A93B', '#D6ECFA', '#B0DAF2', '#E5576B'],
    'KOALA': ['#A9AFBF', '#E9ECF3', '#3B3B4A', '#D7DBE6', '#E5E8F5', '#CDD3EB', '#6B9E86'],
    'RACCOON': ['#A9A9B8', '#F4F4F8', '#3F4050', '#D0D0DC', '#E8E0F0', '#D3C5E6', '#E0A93B'],
    'CHICK': ['#FFD84A', '#FFF0A8', '#B87510', '#F29A2E', '#FFF3C4', '#FFE28A', '#E07A5F'],
    'UNICORN': ['#FFF0F6', '#FFFFFF', '#8A5A8E', '#FFB3D1', '#F0E0FF', '#D9C2FF', '#8E7CC3'],
    'DRAGON': ['#6CC59A', '#E8F7D9', '#2A6B50', '#F2D58A', '#D6F5E8', '#A8E6CC', '#E0A93B'],
    'AXOLOTL': ['#FFB7C9', '#FFE3EA', '#8A4A63', '#FF7FA3', '#D8F1FF', '#B5E3FA', '#6C63FF'],
    'CAPYBARA': ['#B98A5E', '#E6CBA3', '#5A3D24', '#8A6240', '#E3F2D9', '#CBE6BA', '#6B9E86'],
    'TIGER': ['#F5A03C', '#FFF0DA', '#3A2414', '#FFC38A', '#FFEBC9', '#FFD08A', '#3E7C5A'],
    'LION': ['#F2C36B', '#FFF0C8', '#8A4A16', '#E8A24A', '#FFF0CC', '#FFD98A', '#B8473A'],
    'WOLF': ['#9AA3B5', '#E9EDF5', '#2E3446', '#CDD3E0', '#E0E6F5', '#BFCBE8', '#455A9E'],
    'SHEEP': ['#F3D9C2', '#FFF7EC', '#5A4636', '#FFB3C1', '#E6F3FF', '#CDE5FA', '#E5576B'],
    'HEDGEHOG': ['#E9C9A0', '#FFF0DC', '#6B4A33', '#FFB3C1', '#F3E6D2', '#E6CFAE', '#6B9E86'],
    'TURTLE': ['#8ED08A', '#F2F8D8', '#2F6B3A', '#B8E5A0', '#D6F3F5', '#A8E0E8', '#E0A93B'],
    'PIG': ['#F7B6C6', '#FFE3EA', '#8A3E55', '#F29BB0', '#FFE9EE', '#FFCCDA', '#6B9E86'],
    'MONKEY': ['#B5794A', '#F4D9B5', '#4F3320', '#E8B98A', '#E6F3D2', '#CBE6A8', '#E5576B'],
    'HAMSTER': ['#F2C48A', '#FFF3E0', '#8A5A2B', '#FFB3C1', '#FFF0D6', '#FFDDA8', '#6C63FF'],
    'DEER': ['#C98E5A', '#F6E6CF', '#5A3A22', '#F1C9A0', '#E7F2D8', '#CDE6B0', '#B8473A'],
    'COW': ['#FFFFFF', '#F4F4F8', '#2E2E3A', '#FFB3C1', '#DFF0D2', '#BFE3A8', '#3E7CD9'],
    'DUCK': ['#FFD84A', '#FFF0A8', '#B87510', '#F29A2E', '#D6EEFF', '#A8D8F5', '#6B9E86'],
    'BAT': ['#6A5A8E', '#A394C8', '#2A2145', '#C9A6E8', '#3B2563', '#241447', '#3A2A5E'],
    'GHOST': ['#F6F6FF', '#E2E4F8', '#3A3F66', '#C9CCF0', '#3F3470', '#241A4E', '#B8BCE8'],
    'PUMPKIN': ['#F28A1F', '#FFC76B', '#4A2208', '#FFD34A', '#3A1F5C', '#1E1136', '#3E7C3A'],
    'ROBOT': ['#BBC6D8', '#E9EFF8', '#3A4560', '#5CC8FF', '#D8E4F5', '#B0C4E2', '#6C63FF'],
    'ALIEN': ['#8EDC8A', '#C8F5C4', '#2A6B3E', '#FFE066', '#2A1F5C', '#14103A', '#8E7CC3'],
    'DINO': ['#9ACD5A', '#EAF5C4', '#3F6B22', '#F2A93B', '#E2F5CF', '#BFE39A', '#E07A5F'],
    'PHOENIX': ['#F2762E', '#FFD08A', '#5A1D0F', '#FFB02E', '#3A140E', '#8A2A10', '#B8321F'],
}

VARIANTS = {
    'GOLD': ['#F6C844', '#FFF1B5', '#9A5F0C', '#FFE08A', '#FFF1C0', '#FFD36E', '#B8473A'],
    'JADE': ['#4FBF92', '#C6F0DC', '#1F7A5A', '#FFD36E', '#D2F2E4', '#8ED8B8', '#D9A21B'],
    'RAINBOW': ['#FFF0F6', '#FFFFFF', '#8A5A8E', '#FFB3D1', '#FFE0F0', '#D0E4FF', '#B57CF0'],
    'SAKURA': ['#FFD1DC', '#FFF0F4', '#8A3E55', '#FF9EBB', '#FFE3EE', '#FFC4D8', '#F29BB0'],
    'FROST': ['#DDEBFA', '#FFFFFF', '#3F6A9E', '#A8D4F5', '#D6EEFF', '#A8D2F5', '#5D9BD9'],
    'EMBER': ['#FF7A3A', '#FFD08A', '#5A1D0F', '#FFB347', '#3A140E', '#8A2A10', '#2B1010'],
    'SHADOW': ['#5B5470', '#9A92B5', '#17132B', '#C9A6E8', '#1B1030', '#3A1A5E', '#2A1F45'],
    'CANDY': ['#FFC1E3', '#FFF0FA', '#8A3E70', '#9BE3D0', '#FFE6F5', '#D9F5EA', '#B57CF0'],
    'GALAXY': ['#6A4FD6', '#C9B8FF', '#1B1250', '#FFD36E', '#12093A', '#3A1A8F', '#2A1F7A'],
    'PLATINUM': ['#E4ECFA', '#FFFFFF', '#4A5B8A', '#BBCBF2', '#DDE7FA', '#B5C7EE', '#8EA0D0'],
}

VARIANT_NAMES = ["NORMAL", "GOLD", "MIDNIGHT", "JADE", "RAINBOW", "SAKURA", "FROST", "EMBER", "SHADOW", "CANDY", "GALAXY", "PLATINUM"]


def _lerp_hex(a, b, t):
    a = a.lstrip("#"); b = b.lstrip("#")
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return "#%02X%02X%02X" % tuple(int(round(ca[i] + (cb[i] - ca[i]) * t)) for i in range(3))


def palette(animal, variant="NORMAL"):
    """Paleta {ranura: color} de un animal con una variante (como look() de Android)."""
    base = BASE[animal]
    if variant == "NORMAL":
        cols = base
    elif variant == "MIDNIGHT":
        cols = [_lerp_hex(base[0], "#5B5F9E", 0.55), _lerp_hex(base[1], "#C9CCF5", 0.6), "#1F2150", "#E6E8FF", "#2A2E63", "#454B94", "#6C63FF"]
    else:
        cols = VARIANTS[variant]
    return dict(zip(SLOTS, cols))


def all_palettes(animals):
    """Paletas '@pal' de los animales dados con las 12 variantes: {'avatar.FOX.GOLD': {...}, ...}."""
    out = {}
    for a in animals:
        for v in VARIANT_NAMES:
            out["avatar.%s.%s" % (a, v)] = palette(a, v)
    return out
