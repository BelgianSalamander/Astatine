#version 450 core

//Math
double inverseMix(double a, double b, double t) {
    return (a - b) / (t - b);
}

double map(double d, double e, double f, double g, double h) {
    return mix(g, h, inverseMix(d, e, f));
}

//RANDOM

struct ULong {
    uint lo;
    uint hi;
};

struct Long {
    uint lo;
    int hi;
};

ULong add(ULong a, ULong b) {
    ULong c;
    c.lo = a.lo + b.lo;
    c.hi = a.hi + b.hi + (c.lo < a.lo);
    return c;
}

ULong rotateLeft(ULong i, int distance) {
    ULong c = i;

    distance = distance % 64;

    c.lo = (c.lo << distance) | (c.hi >> (32 - distance));
    c.hi = (c.hi << distance) | (c.lo >> (32 - distance));

    return c;
}

ULong shiftLeft(ULong i, int distance) {
    ULong c = i;

    c.lo = (c.lo << distance) | (c.hi >> (32 - distance));
    c.hi = (c.hi << distance);

    return c;
}

ULong xor(ULong a, ULong b) {
    return ULong(a.lo ^ b.lo, a.hi ^ b.hi);
}

struct XoroshiroRandom {
    ULong seedLo;
    ULong seedHi;
};

XoroshiroRandom newXoroshiro(ULong l, ULong m) {
    XoroshiroRandom r;
    r.seedLo = l;
    r.seedHi = m;

    //If all zero
    if (r.seedLo.lo == 0 && r.seedLo.hi == 0 && r.seedHi.lo == 0 && r.seedHi.hi == 0) {
        r.seedLo = ULong(2135587861, 2654435769);
        r.seedHi = ULong(4089235721, 1779033703);
    }

    return r;
}

XoroshiroRandom xoroshiroAt(int x, int y, int z) {
    ULong l = ULong(x * 32426764 ^ (y >> 11), y);
    ULong m = ULong(z ^ (z * 452645263 + x), z ^ y ^ x);

    return newXoroshiro(l, m);
}

ULong nextULong(out XoroshiroRandom rand) {
    ULong l = rand.seedLo;
    ULong m = rand.seedHi;

    ULong n = add(rotateLeft(add(l, m), 17), l);

    m = xor(m, l);

    rand.seedLo = xor(xor(rotateLeft(l, 21), m, leftShift(m, 21)));
    rand.seedHi = rotateLeft(m, 5);

    return n;
}

int nextInt(out XoroshiroRandom rand) {
    //Kind of a yucky hack
    return floatBitsToInt(uintBitsToFloat(nextULong(rand).lo));
}

ULong nextBits(int bits, out XoroshiroRandom rand) {
    ULong n = nextULong(rand);
    int shift = 64 - bits;

    ULong r;
    r.hi = n.hi >> shift;
    r.lo = n.lo >> shift;
    r.lo = r.lo | (n.hi << (32 - shift));

    return r;
}

float nextFloat(out XoroshiroRandom rand) {
    ULong bits = nextBits(24, rand);

    return bits.lo * 5.9604645E-8F;
}

double nextDouble(out XoroshiroRandom rand) {
    ULong bits = nextBits(53, rand);

    double mul = 1.1102230246251565E-16;
    double factor = double(1 << 16);
    factor *= factor; //2^32

    return bits.lo * mul + bits.hi * mul * factor;
}

//NOISE
const ivec3 GRADIENT = {
    ivec3(1, 1, 0),
    ivec3(-1, 1, 0),
    ivec3(1, -1, 0),
    ivec3(-1, -1, 0),
    ivec3(1, 0, 1),
    ivec3(-1, 0, 1),
    ivec3(1, 0, -1),
    ivec3(-1, 0, -1),
    ivec3(0, 1, 1),
    ivec3(0, -1, 1),
    ivec3(0, 1, -1),
    ivec3(0, -1, -1),
    ivec3(1, 1, 0),
    ivec3(0, -1, 1),
    ivec3(-1, 1, 0),
    ivec3(0, -1, -1)
};

struct ImprovedNoise {
    int perm[256 / 4]; //256 bits
    double xo, yo, zo;
};

int p(ImprovedNoise source, int index) {
    int arrayIdx = index / 4;
    int byteIdx = index % 4;

    int value = source.perm[arrayIdx];
    value = value >> (byteIdx * 8);
    value = value & 0xFF;

    return value;
}

double gradDot(ImprovedNoise source, int gradIndex, double x, double y, double z) {
    return dot(GRADIENT[p(source, gradIndex)], ivec3(x, y, z));
}

double minecraftSmoothstep(double x) {
    return x * x * x * (x * (x * 6 - 15) + 10);
}

double sampleAndLerp(ImprovedNoise source, int gridX, int gridY, int gridZ, double deltaX, double weirdDeltaY, double deltaZ, double deltaY) {
    int i = p(source, gridX);
    int j = p(source, gridX + 1);
    int k = p(source, i + gridY);
    int l = p(source, i + gridY + 1);
    int m = p(source, j + gridY);
    int n = p(source, j + gridY + 1);

    double d = gradDot(source, p(source, k + gridZ), deltaX, weirdDeltaY, deltaZ);
    double e = gradDot(source, p(source, m + gridZ), deltaX - 1.0, weirdDeltaY, deltaZ);
    double f = gradDot(source, p(source, l + gridZ), deltaX, weirdDeltaY - 1.0, deltaZ);
    double g = gradDot(source, p(source, n + gridZ), deltaX - 1.0, weirdDeltaY - 1.0, deltaZ);
    double h = gradDot(source, p(source, k + gridZ + 1), deltaX, weirdDeltaY, deltaZ - 1.0);
    double o = gradDot(source, p(source, m + gridZ + 1), deltaX - 1.0, weirdDeltaY, deltaZ - 1.0);
    double p = gradDot(source, p(source, l + gridZ + 1), deltaX, weirdDeltaY - 1.0, deltaZ - 1.0);
    double q = gradDot(source, p(source, n + gridZ + 1), deltaX - 1.0, weirdDeltaY - 1.0, deltaZ - 1.0);

    double tx = minecraftSmoothstep(deltaX);
    double ty = minecraftSmoothstep(deltaY);
    double tz = minecraftSmoothstep(deltaZ);

    return mix(
        mix(
            mix(d, e, tx),
            mix(f, g, tx),
            ty
        ),
        mix(
            mix(h, o, tx),
            mix(p, q, tx),
            ty
        ),
        tz
    );
}

double noise(ImprovedNoise source, double x, double y, double z, double yScale, double yMax) {
    double shifted_x = x + source.xo;
    double shifted_y = y + source.yo;
    double shifted_z = z + source.zo;

    int xi = int(floor(shifted_x));
    int yi = int(floor(shifted_y));
    int zi = int(floor(shifted_z));

    double xf = shifted_x - xi;
    double yf = shifted_y - yi;
    double zf = shifted_z - zi;

    double n = 0.0;
    if (yScale != 0.0) {
        double m;
        if (yMax >= 0.0 && yMax < yf) {
            m = yMax;
        } else {
            m = yf;
        }

        n = floor(m / yScale + + 1.0000000116860974E-7) * yScale;
    }

    return sampleAndLerp(source, xi, yi, zi, xf, yf - n, zf, yf);
}

double wrap(double x) {
    return value - Mth.lfloor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
}