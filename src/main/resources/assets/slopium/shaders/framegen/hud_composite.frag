#version 150
in vec2 uv;
out vec4 color;
uniform sampler2D world,hud;
void main(){vec4 ui=texture(hud,uv);color=vec4(texture(world,uv).rgb*(1.0-ui.a)+ui.rgb,1.0);}
