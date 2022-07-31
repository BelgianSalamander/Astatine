#define INT_MIN -2147483648

uniform uint u_height;

layout(local_size_x = 16, local_size_z = 16) in;

layout(std430, binding = 0) readonly buffer BlockData {
    int data[];
} b_blockData;

layout(std430, binding = 1) readonly buffer BiomeData {
    int data[];
} b_biomeData;

layout(std430, binding = 2) writeonly buffer SurfaceDepth {
    int data[];
} b_surfaceDepth;

layout(std430, binding = 3) writeonly buffer SecondaryDepth {
    double data[];
} b_secondaryDepth;

layout(std430, binding = 4) writeonly buffer StoneDepthAbove {
    int data[];
} b_stoneDepthAbove;

layout(std430, binding = 5) writeonly buffer StoneDepthBelow {
    int data[];
} b_stoneDepthBelow;

layout(std430, binding = 6) writeonly buffer WaterHeight {
    int data[];
} b_waterHeight;

const int air = ${air};
const int isFluid[] = ${isFluid};

int calculateSurfaceDepth(uint x, uint z) {
    double noise = ${surfaceNoise}(double(x), 0.0, double(z));
    XoroshiroRandom rand = xoroshiroAt(x, 0, z);

    return int(noise * 2.75 + 3.0 + nextDouble(rand) * 0.25);
}

double calculateSecondaryDepth(uint x, uint z) {
    return ${secondaryNoise}(double(x), 0.0, double(z));
}

void calculateData() {
    int stoneDepthAbove = 0;
    int waterHeight = INT_MIN;

    uint x = gl_GlobalInvocationID.x;
    uint z = gl_GlobalInvocationID.z;

    for (uint y = u_height - 1; y >= 0; y--) {
        uint idx = x * 16 * u_height + y * 16 + z;
        int block = blockData.data[idx];

        if (block == air) {
            stoneDepthAbove = 0;
            waterHeight = INT_MIN;
        } else if ((isFluid[block / 32] & (1 << (block % 32))) != 0) {
            if (waterHeight == INT_MIN) {
                waterHeight = int(y + 1);
            }
        } else {
            stoneDepthAbove++;
        }

        b_stoneDepthAbove.data[idx] = stoneDepthAbove;
        b_waterHeight.data[idx] = waterHeight;
    }

    int stoneDepthBelow = 0;
    for (uint y = 0; y < u_height; y++) {
        uint idx = x * 16 * u_height + y * 16 + z;
        int block = blockData.data[idx];

        if (block == air) {
            stoneDepthBelow = 0;
        } else if ((isFluid[block / 32] & (1 << (block % 32))) != 0) {
            stoneDepthBelow = 0;
        } else {
            stoneDepthBelow++;
        }

        b_stoneDepthBelow.data[idx] = stoneDepthBelow;
    }

    b_surfaceDepth.data[x * 16 + z] = calculateSurfaceDepth(x, z);
    b_secondaryDepth.data[x * 16 + z] = calculateSecondaryDepth(x, z);
}

int main() {
    calculateData();

    return 0;
}