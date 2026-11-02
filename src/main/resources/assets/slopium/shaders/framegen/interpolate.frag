#version 150
in vec2 uv;
out vec4 color;
uniform sampler2D previousColor,currentColor,flow,fsrMiddle;
uniform vec2 size;
uniform float consistencyPixels;
uniform float fraction;
uniform int useFsrMiddle,protectImage,hasDepth;
uniform sampler2D previousDepth,currentDepth;
uniform mat4 currentToPrevious,previousToCurrent;
vec2 geometric(vec2 at,bool ahead){
    float d=ahead?texture(previousDepth,at).r:texture(currentDepth,at).r;
    vec4 q=(ahead?previousToCurrent:currentToPrevious)*vec4(at*2.0-1.0,d*2.0-1.0,1);
    return q.w>0.0?q.xy/q.w*.5+.5-at:vec2(0);
}
float mismatch(vec2 at,vec2 motion,bool ahead){
    vec2 other=at+motion;
    if(any(lessThan(other,vec2(0)))||any(greaterThan(other,vec2(1))))return 100.0;
    vec3 a=ahead?texture(previousColor,at).rgb:texture(currentColor,at).rgb;
    vec3 b=ahead?texture(currentColor,other).rgb:texture(previousColor,other).rgb;
    return dot(a-b,a-b);
}
vec2 vectorAt(vec2 at,bool ahead){
    if(protectImage==0)return ahead?texture(flow,at).ba:texture(flow,at).rg;
    // Never blend foreground and background velocities across a motion-grid edge.
    ivec2 cell=clamp(ivec2(at*vec2(textureSize(flow,0))),ivec2(0),textureSize(flow,0)-1);
    vec4 sampleFlow=texelFetch(flow,cell,0);
    vec2 motion=ahead?sampleFlow.ba:sampleFlow.rg;
    if(hasDepth!=0){
        vec2 camera=geometric(at,ahead);
        // Static geometry gets exact per-pixel camera motion, including thin silhouettes.
        if(mismatch(at,camera,ahead)<=mismatch(at,motion,ahead)+.001)motion=camera;
    }
    return motion;
}
vec4 safeEndpoint(){return fraction<.5?texture(previousColor,uv):texture(currentColor,uv);}
void main() {
    vec2 p=uv,c=uv;
    for(int i=0;i<6;i++){p=uv-fraction*vectorAt(p,true);c=uv-(1.0-fraction)*vectorAt(c,false);}
    vec2 f=vectorAt(p,true),b=vectorAt(c,false);
    bool valid=all(greaterThanEqual(p,vec2(0)))&&all(lessThanEqual(p,vec2(1)))
        &&all(greaterThanEqual(c,vec2(0)))&&all(lessThanEqual(c,vec2(1)));
    float difference=length((f+b)*size);
    // At disocclusions use the current sample, rather than double-exposing unrelated edges.
    if(protectImage!=0){
        float residual=max(length((p+fraction*f-uv)*size),length((c+(1.0-fraction)*b-uv)*size));
        vec3 a=texture(previousColor,p).rgb,z=texture(currentColor,c).rgb;
        if(!valid||difference>consistencyPixels||residual>consistencyPixels||dot(a-z,a-z)>.16){color=safeEndpoint();return;}
    }
    if(!valid||difference>consistencyPixels)color=texture(currentColor,uv);
    else if(useFsrMiddle!=0) {
        // FSR 3 supplies t=0.5. Slopium adds phases on both sides using the shared motion field.
        vec4 middle=texture(fsrMiddle,p+.5*f);
        color=fraction<.5?mix(texture(previousColor,p),middle,2.0*fraction)
            :mix(middle,texture(currentColor,c),2.0*fraction-1.0);
    } else color=mix(texture(previousColor,p),texture(currentColor,c),fraction);
}
