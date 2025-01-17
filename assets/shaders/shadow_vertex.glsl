#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

attribute vec3 a_position;
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_lightTrans;

varying vec4 v_positionLightTrans;
varying vec4 v_position;

attribute vec3 a_normal;
uniform mat3 u_normalMatrix;
varying vec3 v_normal;


void main()
{

    vec3 normal = normalize(u_normalMatrix * a_normal);
    v_normal = normal;

    // Vertex position after transformation
    v_position = u_worldTrans * vec4(a_position, 1.0);
    // Vertex position in the light perspective
    v_positionLightTrans = u_lightTrans * v_position;
    gl_Position = u_projViewTrans * v_position;
}
