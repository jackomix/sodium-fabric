package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialIdHolder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

import net.minecraft.client.render.VertexConsumer;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
    static final int STRIDE = 44;

    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, STRIDE)
            .addElement(ChunkMeshAttribute.CHUNK_OFFSET, 0, GlVertexAttributeFormat.UNSIGNED_INT_2_10_10_10_REV, 4, false)
            .addElement(ChunkMeshAttribute.POSITION, 4, GlVertexAttributeFormat.UNSIGNED_SHORT, 4, false)
            .addElement(ChunkMeshAttribute.COLOR, 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.MID_TEX_COORD, 24, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.TANGENT, 28, GlVertexAttributeFormat.BYTE, 4, true)
            .addElement(ChunkMeshAttribute.NORMAL, 32, GlVertexAttributeFormat.BYTE, 3, true)
            .addElement(ChunkMeshAttribute.BLOCK_ID, 36, GlVertexAttributeFormat.SHORT, 4, false)
            .build();

    private static final int POSITION_MAX_VALUE = 32768;
    private static final int TEXTURE_MAX_VALUE = 32768;

    private static final float MODEL_ORIGIN = 4.0f;
    private static final float MODEL_RANGE = 8.0f;
    private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;

    private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

    private static final float TEXTURE_SCALE = (1.0f / TEXTURE_MAX_VALUE);

    @Override
    public ModelVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return createBufferWriter(buffer, new MaterialIdHolder());
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, MaterialIdHolder idHolder) {
        return new XHFPModelVertexBufferWriterNio(buffer, idHolder);
    }

    @Override
    public BlittableVertexType<ModelVertexSink> asBlittable() {
        return this;
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public float getTextureScale() {
        return TEXTURE_SCALE;
    }

    @Override
    public float getModelScale() {
        return MODEL_SCALE;
    }

    @Override
    public float getModelOffset() {
        return -MODEL_ORIGIN;
    }

    static short encodeBlockTexture(float value) {
        return (short) (value * TEXTURE_MAX_VALUE);
    }

    static float decodeBlockTexture(short raw) {
        return (raw & 0xFFFF) * TEXTURE_SCALE;
    }

    static short encodePosition(float v) {
        return (short) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
    }

    static float decodePosition(short raw) {
        return (raw & 0xFFFF) * MODEL_SCALE - MODEL_ORIGIN;
    }

    static int encodeLightMapTexCoord(int light) {
        int r = light;

        // Mask off coordinate values outside 0..255
        r &= 0x00FF_00FF;

        // Light coordinates are normalized values, so upcasting requires a shift
        // Scale the coordinates from the range of 0..255 (unsigned byte) into 0..65535 (unsigned short)
        r <<= 8;

        // Add a half-texel offset to each coordinate so we sample from the center of each texel
        r += 0x0800_0800;

        return r;
    }
}
