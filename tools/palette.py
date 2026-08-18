#!/usr/bin/env python3
"""Read and write the small PNGs the palette system needs, with no image library.

A palette is an 8 by 1 strip, the same shape vanilla uses for armour trims: the key strip names the
greyscale values a module's artwork is drawn in, and each material's strip gives the colours those
values become. The atlas builds one recoloured sprite per material from that pair.

Used by tools/make-palettes.py. Kept separate because reading a filtered PNG is fiddly enough to be
worth testing on its own.
"""
import struct
import zlib

FILTERS = {0: "none", 1: "sub", 2: "up", 3: "average", 4: "paeth"}


def _chunks(data):
    pos = 8
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8].decode("ascii")
        yield kind, data[pos + 8:pos + 8 + length]
        pos += 12 + length


def _unfilter(raw, width, height, bytes_per_pixel, stride):
    out = []
    previous = bytearray(stride)
    pos = 0
    for _ in range(height):
        kind = raw[pos]
        line = bytearray(raw[pos + 1:pos + 1 + stride])
        pos += 1 + stride
        bpp = bytes_per_pixel
        if kind == 1:
            for i in range(bpp, len(line)):
                line[i] = (line[i] + line[i - bpp]) & 0xFF
        elif kind == 2:
            for i in range(len(line)):
                line[i] = (line[i] + previous[i]) & 0xFF
        elif kind == 3:
            for i in range(len(line)):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + previous[i]) >> 1)) & 0xFF
        elif kind == 4:
            for i in range(len(line)):
                a = line[i - bpp] if i >= bpp else 0
                b = previous[i]
                c = previous[i - bpp] if i >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        previous = line
        out.append(bytes(line))
    return out


def read(path):
    """Return (width, height, pixels) where each pixel is an RGBA tuple."""
    data = open(path, "rb").read()
    width = height = depth = colour = 0
    palette = []
    transparency = []
    idat = b""
    for kind, body in _chunks(data):
        if kind == "IHDR":
            width, height = struct.unpack(">II", body[:8])
            depth, colour = body[8], body[9]
        elif kind == "PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == "tRNS":
            transparency = list(body)
        elif kind == "IDAT":
            idat += body

    if depth not in (1, 2, 4, 8):
        raise ValueError("%s: unsupported bit depth %d" % (path, depth))

    samples = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
    stride = (width * samples * depth + 7) // 8
    bytes_per_pixel = max(1, samples * depth // 8)
    rows = _unfilter(zlib.decompress(idat), width, height, bytes_per_pixel, stride)

    pixels = []
    for line in rows:
        row = []
        for x in range(width):
            if depth == 8:
                chunk = line[x * samples:(x + 1) * samples]
            else:
                per_byte = 8 // depth
                mask = (1 << depth) - 1
                shift = 8 - depth * (x % per_byte + 1)
                chunk = [(line[x // per_byte] >> shift) & mask]

            if colour == 3:
                index = chunk[0]
                r, g, b = palette[index] if index < len(palette) else (0, 0, 0)
                a = transparency[index] if index < len(transparency) else 255
            elif colour == 0:
                r = g = b = chunk[0]
                a = 255
            elif colour == 4:
                r = g = b = chunk[0]
                a = chunk[1]
            elif colour == 2:
                r, g, b = chunk[0], chunk[1], chunk[2]
                a = 255
            else:
                r, g, b, a = chunk[0], chunk[1], chunk[2], chunk[3]
            row.append((r, g, b, a))
        pixels.append(row)
    return width, height, pixels


def write(path, pixels):
    """Write RGBA pixels as an unfiltered 8 bit RGBA PNG."""
    height = len(pixels)
    width = len(pixels[0])
    raw = b""
    for row in pixels:
        raw += b"\x00" + bytes(v for pixel in row for v in pixel)

    def chunk(kind, body):
        return (struct.pack(">I", len(body)) + kind + body
                + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def luminance(pixel):
    r, g, b, _ = pixel
    return int(round(0.2126 * r + 0.7152 * g + 0.0722 * b))
