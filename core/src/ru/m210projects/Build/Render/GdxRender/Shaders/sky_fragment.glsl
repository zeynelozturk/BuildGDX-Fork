#version 110

#ifdef GL_ES
    precision mediump float;
    precision mediump int;
#endif

uniform sampler2D u_texture;
uniform sampler2D u_palette;
uniform sampler2D u_palookup;

uniform int u_numshades;
uniform int u_shade;
uniform float u_alpha;
uniform vec3 u_camera;
uniform ivec2 u_texSize;
uniform bool u_paletteFiltering;

varying vec4 v_pos;

const float PI = 3.1415926538;

float getpalookup(int dashade) {
    float shade = (min(max(float(dashade), 0.0), float(u_numshades - 1)));
    return shade / 64.0;
}

vec4 textureSample(vec2 uv) {
    float fi = texture2D(u_texture, uv).r;
    if(fi == 1.0) {
        fi -= 0.5 / 256.0;
    }

    float index = texture2D(u_palookup, vec2(fi, getpalookup(u_shade))).r;
    if (index == 1.0) {
        index -= 0.5 / 256.0;
    }

    return vec4(texture2D(u_palette, vec2(index, 0.0)).rgb, u_alpha);
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

void main()
{
    vec4 pix = normalize(v_pos - vec4(u_camera, 1.0));
    vec2 uv = vec2((atan(pix.y, pix.x) + PI) / (2.0 * PI), pix.z / 2.0);
    uv = uv + 0.5;

    float fi = texture2D(u_texture, uv).r;
    if (fi == 1.0) {
        fi -= 0.5 / 256.0;
    }

    float index = texture2D(u_palookup, vec2(fi, getpalookup(u_shade))).r;
    if (index == 1.0) {
        index -= 0.5 / 256.0;
    }

    vec4 colour;
    if (u_paletteFiltering)
        colour = textureBilinearSample(uv);
    else
        colour = textureSample(uv);

    gl_FragColor = colour;
}