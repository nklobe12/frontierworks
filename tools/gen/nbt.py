"""Minimal NBT writer (gzip) for structure templates."""
import gzip
import io
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


class Int(int):
    pass


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Float(float):
    pass


class Double(float):
    pass


class List(list):
    def __init__(self, tag, items=()):
        super().__init__(items)
        self.tag = tag


def _type(v):
    if isinstance(v, Byte):
        return BYTE
    if isinstance(v, Short):
        return SHORT
    if isinstance(v, Long):
        return LONG
    if isinstance(v, Float):
        return FLOAT
    if isinstance(v, Double):
        return DOUBLE
    if isinstance(v, bool):
        return BYTE
    if isinstance(v, int):
        return INT
    if isinstance(v, float):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, List):
        return LIST
    if isinstance(v, list):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError('unsupported NBT value %r' % (v,))


def _str(out, s):
    b = s.encode('utf-8')
    out.write(struct.pack('>H', len(b)))
    out.write(b)


def _payload(out, t, v):
    if t == BYTE:
        out.write(struct.pack('>b', int(v)))
    elif t == SHORT:
        out.write(struct.pack('>h', v))
    elif t == INT:
        out.write(struct.pack('>i', v))
    elif t == LONG:
        out.write(struct.pack('>q', v))
    elif t == FLOAT:
        out.write(struct.pack('>f', v))
    elif t == DOUBLE:
        out.write(struct.pack('>d', v))
    elif t == STRING:
        _str(out, v)
    elif t == LIST:
        et = v.tag if isinstance(v, List) else (_type(v[0]) if v else END)
        out.write(struct.pack('>b', et))
        out.write(struct.pack('>i', len(v)))
        for item in v:
            _payload(out, et, item)
    elif t == COMPOUND:
        for k, item in v.items():
            it = _type(item)
            out.write(struct.pack('>b', it))
            _str(out, k)
            _payload(out, it, item)
        out.write(struct.pack('>b', END))
    else:
        raise TypeError(t)


def dumps(root):
    out = io.BytesIO()
    out.write(struct.pack('>b', COMPOUND))
    _str(out, '')
    _payload(out, COMPOUND, root)
    return gzip.compress(out.getvalue(), mtime=0)
