// Slopium's original OpenGL/D3D12 bridge. AMD's runtime is distributed under its own license.
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <wrl/client.h>
#include <jni.h>
#include <ffx_api.h>
#include <dx12/ffx_api_dx12.h>
#include <ffx_framegeneration.h>
#include <memory>
#include <string>
#include <stdexcept>
#include <vector>
#include <cstring>
using Microsoft::WRL::ComPtr;
static void hr(HRESULT result,const char* where){if(FAILED(result))throw std::runtime_error(std::string(where)+": HRESULT "+std::to_string((unsigned long)result));}
static void ffx(ffxReturnCode_t result,const char* where){if(result)throw std::runtime_error(std::string(where)+": FSR code "+std::to_string(result));}
struct Session {
    ComPtr<ID3D12Device> device;
    ComPtr<ID3D12CommandQueue> queue;
    ComPtr<ID3D12Fence> fence;
    ComPtr<ID3D12CommandAllocator> allocator[3];
    ComPtr<ID3D12GraphicsCommandList> list;
    ComPtr<ID3D12Resource> images[4];
    HANDLE handles[5]{};
    UINT64 sizes[4]{},completedSlots[3]{},lastSignal=0;
    UINT width,height;
    HMODULE library=nullptr;
    ffxContext context=nullptr;
    PfnFfxCreateContext create=nullptr;
    PfnFfxDestroyContext destroy=nullptr;
    PfnFfxConfigure configure=nullptr;
    PfnFfxDispatch dispatch=nullptr;
    PfnFfxQuery query=nullptr;
    std::string provider;
    void wait(UINT64 value) {
        if(!value||fence->GetCompletedValue()>=value)return;
        HANDLE event=CreateEventW(nullptr,FALSE,FALSE,nullptr);
        if(!event)throw std::runtime_error("Cannot create fence event");
        HRESULT result=fence->SetEventOnCompletion(value,event);
        DWORD state=SUCCEEDED(result)?WaitForSingleObject(event,5000):WAIT_FAILED;
        CloseHandle(event);if(state!=WAIT_OBJECT_0)throw std::runtime_error("FSR GPU fence timeout");
    }
    ~Session(){
        try{if(fence&&lastSignal)wait(lastSignal);}catch(...){}
        if(context&&destroy)destroy(&context,nullptr);
        for(auto h:handles)if(h)CloseHandle(h);
        if(library)FreeLibrary(library);
    }
    void init(UINT w,UINT h,UINT64 luid,const wchar_t* path) {
        width=w;height=h;
        ComPtr<IDXGIFactory4> factory;hr(CreateDXGIFactory1(IID_PPV_ARGS(&factory)),"DXGI factory");
        LUID id{};std::memcpy(&id,&luid,8);ComPtr<IDXGIAdapter1> adapter;
        hr(factory->EnumAdapterByLuid(id,IID_PPV_ARGS(&adapter)),"Match OpenGL adapter");
        hr(D3D12CreateDevice(adapter.Get(),D3D_FEATURE_LEVEL_12_0,IID_PPV_ARGS(&device)),"D3D12 device");
        D3D12_COMMAND_QUEUE_DESC q{};q.Type=D3D12_COMMAND_LIST_TYPE_DIRECT;
        hr(device->CreateCommandQueue(&q,IID_PPV_ARGS(&queue)),"Command queue");
        hr(device->CreateFence(0,D3D12_FENCE_FLAG_SHARED,IID_PPV_ARGS(&fence)),"Shared fence");
        hr(device->CreateSharedHandle(fence.Get(),nullptr,GENERIC_ALL,nullptr,&handles[4]),"Fence handle");
        for(auto& a:allocator)hr(device->CreateCommandAllocator(q.Type,IID_PPV_ARGS(&a)),"Command allocator");
        hr(device->CreateCommandList(0,q.Type,allocator[0].Get(),nullptr,IID_PPV_ARGS(&list)),"Command list");hr(list->Close(),"Initial close");
        DXGI_FORMAT formats[]={DXGI_FORMAT_R8G8B8A8_UNORM,DXGI_FORMAT_R32_FLOAT,DXGI_FORMAT_R16G16_FLOAT,DXGI_FORMAT_R8G8B8A8_UNORM};
        for(int i=0;i<4;i++) {
            D3D12_HEAP_PROPERTIES heap{};heap.Type=D3D12_HEAP_TYPE_DEFAULT;
            D3D12_RESOURCE_DESC d{};d.Dimension=D3D12_RESOURCE_DIMENSION_TEXTURE2D;d.Width=w;d.Height=h;
            d.DepthOrArraySize=1;d.MipLevels=1;d.Format=formats[i];d.SampleDesc.Count=1;
            d.Flags=i==3?D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS:D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;
            sizes[i]=device->GetResourceAllocationInfo(0,1,&d).SizeInBytes;
            hr(device->CreateCommittedResource(&heap,D3D12_HEAP_FLAG_SHARED,&d,D3D12_RESOURCE_STATE_COMMON,nullptr,IID_PPV_ARGS(&images[i])),"Shared texture");
            hr(device->CreateSharedHandle(images[i].Get(),nullptr,GENERIC_ALL,nullptr,&handles[i]),"Texture handle");
        }
        library=LoadLibraryExW(path,nullptr,LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR|LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
        if(!library)throw std::runtime_error("Cannot load AMD runtime: "+std::to_string(GetLastError()));
        create=(PfnFfxCreateContext)GetProcAddress(library,"ffxCreateContext");destroy=(PfnFfxDestroyContext)GetProcAddress(library,"ffxDestroyContext");
        configure=(PfnFfxConfigure)GetProcAddress(library,"ffxConfigure");dispatch=(PfnFfxDispatch)GetProcAddress(library,"ffxDispatch");query=(PfnFfxQuery)GetProcAddress(library,"ffxQuery");
        if(!create||!destroy||!configure||!dispatch||!query)throw std::runtime_error("Invalid AMD runtime exports");
        uint64_t count=0;ffxQueryDescGetVersions versions{};versions.header.type=FFX_API_QUERY_DESC_TYPE_GET_VERSIONS;
        versions.createDescType=FFX_API_CREATE_CONTEXT_DESC_TYPE_FRAMEGENERATION;versions.device=device.Get();versions.outputCount=&count;
        ffx(query(nullptr,&versions.header),"Query FSR providers");if(!count||count>64)throw std::runtime_error("No FSR provider");
        std::vector<uint64_t> ids(count);std::vector<const char*> names(count);versions.versionIds=ids.data();versions.versionNames=names.data();
        ffx(query(nullptr,&versions.header),"Query FSR versions");size_t selected=count;
        for(size_t i=0;i<count;i++)if(names[i]&&(strstr(names[i],"3.1")||strstr(names[i],"FSR3"))){selected=i;break;}
        if(selected==count)throw std::runtime_error("FSR 3 provider unavailable");provider=names[selected];
        ffxOverrideVersion overrideVersion{};overrideVersion.header.type=FFX_API_DESC_TYPE_OVERRIDE_VERSION;overrideVersion.versionId=ids[selected];
        ffxCreateBackendDX12Desc backend{};backend.header.type=FFX_API_CREATE_CONTEXT_DESC_TYPE_BACKEND_DX12;backend.device=device.Get();backend.header.pNext=&overrideVersion.header;
        ffxCreateContextDescFrameGenerationVersion version{};version.header.type=FFX_API_CREATE_CONTEXT_DESC_TYPE_FRAMEGENERATION_VERSION;version.version=FFX_FRAMEGENERATION_VERSION;version.header.pNext=&backend.header;
        ffxCreateContextDescFrameGeneration desc{};desc.header.type=FFX_API_CREATE_CONTEXT_DESC_TYPE_FRAMEGENERATION;desc.header.pNext=&version.header;
        desc.displaySize={w,h};desc.maxRenderSize={w,h};desc.backBufferFormat=FFX_API_SURFACE_FORMAT_R8G8B8A8_UNORM;
        ffx(create(&context,&desc.header,nullptr),"Create FSR 3 frame generation");
    }
    void generate(UINT64 frame,bool reset,float delta,float nearPlane,float farPlane,float fov,bool debug) {
        const int slot=frame%3;wait(completedSlots[slot]);
        hr(allocator[slot]->Reset(),"Reset allocator");hr(list->Reset(allocator[slot].Get(),nullptr),"Reset list");
        D3D12_RESOURCE_BARRIER barriers[4]{};
        for(int i=0;i<4;i++){auto& b=barriers[i];b.Type=D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;b.Transition.pResource=images[i].Get();b.Transition.Subresource=D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;b.Transition.StateBefore=D3D12_RESOURCE_STATE_COMMON;b.Transition.StateAfter=i==3?D3D12_RESOURCE_STATE_UNORDERED_ACCESS:D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE;}
        list->ResourceBarrier(4,barriers);
        ffxConfigureDescFrameGeneration config{};config.header.type=FFX_API_CONFIGURE_DESC_TYPE_FRAMEGENERATION;
        config.frameGenerationEnabled=true;config.flags=FFX_FRAMEGENERATION_FLAG_NO_SWAPCHAIN_CONTEXT_NOTIFY|(debug?FFX_FRAMEGENERATION_FLAG_DRAW_DEBUG_VIEW:0);
        config.generationRect={0,0,(int)width,(int)height};config.frameID=frame;
        ffx(configure(&context,&config.header),"Configure FSR");
        ffxDispatchDescFrameGenerationPrepareV2 prep{};prep.header.type=FFX_API_DISPATCH_DESC_TYPE_FRAMEGENERATION_PREPARE_V2;
        prep.commandList=list.Get();prep.frameID=frame;prep.renderSize={width,height};prep.motionVectorScale={(float)width,(float)height};
        prep.frameTimeDelta=delta;prep.reset=reset;prep.cameraNear=nearPlane;prep.cameraFar=farPlane;prep.cameraFovAngleVertical=fov;prep.viewSpaceToMetersFactor=1;
        prep.cameraUp[1]=1;prep.cameraRight[0]=1;prep.cameraForward[2]=-1;
        prep.depth=ffxApiGetResourceDX12(images[1].Get(),FFX_API_RESOURCE_STATE_COMPUTE_READ);prep.motionVectors=ffxApiGetResourceDX12(images[2].Get(),FFX_API_RESOURCE_STATE_COMPUTE_READ);
        ffx(dispatch(&context,&prep.header),"FSR prepare");
        ffxDispatchDescFrameGeneration gen{};gen.header.type=FFX_API_DISPATCH_DESC_TYPE_FRAMEGENERATION;gen.commandList=list.Get();gen.frameID=frame;
        gen.presentColor=ffxApiGetResourceDX12(images[0].Get(),FFX_API_RESOURCE_STATE_COMPUTE_READ);
        gen.outputs[0]=ffxApiGetResourceDX12(images[3].Get(),FFX_API_RESOURCE_STATE_UNORDERED_ACCESS);gen.numGeneratedFrames=1;gen.reset=reset;
        gen.backbufferTransferFunction=FFX_API_BACKBUFFER_TRANSFER_FUNCTION_SRGB;gen.minMaxLuminance[0]=0;gen.minMaxLuminance[1]=1000;
        gen.generationRect={0,0,(int)width,(int)height};ffx(dispatch(&context,&gen.header),"FSR interpolation");
        for(auto& b:barriers)std::swap(b.Transition.StateBefore,b.Transition.StateAfter);list->ResourceBarrier(4,barriers);hr(list->Close(),"Close list");
        // GL signals an odd value; D3D signals the next even value. Both waits stay on the GPU.
        hr(queue->Wait(fence.Get(),frame*2-1),"Wait for OpenGL");ID3D12CommandList* lists[]={list.Get()};queue->ExecuteCommandLists(1,lists);
        hr(queue->Signal(fence.Get(),frame*2),"Signal OpenGL");lastSignal=completedSlots[slot]=frame*2;
    }
};
static void error(JNIEnv* e,const std::exception& ex){e->ThrowNew(e->FindClass("java/lang/IllegalStateException"),ex.what());}
extern "C" JNIEXPORT jlong JNICALL Java_dev_slopium_framegen_FsrNative_create(JNIEnv* e,jclass,jint w,jint h,jlong luid,jstring path) {
    try{if(w<1||h<1||w>16384||h>16384)throw std::runtime_error("Invalid resolution");
        const jchar* chars=e->GetStringChars(path,nullptr);std::wstring p((const wchar_t*)chars,e->GetStringLength(path));e->ReleaseStringChars(path,chars);
        auto s=std::make_unique<Session>();s->init(w,h,luid,p.c_str());return (jlong)s.release();
    }catch(const std::exception& ex){error(e,ex);return 0;}
}
extern "C" JNIEXPORT jlongArray JNICALL Java_dev_slopium_framegen_FsrNative_resources(JNIEnv* e,jclass,jlong ptr) {
    auto s=(Session*)ptr;jlong values[9];for(int i=0;i<4;i++){values[i]=(jlong)s->handles[i];values[4+i]=(jlong)s->sizes[i];}values[8]=(jlong)s->handles[4];
    auto result=e->NewLongArray(9);e->SetLongArrayRegion(result,0,9,values);return result;
}
extern "C" JNIEXPORT jstring JNICALL Java_dev_slopium_framegen_FsrNative_provider(JNIEnv* e,jclass,jlong ptr){return e->NewStringUTF(((Session*)ptr)->provider.c_str());}
extern "C" JNIEXPORT void JNICALL Java_dev_slopium_framegen_FsrNative_generate(JNIEnv* e,jclass,jlong ptr,jlong frame,jboolean reset,jfloat delta,jfloat nearPlane,jfloat farPlane,jfloat fov,jboolean debug) {
    try{((Session*)ptr)->generate(frame,reset,delta,nearPlane,farPlane,fov,debug);}catch(const std::exception& ex){error(e,ex);}
}
extern "C" JNIEXPORT void JNICALL Java_dev_slopium_framegen_FsrNative_destroy(JNIEnv*,jclass,jlong ptr){delete (Session*)ptr;}
