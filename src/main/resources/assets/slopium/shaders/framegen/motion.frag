#version 150
in vec2 uv;
out vec4 color;
uniform sampler2D source;
uniform mat4 currentToPrevious;
void main(){
    float depth=texture(source,uv).r;
    vec4 previous=currentToPrevious*vec4(uv*2.0-1.0,depth*2.0-1.0,1);
    vec2 motion=previous.w>0.0?previous.xy/previous.w*.5+.5-uv:vec2(0);
    color=vec4(motion,0,1);
}
