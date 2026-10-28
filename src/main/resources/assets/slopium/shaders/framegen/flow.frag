#version 150
in vec2 uv;
out vec4 color;
uniform sampler2D previousColor,currentColor,previousDepth,currentDepth;
uniform mat4 currentToPrevious,previousToCurrent;
uniform vec2 size;
uniform float searchPixels;
vec2 cameraMotion(sampler2D depth,mat4 transform,vec2 p) {
    float d=texture(depth,p).r;
    vec4 q=transform*vec4(p*2.0-1.0,d*2.0-1.0,1.0);
    if(q.w<=0.0)return vec2(0);
    return q.xy/q.w*.5+.5-p;
}
float errorAt(sampler2D a,sampler2D b,vec2 p,vec2 motion,vec2 prior) {
    vec2 q=p+motion;
    if(any(lessThan(q,vec2(0)))||any(greaterThan(q,vec2(1))))return 100.0;
    float cost=0.0;
    for(int i=0;i<5;i++) {
        vec2 o=i==0?vec2(0):i==1?vec2(2,0):i==2?vec2(-2,0):i==3?vec2(0,2):vec2(0,-2);
        vec3 d=texture(a,p+o/size).rgb-texture(b,q+o/size).rgb;
        cost+=dot(d,d);
    }
    vec2 residual=(motion-prior)*size;
    return cost+dot(residual,residual)*0.00001;
}
vec2 estimate(sampler2D a,sampler2D b,sampler2D depth,mat4 transform) {
    vec2 prior=cameraMotion(depth,transform,uv);
    vec2 best=prior;
    float cost=errorAt(a,b,uv,best,prior);
    // Hierarchical bounded search; a stationary patch prefers its camera-derived vector.
    for(float stepSize=searchPixels*.5;stepSize>=1.0;stepSize*=.5) {
        vec2 center=best;
        for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++) {
            vec2 candidate=center+vec2(x,y)*stepSize/size;
            float e=errorAt(a,b,uv,candidate,prior);
            if(e<cost){cost=e;best=candidate;}
        }
    }
    return best;
}
void main() {
    color=vec4(estimate(currentColor,previousColor,currentDepth,currentToPrevious),
               estimate(previousColor,currentColor,previousDepth,previousToCurrent));
}
