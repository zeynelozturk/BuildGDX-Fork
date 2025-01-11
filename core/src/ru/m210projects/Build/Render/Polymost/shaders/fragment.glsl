uniform sampler2D u_texture;
uniform sampler2D u_palette;
uniform sampler2D u_palookup;
uniform int u_shade;
uniform int u_numshades;
uniform float u_visibility;
uniform float u_alpha;
uniform int u_draw255;

//uniform int u_fogenable;
//uniform vec4 u_fogcolour;
//uniform float u_fogstart;
//uniform float u_fogend;
//uniform float u_cx1;
//uniform float u_cy1;
//uniform float u_cx2;
//uniform float u_cy2;

varying float v_dist;
varying vec2 v_texCoords;

//float fog(float dist) {
//	if(u_fogenable == 1) {
//        return clamp(1.0 - (u_fogend - dist) / (u_fogend - u_fogstart), 0.0, 1.0);
//    }
//    return 0.0;
//}

float getpalookup(int dashade) {
    float davis = v_dist * u_visibility;
//    if(u_fogenable != -1) {
//        davis = u_visibility / 64.0;
//    }
    float shade = (min(max(float(dashade) + davis, 0.0), float(u_numshades - 1)));
    return shade / 64.0;
}

void main() {
//    if(gl_FragCoord.x < u_cx1 || gl_FragCoord.x > u_cx2 || gl_FragCoord.y > u_cy1 || gl_FragCoord.y < u_cy2) {
//        discard;
//    }

    float fi = texture2D(u_texture, v_texCoords).r;
    if (fi == 1.0) {
        if (u_draw255 == 0)
            discard;
        fi -= 0.5 / 256.0;
    }

    float index = texture2D(u_palookup, vec2(fi, getpalookup(u_shade))).r;
    if (index == 1.0) {
        index -= 0.5 / 256.0;
    }

    vec4 src = vec4(texture2D(u_palette, vec2(index, 0.0)).rgb, u_alpha);
//    if(u_fogenable == -1) {
        gl_FragColor = src;
//    } else {
//        gl_FragColor = mix(src, u_fogcolour, fog(v_dist));
//    }
}
