#version 110

attribute vec4 a_position;
uniform mat4 u_projTrans;
uniform mat4 u_transform;
uniform bool u_mirror;
varying vec4 v_pos;

void main()
{
    v_pos = u_transform * a_position;
    gl_Position = u_projTrans * v_pos;
    if(u_mirror) {
        gl_Position.x *= -1.0;
    }
}