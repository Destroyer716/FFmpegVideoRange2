attribute vec4 av_Position;
attribute vec2 af_Position;
varying vec2 v_texPosition;
void main() {
    v_texPosition = af_Position;
    gl_Position = av_Position;
}
