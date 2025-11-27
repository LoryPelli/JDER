package com.jder.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.jder.domain.model.Attribute
import com.jder.domain.model.AttributeType
import com.jder.domain.model.DiagramState
import com.jder.domain.model.Entity
import com.jder.domain.model.Relationship
import com.jder.domain.model.ToolMode
@Composable
fun ERDiagramCanvas(
    state: DiagramState,
    onContextMenuRequest: ((Offset, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val entityColor = MaterialTheme.colorScheme.primary
    val relationshipColor = MaterialTheme.colorScheme.error
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val textColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val connectionColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    var isDragging by remember { mutableStateOf(false) }
    var draggedAttributeInfo by remember { mutableStateOf<Triple<String?, String?, String?>>(Triple(null, null, null)) }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.diagram.entities.size, state.diagram.relationships.size) {
                detectTapGestures(
                    onTap = { offset ->
                        if (!isDragging) {
                            val adjustedOffset = Offset(
                                (offset.x - state.panOffset.x) / state.zoom,
                                (offset.y - state.panOffset.y) / state.zoom
                            )
                            handleCanvasTap(state, adjustedOffset)
                        }
                    },
                    onLongPress = { offset ->
                        val adjustedOffset = Offset(
                            (offset.x - state.panOffset.x) / state.zoom,
                            (offset.y - state.panOffset.y) / state.zoom
                        )
                        val clickedEntity = state.diagram.entities.find { entity ->
                            adjustedOffset.x >= entity.x &&
                            adjustedOffset.x <= entity.x + entity.width &&
                            adjustedOffset.y >= entity.y &&
                            adjustedOffset.y <= entity.y + entity.height
                        }
                        val clickedRelationship = state.diagram.relationships.find { rel ->
                            val centerX = rel.x + rel.width / 2
                            val centerY = rel.y + rel.height / 2
                            val halfWidth = rel.width / 2
                            val halfHeight = rel.height / 2
                            val dx = kotlin.math.abs(adjustedOffset.x - centerX) / halfWidth
                            val dy = kotlin.math.abs(adjustedOffset.y - centerY) / halfHeight
                            dx + dy < 1f
                        }
                        when {
                            clickedEntity != null -> {
                                state.selectEntity(clickedEntity.id)
                                onContextMenuRequest?.invoke(offset, true)
                            }
                            clickedRelationship != null -> {
                                state.selectRelationship(clickedRelationship.id)
                                onContextMenuRequest?.invoke(offset, false)
                            }
                        }
                    }
                )
            }
            .pointerInput(state.selectedEntityId, state.selectedRelationshipId, onContextMenuRequest) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.buttons.isSecondaryPressed) {
                            val position = event.changes.first().position
                            val adjustedOffset = Offset(
                                (position.x - state.panOffset.x) / state.zoom,
                                (position.y - state.panOffset.y) / state.zoom
                            )
                            val clickedEntity = state.diagram.entities.find { entity ->
                                adjustedOffset.x >= entity.x &&
                                adjustedOffset.x <= entity.x + entity.width &&
                                adjustedOffset.y >= entity.y &&
                                adjustedOffset.y <= entity.y + entity.height
                            }
                            val clickedRelationship = state.diagram.relationships.find { rel ->
                                val centerX = rel.x + rel.width / 2
                                val centerY = rel.y + rel.height / 2
                                val halfWidth = rel.width / 2
                                val halfHeight = rel.height / 2
                                val dx = kotlin.math.abs(adjustedOffset.x - centerX) / halfWidth
                                val dy = kotlin.math.abs(adjustedOffset.y - centerY) / halfHeight
                                dx + dy < 1f
                            }
                            when {
                                clickedEntity != null -> {
                                    state.selectEntity(clickedEntity.id)
                                    onContextMenuRequest?.invoke(position, true)
                                }
                                clickedRelationship != null -> {
                                    state.selectRelationship(clickedRelationship.id)
                                    onContextMenuRequest?.invoke(position, false)
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(state.selectedEntityId, state.selectedRelationshipId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = false
                        draggedAttributeInfo = handleDragStart(state, offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        isDragging = true
                        handleDrag(state, dragAmount, draggedAttributeInfo.first, draggedAttributeInfo.second, draggedAttributeInfo.third)
                    },
                    onDragEnd = {
                        isDragging = false
                        draggedAttributeInfo = Triple(null, null, null)
                    }
                )
            }
    ) {
        drawRect(
            color = backgroundColor,
            size = size
        )
        withTransform({
            translate(state.panOffset.x, state.panOffset.y)
            scale(state.zoom, state.zoom, Offset.Zero)
        }) {
            drawGrid(size, gridColor)
            state.diagram.relationships.forEach { relationship ->
                drawConnectionsForRelationship(
                    relationship = relationship,
                    entities = state.diagram.entities,
                    color = connectionColor,
                    textMeasurer = textMeasurer
                )
            }
            state.diagram.entities.forEach { entity ->
                val isSelected = state.selectedEntityId == entity.id
                drawEntity(
                    entity = entity,
                    isSelected = isSelected,
                    isHovered = false,
                    entityColor = entityColor,
                    selectedColor = selectedColor,
                    textColor = textColor,
                    surfaceColor = surfaceColor,
                    textMeasurer = textMeasurer
                )
            }
            state.diagram.relationships.forEach { relationship ->
                val isSelected = state.selectedRelationshipId == relationship.id
                drawRelationship(
                    relationship = relationship,
                    isSelected = isSelected,
                    isHovered = false,
                    relationshipColor = relationshipColor,
                    selectedColor = selectedColor,
                    textColor = textColor,
                    surfaceColor = surfaceColor,
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}
private fun DrawScope.drawGrid(canvasSize: Size, color: Color, spacing: Float = 20f) {
    val numVerticalLines = (canvasSize.width / spacing).toInt()
    val numHorizontalLines = (canvasSize.height / spacing).toInt()
    for (i in 0..numVerticalLines) {
        drawLine(
            color = color,
            start = Offset(i * spacing, 0f),
            end = Offset(i * spacing, canvasSize.height),
            strokeWidth = 1f
        )
    }
    for (i in 0..numHorizontalLines) {
        drawLine(
            color = color,
            start = Offset(0f, i * spacing),
            end = Offset(canvasSize.width, i * spacing),
            strokeWidth = 1f
        )
    }
}
private fun DrawScope.drawEntity(
    entity: Entity,
    isSelected: Boolean,
    isHovered: Boolean,
    entityColor: Color,
    selectedColor: Color,
    textColor: Color,
    surfaceColor: Color,
    textMeasurer: TextMeasurer
) {
    val color = if (isSelected) selectedColor else entityColor
    val strokeWidth = when {
        isSelected -> 3.5f
        isHovered -> 2.5f
        else -> 2f
    }
    val fillColor = when {
        isSelected -> selectedColor.copy(alpha = 0.08f)
        isHovered -> entityColor.copy(alpha = 0.05f)
        else -> surfaceColor
    }
    drawRect(
        color = fillColor,
        topLeft = Offset(entity.x, entity.y),
        size = Size(entity.width, entity.height),
        style = Fill
    )
    drawRect(
        color = color,
        topLeft = Offset(entity.x, entity.y),
        size = Size(entity.width, entity.height),
        style = Stroke(width = strokeWidth)
    )
    if (entity.isWeak) {
        drawRect(
            color = color,
            topLeft = Offset(entity.x + 4, entity.y + 4),
            size = Size(entity.width - 8, entity.height - 8),
            style = Stroke(width = 1.5f)
        )
    }
    val textLayoutResult = textMeasurer.measure(
        text = entity.name,
        style = TextStyle(
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isHovered) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            entity.x + (entity.width - textLayoutResult.size.width) / 2,
            entity.y + (entity.height - textLayoutResult.size.height) / 2
        )
    )
    if (entity.attributes.isNotEmpty()) {
        entity.attributes.forEachIndexed { index, attribute ->
            drawAttribute(
                attribute = attribute,
                entityX = entity.x,
                entityY = entity.y,
                entityWidth = entity.width,
                entityHeight = entity.height,
                index = index,
                total = entity.attributes.size,
                textMeasurer = textMeasurer,
                textColor = textColor,
                isRectangle = true
            )
        }
    }
}
private fun DrawScope.drawRelationship(
    relationship: Relationship,
    isSelected: Boolean,
    isHovered: Boolean,
    relationshipColor: Color,
    selectedColor: Color,
    textColor: Color,
    surfaceColor: Color,
    textMeasurer: TextMeasurer
) {
    val color = if (isSelected) selectedColor else relationshipColor
    val strokeWidth = when {
        isSelected -> 3.5f
        isHovered -> 2.5f
        else -> 2f
    }
    val fillColor = when {
        isSelected -> selectedColor.copy(alpha = 0.08f)
        isHovered -> relationshipColor.copy(alpha = 0.05f)
        else -> surfaceColor
    }
    val centerX = relationship.x + relationship.width / 2
    val centerY = relationship.y + relationship.height / 2
    val path = Path().apply {
        moveTo(centerX, relationship.y)
        lineTo(relationship.x + relationship.width, centerY)
        lineTo(centerX, relationship.y + relationship.height)
        lineTo(relationship.x, centerY)
        close()
    }
    drawPath(
        path = path,
        color = fillColor,
        style = Fill
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
    val textLayoutResult = textMeasurer.measure(
        text = relationship.name,
        style = TextStyle(
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isHovered) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            centerX - textLayoutResult.size.width / 2,
            centerY - textLayoutResult.size.height / 2
        )
    )
    if (relationship.attributes.isNotEmpty()) {
        relationship.attributes.forEachIndexed { index, attribute ->
            drawAttribute(
                attribute = attribute,
                entityX = relationship.x,
                entityY = relationship.y,
                entityWidth = relationship.width,
                entityHeight = relationship.height,
                index = index,
                total = relationship.attributes.size,
                textMeasurer = textMeasurer,
                textColor = textColor,
                isRectangle = false
            )
        }
    }
}
private fun DrawScope.drawAttribute(
    attribute: Attribute,
    entityX: Float,
    entityY: Float,
    entityWidth: Float,
    entityHeight: Float,
    index: Int,
    total: Int,
    textMeasurer: TextMeasurer,
    textColor: Color,
    isRectangle: Boolean
) {
    val radius = 20f
    val arrowLength = 60f
    val verticalSpacing = 60f
    val centerX = entityX + entityWidth / 2
    val centerY = entityY + entityHeight / 2
    val startY = centerY - ((total - 1) * verticalSpacing / 2f)
    val defaultAttrX = entityX + entityWidth + arrowLength
    val defaultAttrY = startY + (index * verticalSpacing)
    val attrX = if (attribute.x != 0f) centerX + attribute.x else defaultAttrX
    val attrY = if (attribute.y != 0f) centerY + attribute.y else defaultAttrY
    val dx = attrX - centerX
    val dy = attrY - centerY
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
    val dirX = if (distance > 0) dx / distance else 1f
    val dirY = if (distance > 0) dy / distance else 0f
    val arrowStartX = attrX - dirX * arrowLength
    val arrowStartY = attrY - dirY * arrowLength
    val connectionPoint = if (isRectangle) {
        getClosestPointOnRectangle(entityX, entityY, entityWidth, entityHeight, arrowStartX, arrowStartY)
    } else {
        getClosestPointOnDiamond(centerX, centerY, entityWidth, entityHeight, arrowStartX, arrowStartY)
    }
    val attributeBackgroundColor = Color(0xFF424242)
    val normalAttributeColor = Color(0xFF90CAF9)
    val primaryKeyColor = Color(0xFFFFEB3B)
    val compositeColor = Color(0xFFFFA726)
    drawLine(
        color = textColor.copy(alpha = 0.4f),
        start = connectionPoint,
        end = Offset(attrX, attrY),
        strokeWidth = 1.5f
    )
    when (attribute.type) {
        AttributeType.COMPOSITE -> {
            drawCircle(
                color = attributeBackgroundColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Fill
            )
            drawCircle(
                color = compositeColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = compositeColor,
                radius = radius - 5,
                center = Offset(attrX, attrY),
                style = Stroke(width = 2f)
            )
            if (attribute.components.isNotEmpty()) {
                attribute.components.forEachIndexed { compIndex, component ->
                    drawAttributeComponent(
                        component = component,
                        parentX = attrX + radius,
                        parentY = attrY,
                        index = compIndex,
                        total = attribute.components.size,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
        AttributeType.MULTIVALUED -> {
            drawCircle(
                color = attributeBackgroundColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Fill
            )
            drawCircle(
                color = normalAttributeColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = normalAttributeColor,
                radius = radius - 5,
                center = Offset(attrX, attrY),
                style = Stroke(width = 2f)
            )
        }
        AttributeType.DERIVED -> {
            drawCircle(
                color = attributeBackgroundColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Fill
            )
            drawCircle(
                color = normalAttributeColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
            )
        }
        else -> {
            val circleStrokeColor = if (attribute.isPrimaryKey) primaryKeyColor else normalAttributeColor
            drawCircle(
                color = attributeBackgroundColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Fill
            )
            drawCircle(
                color = circleStrokeColor,
                radius = radius,
                center = Offset(attrX, attrY),
                style = Stroke(width = if (attribute.isPrimaryKey) 3.5f else 2.5f)
            )
        }
    }
    val text = attribute.name
    val attributeTextColor = if (attribute.isPrimaryKey) primaryKeyColor
                            else if (attribute.type == AttributeType.COMPOSITE) compositeColor
                            else textColor
    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = attributeTextColor,
            fontSize = 13.sp,
            fontWeight = if (attribute.isPrimaryKey)
                androidx.compose.ui.text.font.FontWeight.Bold
            else
                androidx.compose.ui.text.font.FontWeight.SemiBold
        )
    )
    val textX = attrX + radius + 10
    val textY = attrY - textLayoutResult.size.height / 2
    drawRoundRect(
        color = Color(0xDD000000),
        topLeft = Offset(textX - 5, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 10,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = attributeTextColor.copy(alpha = 0.5f),
        topLeft = Offset(textX - 5, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 10,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        style = Stroke(width = 1.5f)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, textY)
    )
    if (attribute.type == AttributeType.MULTIVALUED && attribute.multiplicity.isNotBlank()) {
        val multiplicityText = textMeasurer.measure(
            text = attribute.multiplicity,
            style = TextStyle(
                color = normalAttributeColor.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
        )
        val multY = textY + textLayoutResult.size.height + 4
        drawRoundRect(
            color = Color(0xDD000000),
            topLeft = Offset(textX - 3, multY - 1),
            size = Size(
                multiplicityText.size.width.toFloat() + 6,
                multiplicityText.size.height.toFloat() + 2
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawText(
            textLayoutResult = multiplicityText,
            topLeft = Offset(textX, multY)
        )
    }
}
private fun DrawScope.drawAttributeComponent(
    component: Attribute,
    parentX: Float,
    parentY: Float,
    index: Int,
    total: Int,
    textMeasurer: TextMeasurer
) {
    val radius = 12f
    val horizontalSpacing = 60f
    val verticalSpacing = 40f
    val startY = parentY - ((total - 1) * verticalSpacing / 2f)
    val compX = parentX + horizontalSpacing
    val compY = startY + (index * verticalSpacing)
    val compositeColor = Color(0xFFFFA726)
    val componentColor = Color(0xFFFFCC80)
    val attributeBackgroundColor = Color(0xFF424242)
    drawLine(
        color = compositeColor.copy(alpha = 0.4f),
        start = Offset(parentX, parentY),
        end = Offset(compX, compY),
        strokeWidth = 1.2f
    )
    drawCircle(
        color = attributeBackgroundColor,
        radius = radius,
        center = Offset(compX, compY),
        style = Fill
    )
    drawCircle(
        color = componentColor,
        radius = radius,
        center = Offset(compX, compY),
        style = Stroke(width = 2f)
    )
    val textLayoutResult = textMeasurer.measure(
        text = component.name,
        style = TextStyle(
            color = componentColor,
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
        )
    )
    val textX = compX + radius + 8
    val textY = compY - textLayoutResult.size.height / 2
    drawRoundRect(
        color = Color(0xDD000000),
        topLeft = Offset(textX - 4, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 8,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = componentColor.copy(alpha = 0.4f),
        topLeft = Offset(textX - 4, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 8,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        style = Stroke(width = 1.2f)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(textX, textY)
    )
}
private fun DrawScope.drawConnectionsForRelationship(
    relationship: Relationship,
    entities: List<Entity>,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val centerX = relationship.x + relationship.width / 2
    val centerY = relationship.y + relationship.height / 2
    val cardinalityColor = Color(0xFFFFEB3B)
    relationship.connections.forEach { connection ->
        val entity = entities.find { it.id == connection.entityId }
        if (entity != null) {
            val entityCenterX = entity.x + entity.width / 2
            val entityCenterY = entity.y + entity.height / 2
            drawLine(
                color = color,
                start = Offset(centerX, centerY),
                end = Offset(entityCenterX, entityCenterY),
                strokeWidth = 1.5f
            )
            val textLayoutResult = textMeasurer.measure(
                text = connection.cardinality.display,
                style = TextStyle(
                    color = cardinalityColor,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
            val offsetFactor = 0.5f
            val labelCenterX = centerX + (entityCenterX - centerX) * offsetFactor
            val labelCenterY = centerY + (entityCenterY - centerY) * offsetFactor
            val textWidth = textLayoutResult.size.width.toFloat()
            val textHeight = textLayoutResult.size.height.toFloat()
            val labelX = labelCenterX - textWidth / 2
            val labelY = labelCenterY - textHeight / 2
            drawRoundRect(
                color = Color(0xDD000000),
                topLeft = Offset(labelX - 6, labelY - 3),
                size = Size(
                    textWidth + 12,
                    textHeight + 6
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = cardinalityColor.copy(alpha = 0.6f),
                topLeft = Offset(labelX - 6, labelY - 3),
                size = Size(
                    textWidth + 12,
                    textHeight + 6
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(labelX, labelY)
            )
        }
    }
}
private fun handleCanvasTap(state: DiagramState, offset: Offset) {
    when (state.toolMode) {
        ToolMode.SELECT -> {
            val clickedRelationship = state.diagram.relationships.firstOrNull { rel ->
                isPointInDiamond(offset, rel)
            }
            if (clickedRelationship != null) {
                state.selectRelationship(clickedRelationship.id)
                return
            }
            val clickedEntity = state.diagram.entities.firstOrNull { entity ->
                offset.x >= entity.x && offset.x <= entity.x + entity.width &&
                offset.y >= entity.y && offset.y <= entity.y + entity.height
            }
            if (clickedEntity != null) {
                state.selectEntity(clickedEntity.id)
            } else {
                state.clearSelection()
            }
        }
        ToolMode.ENTITY -> {
            state.addEntity(offset.x - 70f, offset.y - 35f)
            state.toolMode = ToolMode.SELECT
        }
        ToolMode.RELATIONSHIP -> {
            state.addRelationship(offset.x - 60f, offset.y - 60f)
            state.toolMode = ToolMode.SELECT
        }
    }
}
private fun handleDragStart(state: DiagramState, offset: Offset): Triple<String?, String?, String?> {
    val adjustedOffset = Offset(
        (offset.x - state.panOffset.x) / state.zoom,
        (offset.y - state.panOffset.y) / state.zoom
    )
    state.diagram.entities.forEach { entity ->
        entity.attributes.forEachIndexed { index, attribute ->
            val attrPos = calculateAttributePosition(
                entity.x,
                entity.y,
                entity.width,
                entity.height,
                index,
                entity.attributes.size,
                attribute
            )
            val distance = kotlin.math.sqrt(
                (adjustedOffset.x - attrPos.x).let { it * it } +
                (adjustedOffset.y - attrPos.y).let { it * it }
            )
            if (distance <= 20f) {
                state.selectEntity(entity.id)
                state.saveDragStartState()
                return Triple(attribute.id, entity.id, null)
            }
        }
    }
    state.diagram.relationships.forEach { rel ->
        rel.attributes.forEachIndexed { index, attribute ->
            val attrPos = calculateAttributePosition(
                rel.x,
                rel.y,
                rel.width,
                rel.height,
                index,
                rel.attributes.size,
                attribute
            )
            val distance = kotlin.math.sqrt(
                (adjustedOffset.x - attrPos.x).let { it * it } +
                (adjustedOffset.y - attrPos.y).let { it * it }
            )
            if (distance <= 20f) {
                state.selectRelationship(rel.id)
                state.saveDragStartState()
                return Triple(attribute.id, null, rel.id)
            }
        }
    }
    val entity = state.diagram.entities.find { entity ->
        adjustedOffset.x >= entity.x && adjustedOffset.x <= entity.x + entity.width &&
        adjustedOffset.y >= entity.y && adjustedOffset.y <= entity.y + entity.height
    }
    if (entity != null) {
        state.selectEntity(entity.id)
        state.saveDragStartState()
        return Triple(null, null, null)
    }
    val relationship = state.diagram.relationships.find { rel ->
        isPointInDiamond(adjustedOffset, rel)
    }
    if (relationship != null) {
        state.selectRelationship(relationship.id)
        state.saveDragStartState()
        return Triple(null, null, null)
    }
    return Triple(null, null, null)
}
private fun handleDrag(
    state: DiagramState,
    dragAmount: Offset,
    draggedAttributeId: String?,
    draggedAttributeForEntity: String?,
    draggedAttributeForRelationship: String?
) {
    if (draggedAttributeId != null && draggedAttributeForEntity != null) {
        state.updateEntityWithoutSave(draggedAttributeForEntity) { entity ->
            entity.copy(
                attributes = entity.attributes.mapIndexed { index, attr ->
                    if (attr.id == draggedAttributeId) {
                        val centerX = entity.x + entity.width / 2
                        val centerY = entity.y + entity.height / 2
                        val currentAttrX = if (attr.x == 0f && attr.y == 0f) {
                            val arrowLength = 60f
                            entity.x + entity.width + arrowLength
                        } else {
                            centerX + attr.x
                        }
                        val currentAttrY = if (attr.x == 0f && attr.y == 0f) {
                            val verticalSpacing = 60f
                            val startY = centerY - ((entity.attributes.size - 1) * verticalSpacing / 2f)
                            startY + (index * verticalSpacing)
                        } else {
                            centerY + attr.y
                        }
                        val currentDx = currentAttrX - centerX
                        val currentDy = currentAttrY - centerY
                        val currentDistanceFromCenter = kotlin.math.sqrt(currentDx * currentDx + currentDy * currentDy)
                        val fixedDistance = if (currentDistanceFromCenter > 0) currentDistanceFromCenter else (entity.width / 2 + 60f)
                        val newAttrX = currentAttrX + dragAmount.x / state.zoom
                        val newAttrY = currentAttrY + dragAmount.y / state.zoom
                        val dx = newAttrX - centerX
                        val dy = newAttrY - centerY
                        val currentDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                        val normalizedX = if (currentDistance > 0) dx / currentDistance else 1f
                        val normalizedY = if (currentDistance > 0) dy / currentDistance else 0f
                        attr.copy(
                            x = normalizedX * fixedDistance,
                            y = normalizedY * fixedDistance
                        )
                    } else {
                        attr
                    }
                }
            )
        }
        return
    }
    if (draggedAttributeId != null && draggedAttributeForRelationship != null) {
        state.updateRelationshipWithoutSave(draggedAttributeForRelationship) { rel ->
            rel.copy(
                attributes = rel.attributes.mapIndexed { index, attr ->
                    if (attr.id == draggedAttributeId) {
                        val centerX = rel.x + rel.width / 2
                        val centerY = rel.y + rel.height / 2
                        val currentAttrX = if (attr.x == 0f && attr.y == 0f) {
                            val arrowLength = 60f
                            rel.x + rel.width + arrowLength
                        } else {
                            centerX + attr.x
                        }
                        val currentAttrY = if (attr.x == 0f && attr.y == 0f) {
                            val verticalSpacing = 60f
                            val startY = centerY - ((rel.attributes.size - 1) * verticalSpacing / 2f)
                            startY + (index * verticalSpacing)
                        } else {
                            centerY + attr.y
                        }
                        val currentDx = currentAttrX - centerX
                        val currentDy = currentAttrY - centerY
                        val currentDistanceFromCenter = kotlin.math.sqrt(currentDx * currentDx + currentDy * currentDy)
                        val fixedDistance = if (currentDistanceFromCenter > 0) currentDistanceFromCenter else (rel.width / 2 + 60f)
                        val newAttrX = currentAttrX + dragAmount.x / state.zoom
                        val newAttrY = currentAttrY + dragAmount.y / state.zoom
                        val dx = newAttrX - centerX
                        val dy = newAttrY - centerY
                        val currentDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                        val normalizedX = if (currentDistance > 0) dx / currentDistance else 1f
                        val normalizedY = if (currentDistance > 0) dy / currentDistance else 0f
                        attr.copy(
                            x = normalizedX * fixedDistance,
                            y = normalizedY * fixedDistance
                        )
                    } else {
                        attr
                    }
                }
            )
        }
        return
    }
    state.selectedEntityId?.let { entityId ->
        state.updateEntityWithoutSave(entityId) { entity ->
            entity.copy(
                x = entity.x + dragAmount.x / state.zoom,
                y = entity.y + dragAmount.y / state.zoom
            )
        }
    }
    state.selectedRelationshipId?.let { relId ->
        state.updateRelationshipWithoutSave(relId) { rel ->
            rel.copy(
                x = rel.x + dragAmount.x / state.zoom,
                y = rel.y + dragAmount.y / state.zoom
            )
        }
    }
}
private fun isPointInDiamond(point: Offset, relationship: Relationship): Boolean {
    val centerX = relationship.x + relationship.width / 2
    val centerY = relationship.y + relationship.height / 2
    val dx = kotlin.math.abs(point.x - centerX) / (relationship.width / 2)
    val dy = kotlin.math.abs(point.y - centerY) / (relationship.height / 2)
    return (dx + dy) <= 1.0
}
private fun calculateAttributePosition(
    entityX: Float,
    entityY: Float,
    entityWidth: Float,
    entityHeight: Float,
    index: Int,
    total: Int,
    attribute: Attribute
): Offset {
    val centerX = entityX + entityWidth / 2
    val centerY = entityY + entityHeight / 2
    if (attribute.x != 0f || attribute.y != 0f) {
        return Offset(centerX + attribute.x, centerY + attribute.y)
    }
    val arrowLength = 60f
    val verticalSpacing = 60f
    val startY = centerY - ((total - 1) * verticalSpacing / 2f)
    val attrX = entityX + entityWidth + arrowLength
    val attrY = startY + (index * verticalSpacing)
    return Offset(attrX, attrY)
}
private fun getClosestPointOnRectangle(
    rectX: Float,
    rectY: Float,
    rectWidth: Float,
    rectHeight: Float,
    targetX: Float,
    targetY: Float
): Offset {
    val centerX = rectX + rectWidth / 2
    val centerY = rectY + rectHeight / 2
    val dx = targetX - centerX
    val dy = targetY - centerY
    val scaleX = if (dx != 0f) (rectWidth / 2) / kotlin.math.abs(dx) else Float.MAX_VALUE
    val scaleY = if (dy != 0f) (rectHeight / 2) / kotlin.math.abs(dy) else Float.MAX_VALUE
    val scale = kotlin.math.min(scaleX, scaleY)
    return Offset(
        centerX + dx * scale,
        centerY + dy * scale
    )
}
private fun getClosestPointOnDiamond(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    targetX: Float,
    targetY: Float
): Offset {
    val dx = targetX - centerX
    val dy = targetY - centerY
    val halfWidth = width / 2
    val halfHeight = height / 2
    val totalScale = 1f / (kotlin.math.abs(dx) / halfWidth + kotlin.math.abs(dy) / halfHeight)
    return Offset(
        centerX + dx * totalScale,
        centerY + dy * totalScale
    )
}
