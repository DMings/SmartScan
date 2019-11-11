#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 textureCoordinate;
uniform samplerExternalOES inputImageOESTexture;

void main() {
    gl_FragColor = texture2D(inputImageOESTexture, textureCoordinate);
}