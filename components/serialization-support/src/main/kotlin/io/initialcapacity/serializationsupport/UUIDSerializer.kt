package io.initialcapacity.serializationsupport

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/**
 * Custom serializer for UUID serialization.
 */
class UUIDSerializer: KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    /**
     * Deserializes a string representation to a UUID object.
     *
     * @param decoder The decoder used for deserialization.
     * @return The deserialized UUID object.
     */
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
    /**
     * Serializes a UUID to a string representation.
     *
     * @param encoder The encoder used for serialization.
     * @param value The UUID value to be serialized.
     */
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
}
