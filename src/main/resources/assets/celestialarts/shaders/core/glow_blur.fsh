#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 BlurDir;
uniform float Gain;

in vec2 texCoord0;

out vec4 fragColor;

// 9-tap separable gaussian. With BlurDir = 0 it degenerates to a plain (gain-scaled) copy.
void main() {
    vec3 sum = texture(DiffuseSampler, texCoord0).rgb * 0.227027;
    sum += texture(DiffuseSampler, texCoord0 + BlurDir * 1.0).rgb * 0.1945946;
    sum += texture(DiffuseSampler, texCoord0 - BlurDir * 1.0).rgb * 0.1945946;
    sum += texture(DiffuseSampler, texCoord0 + BlurDir * 2.0).rgb * 0.1216216;
    sum += texture(DiffuseSampler, texCoord0 - BlurDir * 2.0).rgb * 0.1216216;
    sum += texture(DiffuseSampler, texCoord0 + BlurDir * 3.0).rgb * 0.054054;
    sum += texture(DiffuseSampler, texCoord0 - BlurDir * 3.0).rgb * 0.054054;
    sum += texture(DiffuseSampler, texCoord0 + BlurDir * 4.0).rgb * 0.016216;
    sum += texture(DiffuseSampler, texCoord0 - BlurDir * 4.0).rgb * 0.016216;
    fragColor = vec4(sum * Gain, 1.0);
}
