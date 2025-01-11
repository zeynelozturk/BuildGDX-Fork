#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_modelView;
uniform mat4 u_projTrans;
uniform mat4 u_transform;
uniform mat3 u_texture_transform;
uniform bool u_mirror;

varying LOWP float v_dist;
varying vec2 v_texCoords;
varying vec4 v_color;

void main() {
    v_texCoords = vec2(u_texture_transform * vec3(a_texCoord0, 1.0));
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    vec4 mv = u_modelView * u_transform  * a_position;
    gl_Position = u_projTrans * u_transform * a_position;
    if (u_mirror) {
        gl_Position.x *= -1.0;
    }
    v_dist = mv.z / mv.w;
}