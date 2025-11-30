package com.jder.domain.model
import kotlinx.serialization.Serializable
@Serializable
data class Entity(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float = 140f,
    val height: Float = 70f,
    val attributes: List<Attribute> = emptyList(),
    val documentation: String = "",
    val isWeak: Boolean = false
)
@Serializable
data class Relationship(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float = 120f,
    val height: Float = 120f,
    val attributes: List<Attribute> = emptyList(),
    val connections: List<Connection> = emptyList(),
    val documentation: String = ""
)
@Serializable
data class Connection(
    val entityId: String,
    val cardinality: Cardinality
)
@Serializable
enum class Cardinality(val display: String) {
    ONE("1"),
    ZERO_ONE("0,1"),
    ONE_ONE("1:1"),
    MANY("N"),
    ZERO_MANY("0,N"),
    ONE_MANY("1,N"),
    MANY_MANY("N:N");
}
@Serializable
data class Attribute(
    val id: String,
    val name: String,
    val type: AttributeType,
    val x: Float = 0f,
    val y: Float = 0f,
    val isPrimaryKey: Boolean = false,
    val components: List<Attribute> = emptyList(),
    val multiplicity: String = ""
)
@Serializable
enum class AttributeType {
    NORMAL,
    KEY,
    MULTIVALUED,
    DERIVED,
    COMPOSITE
}
@Serializable
data class ERDiagram(
    val name: String,
    val entities: List<Entity> = emptyList(),
    val relationships: List<Relationship> = emptyList(),
    val documentation: String = ""
)
