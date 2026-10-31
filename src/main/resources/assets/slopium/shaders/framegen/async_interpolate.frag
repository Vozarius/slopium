#version 150
in vec2 uv;
out vec4 color;
uniform sampler2D previousColor,currentColor,flow,fsrMiddle;
uniform vec2 size;
uniform float consistencyPixels;
uniform float fraction;
uniform int useFsrMiddle;
void main() {
    vec2 p=uv,c=uv;
    for(int i=0;i<3;i++){p=uv-fraction*texture(flow,p).ba;c=uv-(1.0-fraction)*texture(flow,c).rg;}
    vec2 f=texture(flow,p).ba,b=texture(flow,c).rg;
    bool valid=all(greaterThanEqual(p,vec2(0)))&&all(lessThanEqual(p,vec2(1)))
        &&all(greaterThanEqual(c,vec2(0)))&&all(lessThanEqual(c,vec2(1)));
    float difference=length((f+b)*size);
    // Uncertain pixels transition in time; never jump straight to the new endpoint at t=0.
    if(!valid||difference>consistencyPixels)color=mix(texture(previousColor,uv),texture(currentColor,uv),fraction);
    else if(useFsrMiddle!=0) {
        // FSR 3 supplies t=0.5. Slopium adds phases on both sides using the shared motion field.
        vec4 middle=texture(fsrMiddle,p+.5*f);
        color=fraction<.5?mix(texture(previousColor,p),middle,2.0*fraction)
            :mix(middle,texture(currentColor,c),2.0*fraction-1.0);
    } else color=mix(texture(previousColor,p),texture(currentColor,c),fraction);
}
