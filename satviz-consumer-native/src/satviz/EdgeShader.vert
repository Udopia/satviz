#version 330 core
#extension GL_ARB_explicit_uniform_location : enable

#define SENTINEL_INDEX 0xFFFFFFFF

layout(location = 0) uniform mat4 world_to_view;
uniform samplerBuffer offset_texview;

layout(location = 0) in ivec2 edge_indices;
layout(location = 1) in float edge_weight;

out float weight;

void main() {
    int index = gl_VertexID == 0 ? edge_indices.x : edge_indices.y;
    if (index == SENTINEL_INDEX) {
        gl_Position = vec4(0.0, 0.0, -10.0, 1.0);
    } else {
        vec2 offset = texelFetch(offset_texview, index).rg;
        gl_Position = world_to_view * vec4(offset, 0.0, 1.0);
    }
    weight = edge_weight;
}