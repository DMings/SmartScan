precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageOESTexture;

void main() {
    gl_FragColor = texture2D(inputImageOESTexture, textureCoordinate);
}