/*
 * FrameGen -- frame generation for Minecraft via depth reprojection.
 * Copyright (C) 2026 Screamix
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.slopium.framegen.reference;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal raw-GL shader program wrapper.
 *
 * Deliberately does not go through {@code ShaderInstance}: vanilla's shader
 * machinery is tied to JSON definitions and fixed vertex formats, and our
 * passes use no vertex attributes at all.
 */
public final class GlProgram implements AutoCloseable {

    private final String name;
    private final int id;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    private GlProgram(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public static GlProgram compile(String name, String vertexSource, String fragmentSource, String... fragDataNames) {
        int vs = compileStage(GL20.GL_VERTEX_SHADER, vertexSource, name + " (vertex)");
        int fs;
        try {
            fs = compileStage(GL20.GL_FRAGMENT_SHADER, fragmentSource, name + " (fragment)");
        } catch (RuntimeException e) {
            GL20.glDeleteShader(vs);
            throw e;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);

        for (int i = 0; i < fragDataNames.length; i++) {
            GL30.glBindFragDataLocation(program, i, fragDataNames[i]);
        }

        GL20.glLinkProgram(program);

        boolean linked = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_FALSE;
        String log = GL20.glGetProgramInfoLog(program);

        GL20.glDetachShader(program, vs);
        GL20.glDetachShader(program, fs);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        if (!linked) {
            GL20.glDeleteProgram(program);
            throw new IllegalStateException("FrameGen: failed to link program '" + name + "':\n" + log);
        }

        return new GlProgram(name, program);
    }

    private static int compileStage(int type, String source, String what) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("FrameGen: failed to compile " + what + ":\n" + log);
        }
        return shader;
    }

    public void use() {
        GL20.glUseProgram(id);
    }

    private int uniform(String uniformName) {
        return uniformCache.computeIfAbsent(uniformName, n -> GL20.glGetUniformLocation(id, n));
    }

    public void setInt(String uniformName, int value) {
        int loc = uniform(uniformName);
        if (loc >= 0) GL20.glUniform1i(loc, value);
    }

    public void setFloat(String uniformName, float value) {
        int loc = uniform(uniformName);
        if (loc >= 0) GL20.glUniform1f(loc, value);
    }

    public void setVec2(String uniformName, float x, float y) {
        int loc = uniform(uniformName);
        if (loc >= 0) GL20.glUniform2f(loc, x, y);
    }

    public void setVec3(String uniformName, float x, float y, float z) {
        int loc = uniform(uniformName);
        if (loc >= 0) GL20.glUniform3f(loc, x, y, z);
    }

    public void setIVec2(String uniformName, int x, int y) {
        int loc = uniform(uniformName);
        if (loc >= 0) GL20.glUniform2i(loc, x, y);
    }

    public void setMatrix(String uniformName, Matrix4f matrix) {
        int loc = uniform(uniformName);
        if (loc < 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            GL20.glUniformMatrix4fv(loc, false, buffer);
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(id);
        uniformCache.clear();
    }
}
