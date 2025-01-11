#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP float v_dist;
varying vec2 v_texCoords;

void main(){
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_ClipVertex = gl_ModelViewMatrix * gl_Vertex;
    v_dist = gl_ClipVertex.z / gl_ClipVertex.w;
    gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
    v_texCoords = gl_TexCoord[0].xy;
}