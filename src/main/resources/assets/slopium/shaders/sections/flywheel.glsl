layout(std430,binding=4) readonly buffer SlopiumSectionMask { uint slopiumVisible[]; };
layout(std430,binding=5) readonly buffer SlopiumSectionKeys { ivec4 slopiumKeys[]; };
uniform int slopiumMaskEnabled;
uniform uint slopiumHashMask;
uint slopiumHash(ivec3 p){return (uint(p.x)*73856093u ^ uint(p.y)*19349663u ^ uint(p.z)*83492791u)&slopiumHashMask;}
int slopiumSectionVisibility(vec3 world,float radius){
 if(slopiumMaskEnabled==0||isnan(radius)||isinf(radius)||radius<0.||any(isnan(world))||any(isinf(world)))return -1;
 ivec3 p=ivec3(floor(world/16.));vec3 local=world-vec3(p)*16.;
 if(any(lessThan(local-vec3(radius),vec3(-8.)))||any(greaterThan(local+vec3(radius),vec3(24.))))return -1;
 uint h=slopiumHash(p);for(int i=0;i<32;i++){ivec4 key=slopiumKeys[h];if(key.w==0)return -1;if(all(equal(key.xyz,p)))return key.w<0?-1:int(slopiumVisible[key.w-1]);h=(h+1u)&slopiumHashMask;}return -1;
}
