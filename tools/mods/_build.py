"""Shared building pieces for the structure mods (all coordinates are template-local)."""
import math

from gen.structures import AIR, stairs

FACE_OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


def ladder(s, x, y0, y1, z, facing):
    """Ladder column; facing is the direction the ladder faces (it hangs on the block behind it)."""
    for y in range(y0, y1 + 1):
        s.set(x, y, z, 'minecraft:ladder[facing=%s,waterlogged=false]' % facing)


def lantern(s, x, y, z, hanging=True, soul=False):
    s.set(x, y, z, 'minecraft:%slantern[hanging=%s,waterlogged=false]' % ('soul_' if soul else '', 'true' if hanging else 'false'))


def torch_wall(s, x, y, z, facing, soul=False):
    s.set(x, y, z, 'minecraft:%swall_torch[facing=%s]' % ('soul_' if soul else '', facing))


def pane(s, x, y, z, block='minecraft:glass_pane', axis='x'):
    if axis == 'x':
        s.set(x, y, z, '%s[east=true,west=true,north=false,south=false,waterlogged=false]' % block)
    else:
        s.set(x, y, z, '%s[north=true,south=true,east=false,west=false,waterlogged=false]' % block)


def bars(s, x, y, z, axis='x'):
    pane(s, x, y, z, 'minecraft:iron_bars', axis)


def fence_line(s, x0, z0, x1, z1, y, block='minecraft:oak_fence'):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            ew = 'true' if x0 != x1 else 'false'
            ns = 'true' if z0 != z1 else 'false'
            s.set(x, y, z, '%s[east=%s,west=%s,north=%s,south=%s,waterlogged=false]' % (block, ew, ew, ns, ns))


def bed(s, x, y, z, facing='south', color='red'):
    """Bed with its foot at (x,y,z) and head one block towards facing."""
    dx, dz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
    s.set(x, y, z, 'minecraft:%s_bed[facing=%s,part=foot,occupied=false]' % (color, facing))
    s.set(x + dx, y, z + dz, 'minecraft:%s_bed[facing=%s,part=head,occupied=false]' % (color, facing))


def door(s, x, y, z, facing, wood='oak', hinge='left', iron=False):
    b = 'minecraft:iron_door' if iron else 'minecraft:%s_door' % wood
    s.set(x, y, z, '%s[facing=%s,half=lower,hinge=%s,open=false,powered=false]' % (b, facing, hinge))
    s.set(x, y + 1, z, '%s[facing=%s,half=upper,hinge=%s,open=false,powered=false]' % (b, facing, hinge))


def trapdoor(s, x, y, z, facing='north', half='top', block='minecraft:iron_trapdoor', open_=False):
    s.set(x, y, z, '%s[facing=%s,half=%s,open=%s,powered=false,waterlogged=false]' % (block, facing, half, 'true' if open_ else 'false'))


def slab(material, half='bottom'):
    return 'minecraft:%s_slab[type=%s,waterlogged=false]' % (material, half)


def gable_roof(s, x0, x1, z0, z1, y, material, fill=None):
    """Stair gable roof running along x, ridge in the middle of z0..z1, starting at height y."""
    depth = (z1 - z0 + 1)
    layers = (depth + 1) // 2
    for i in range(layers):
        zn, zs = z0 + i, z1 - i
        yy = y + i
        for x in range(x0, x1 + 1):
            if zn == zs:
                s.set(x, yy, zn, slab(material))
            else:
                s.set(x, yy, zn, stairs(material, 'south'))
                s.set(x, yy, zs, stairs(material, 'north'))
        if fill:
            for z in range(zn + 1, zs):
                s.set(x0, yy, z, fill)
                s.set(x1, yy, z, fill)


def round_tower(s, cx, cz, r, y0, y1, wall, floor_every=None, floor_block=None, windows='minecraft:glass_pane', inner=AIR):
    """Hollow cylinder from y0..y1 (inclusive); optional floors and window slits."""
    for y in range(y0, y1 + 1):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2)
                if d <= r + 0.4:
                    if d > r - 0.6:
                        s.set(x, y, z, wall() if callable(wall) else wall)
                    else:
                        s.set(x, y, z, inner)
    if floor_every:
        for y in range(y0, y1 + 1, floor_every):
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    if math.sqrt((x - cx) ** 2 + (z - cz) ** 2) <= r - 0.6:
                        s.set(x, y, z, floor_block)
            if windows and y + 2 <= y1:
                for (wx, wz, ax) in ((cx, cz - r, 'x'), (cx, cz + r, 'x'), (cx - r, cz, 'z'), (cx + r, cz, 'z')):
                    pane(s, wx, y + 2, wz, windows, ax)


def battlements(s, x0, z0, x1, z1, y, block):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                s.set(x, y, z, block)
                if (x + z) % 2 == 0:
                    s.set(x, y + 1, z, block)


def tent(s, x0, z0, length, y, wool, pole='minecraft:oak_fence', open_south=True):
    """A-frame tent 5 wide (x0..x0+4) running along z for length blocks."""
    for z in range(z0, z0 + length):
        s.set(x0, y, z, wool)
        s.set(x0 + 4, y, z, wool)
        s.set(x0 + 1, y + 1, z, wool)
        s.set(x0 + 3, y + 1, z, wool)
        s.set(x0 + 2, y + 2, z, wool)
        for x in range(x0 + 1, x0 + 4):
            s.set(x, y, z, AIR)
        s.set(x0 + 2, y + 1, z, AIR)
    # back wall
    zb = z0
    for x in range(x0 + 1, x0 + 4):
        s.set(x, y, zb, wool)
    s.set(x0 + 2, y + 1, zb, wool)
    if not open_south:
        ze = z0 + length - 1
        s.set(x0 + 1, y, ze, wool)
        s.set(x0 + 3, y, ze, wool)


def campfire(s, x, y, z, lit=True, soul=False):
    s.set(x, y, z, 'minecraft:%scampfire[facing=north,lit=%s,signal_fire=false,waterlogged=false]' % ('soul_' if soul else '', 'true' if lit else 'false'))


def scatter(s, x0, z0, x1, z1, y, states, chance, only_on=None):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if s.rng.random() < chance and s.get(x, y, z) in (None, AIR):
                if only_on and s.get(x, y - 1, z) not in only_on:
                    continue
                s.set(x, y, z, s.rng.choice(states))


def pick(s, *weighted):
    """weighted: (state, weight) pairs -> callable choosing one per block."""
    total = sum(w for _, w in weighted)

    def f():
        r = s.rng.random() * total
        for st, w in weighted:
            r -= w
            if r <= 0:
                return st
        return weighted[-1][0]
    return f
