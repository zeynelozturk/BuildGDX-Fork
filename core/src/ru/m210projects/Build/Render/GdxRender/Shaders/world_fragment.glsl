#ifdef GL_ES
    precision mediump float;
    precision mediump int;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_palette;
uniform sampler2D u_palookup;
uniform int u_numshades;
uniform float u_visibility;
uniform int u_shade;
uniform bool u_draw255;
uniform float u_alpha;

varying float v_dist;
varying vec2 v_texCoords;

uniform mat4 u_invProjectionView;
uniform vec4 u_plane[2];
uniform vec4 u_viewport;
uniform int u_planeClipping;
uniform ivec2 u_texSize;
uniform bool u_paletteFiltering;
uniform bool u_softShading;

const float LOOKUP_SIZE = 64.0;
const float INDEX_MAX = 1.0 - 0.5 / 256.0;

const float ALPHA_CUT_DISABLE = 0.01;

float getpalookup(int dashade) {
    float davis = v_dist * u_visibility;
    float shade = (min(max(float(dashade) + davis, 0.0), float(u_numshades - 1)));
    return shade / LOOKUP_SIZE;
}

vec4 getPos() {
    vec4 ndc;
    vec2 xy = gl_FragCoord.xy - vec2(u_viewport.xy);    ndc.xy = (2.0 * xy) / u_viewport.zw - 1.0;
    ndc.z = (2.0 * gl_FragCoord.z) - 1.0;
    ndc.w = 1.0;

    vec4 worldCoords = u_invProjectionView * ndc;
    worldCoords.xyz /= worldCoords.w;
    worldCoords.xyz *= vec3(512.0, 512.0, 8192.0); // Engine coords scale
    worldCoords.w = 1.0;
    return worldCoords;
}

bool isvisible() {
    vec4 pos = getPos();
    for(int i = 0; i < 2; i++) {
        if(dot(u_plane[i], pos) < 0.0)
        return false;
    }
    return true;
}

vec4 textureSample(vec2 uv) {
    float fi = texture2D(u_texture, uv).r;
    if(fi == 1.0) {
        if (!u_draw255) {
            return vec4(0.00);
        }
        fi -= 0.5 / 256.0;
    }

    float lookup = getpalookup(u_shade);

    if (u_softShading) {
        // bilinear blending between closest shade colors
        float blendWeight = fract(lookup * LOOKUP_SIZE);

        float lookupA = floor(lookup * LOOKUP_SIZE) / LOOKUP_SIZE;
        float lookupB = ceil(lookup * LOOKUP_SIZE) / LOOKUP_SIZE;

        float indexA = min(texture2D(u_palookup, vec2(fi, lookupA)).r, INDEX_MAX);
        float indexB = min(texture2D(u_palookup, vec2(fi, lookupB)).r, INDEX_MAX);

        vec4 colourA = vec4(texture2D(u_palette, vec2(indexA, 0.0)).rgb, 1.0);
        vec4 colourB = vec4(texture2D(u_palette, vec2(indexB, 0.0)).rgb, 1.0);

        return mix(colourA, colourB, blendWeight);
    }

    float index = min(texture2D(u_palookup, vec2(fi, lookup)).r, INDEX_MAX);
    return vec4(texture2D(u_palette, vec2(index, 0.0)).rgb, 1.0);
}

vec4 textureBilinearSample(vec2 uv) {
    vec2 texSize = vec2(u_texSize);
    vec2 texCoord = uv * texSize - 0.5;

    vec2 base = floor(texCoord);
    vec2 frac = fract(texCoord);

    vec2 p0 = (base + vec2(0.5, 0.5)) / texSize;
    vec2 p1 = (base + vec2(1.5, 0.5)) / texSize;
    vec2 p2 = (base + vec2(0.5, 1.5)) / texSize;
    vec2 p3 = (base + vec2(1.5, 1.5)) / texSize;

    vec4 c0 = textureSample(p0);
    vec4 c1 = textureSample(p1);
    vec4 c2 = textureSample(p2);
    vec4 c3 = textureSample(p3);

    vec4 x0 = mix(c0, c1, frac.x);
    vec4 x1 = mix(c2, c3, frac.x);
    return mix(x0, x1, frac.y);
}

void main() {
    if((u_planeClipping == 1 && !isvisible())) {
        discard;
    }

    if(u_planeClipping == 2 &&
    (gl_FragCoord.x < u_viewport.x || gl_FragCoord.x > u_viewport.z
    || gl_FragCoord.y < u_viewport.w || gl_FragCoord.y > u_viewport.y))	{
        discard;
    }

    if (u_alpha == ALPHA_CUT_DISABLE) { // ROR or mirror
        gl_FragColor = vec4(0.);
        return;
    }

    vec4 colour;
    if (u_paletteFiltering)
        colour = textureBilinearSample(v_texCoords);
    else
        colour = textureSample(v_texCoords);

    if (colour.a < 0.33) // Transparent bit2
        discard;

    gl_FragColor = vec4(colour.xyz / colour.a, colour.a * u_alpha);
}