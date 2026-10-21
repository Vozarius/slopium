package dev.slopium.visibility;

/** Pause only the GPU representation; the live Create instance values remain untouched. */
public interface PausedRotation {
    void slopium$pauseRotation(boolean paused, float renderSeconds);
    float slopium$renderSpeed(float speed);
    float slopium$renderOffset(float offset);
}
