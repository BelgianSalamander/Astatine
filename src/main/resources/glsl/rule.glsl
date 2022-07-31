layout(local_size_x = 16, local_size_y = ${localSizeY}, local_size_z = 16) in;

uniform int low_y;
uniform uint u_height;

layout(std430, binding = 0) readonly buffer BlockData {
    int data[];
} b_blockData;

layout(std430, binding = 1) readonly buffer BiomeData {
    int data[];
} b_biomeData;

layout(std430, binding = 2) readonly buffer SurfaceDepth {
    int data[];
} b_surfaceDepth;

layout(std430, binding = 3) readonly buffer SecondaryDepth {
    double data[];
} b_secondaryDepth;

layout(std430, binding = 4) readonly buffer StoneDepthAbove {
    int data[];
} b_stoneDepthAbove;

layout(std430, binding = 5) readonly buffer StoneDepthBelow {
    int data[];
} b_stoneDepthBelow;

layout(std430, binding = 6) readonly buffer WaterHeight {
    int data[];
} b_waterHeight;

layout(std430, binding = 7) readonly buffer MinSurfaceLevel {
    int data[];
} b_minSurfaceLevel;

layout(std430, binding = 8) readonly buffer Heightmap {
    int data[];
} b_heightmap;

int block_x;
int block_y;
int block_z;

${define};

int main() {
    block_x = gl_GlobalInvocationID.x;
    block_y = gl_GlobalInvocationID.y + low_y;
    block_z = gl_GlobalInvocationID.z;

    uint idx = 16 * u_height * (block_x & 0xf) + 16 * (block_y - low_y) + (block_z & 0xf);

    if (b_blockData.data[idx] == ${defaultBlock}) {
        int result = ${rule}();

        if (result != -1) {
            b_blockData.data[idx] = result;
        }
    }
}