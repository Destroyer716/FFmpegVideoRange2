precision mediump float;
varying vec2 v_texPosition;
uniform sampler2D sampler_y;
uniform sampler2D sampler_u;
uniform sampler2D sampler_v;
void main() {

    mediump vec3 yuv;
    lowp vec3 rgb;
    yuv.x = texture2D(sampler_y, v_texPosition).r - 0.0625;
    yuv.y = texture2D(sampler_u, v_texPosition).r - 0.5;
    yuv.z = texture2D(sampler_v, v_texPosition).r - 0.5;

    rgb = mat3( 1,1,1,
    0,-0.39465,2.03211,
    1.13983,-0.58060,0) * yuv;


    //float y,u,v;
    //y = texture2D(sampler_y,v_texPosition).r - 0.0625;
    //u = texture2D(sampler_u,v_texPosition).r- 0.5;
    //v = texture2D(sampler_v,v_texPosition).r- 0.5;


    //vec3 rgb;
    //rgb.r = y + 1.403 * v;
    //rgb.g = y - 0.344 * u - 0.714 * v;
    //rgb.b = y + 1.770 * u;

    gl_FragColor = vec4(rgb,1);
}
