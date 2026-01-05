package com.jder.ui.components
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jder.domain.model.Attribute
import com.jder.domain.model.AttributeType
import com.jder.domain.model.DiagramState
import com.jder.domain.model.Entity
import com.jder.domain.model.Note
import com.jder.domain.model.Relationship
import com.jder.domain.model.ToolMode
import kotlin.math.abs
import kotlin.math.sqrt
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ERDiagramCanvas(
    state: DiagramState,
    onContextMenuRequest: ((Offset, ContextMenuType) -> Unit)? = null,
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
    val attributeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val normalAttributeColor = MaterialTheme.colorScheme.primary
    val primaryKeyColor = MaterialTheme.colorScheme.tertiary
    val compositeColor = MaterialTheme.colorScheme.secondary
    val componentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    val cardinalityColor = MaterialTheme.colorScheme.tertiary
    val textBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    var isDragging by remember { mutableStateOf(false) }
    var draggedAttributeInfo by remember { mutableStateOf<Triple<String?, String?, String?>>(Triple(null, null, null)) }
    var totalDragDistance by remember { mutableStateOf(0f) }
    var isMiddleMouseDragging by remember { mutableStateOf(false) }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Scroll -> {
                                val change = event.changes.first()
                                val scrollDelta = change.scrollDelta
                                val zoomFactor = if (scrollDelta.y < 0) 1.1f else 0.9f
                                val newZoom = (state.zoom * zoomFactor).coerceIn(0.25f, 3f)
                                val mousePos = change.position
                                val oldMouseWorld = Offset(
                                    (mousePos.x - state.panOffset.x) / state.zoom,
                                    (mousePos.y - state.panOffset.y) / state.zoom
                                )
                                state.zoom = newZoom
                                state.panOffset = Offset(
                                    mousePos.x - oldMouseWorld.x * newZoom,
                                    mousePos.y - oldMouseWorld.y * newZoom
                                )
                            }
                            PointerEventType.Press -> {
                                if (event.button == PointerButton.Tertiary) {
                                    isMiddleMouseDragging = true
                                }
                            }
                            PointerEventType.Move -> {
                                if (isMiddleMouseDragging) {
                                    val change = event.changes.first()
                                    val dragAmount = change.position - change.previousPosition
                                    state.panOffset = Offset(
                                        state.panOffset.x + dragAmount.x,
                                        state.panOffset.y + dragAmount.y
                                    )
                                }
                            }
                            PointerEventType.Release -> {
                                if (event.button == PointerButton.Tertiary) {
                                    isMiddleMouseDragging = false
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(state.diagram.entities.size, state.diagram.relationships.size) {
                detectTapGestures(
                    onTap = { offset ->
                        if (!isDragging && totalDragDistance < 5f) {
                            val adjustedOffset = Offset(
                                (offset.x - state.panOffset.x) / state.zoom,
                                (offset.y - state.panOffset.y) / state.zoom
                            )
                            handleCanvasTap(state, adjustedOffset)
                        }
                        isDragging = false
                        totalDragDistance = 0f
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
                        val clickedRelationship = if (clickedEntity == null) {
                            state.diagram.relationships.find {
                                isPointInDiamond(adjustedOffset, it)
                            }
                        } else null
                        val clickedNote = if (clickedEntity == null && clickedRelationship == null) {
                            state.diagram.notes.find { note ->
                                adjustedOffset.x >= note.x && adjustedOffset.x <= note.x + note.width &&
                                adjustedOffset.y >= note.y && adjustedOffset.y <= note.y + note.height
                            }
                        } else null
                        clickedEntity?.let {
                            state.selectEntity(it.id)
                            onContextMenuRequest?.invoke(offset, ContextMenuType.ENTITY)
                        }
                        clickedRelationship?.let {
                            state.selectRelationship(it.id)
                            onContextMenuRequest?.invoke(offset, ContextMenuType.RELATIONSHIP)
                        }
                        clickedNote?.let {
                            state.selectNote(it.id)
                            onContextMenuRequest?.invoke(offset, ContextMenuType.NOTE)
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
                            val clickedRelationship = if (clickedEntity == null) {
                                state.diagram.relationships.find {
                                    isPointInDiamond(adjustedOffset, it)
                                }
                            } else null
                            val clickedNote = if (clickedEntity == null && clickedRelationship == null) {
                                state.diagram.notes.find { note ->
                                    adjustedOffset.x >= note.x && adjustedOffset.x <= note.x + note.width &&
                                    adjustedOffset.y >= note.y && adjustedOffset.y <= note.y + note.height
                                }
                            } else null
                                clickedEntity?.let {
                                    state.selectEntity(it.id)
                                    onContextMenuRequest?.invoke(position, ContextMenuType.ENTITY)
                                }
                                clickedRelationship?.let {
                                    state.selectRelationship(it.id)
                                    onContextMenuRequest?.invoke(position, ContextMenuType.RELATIONSHIP)
                                }
                                clickedNote?.let {
                                    state.selectNote(it.id)
                                    onContextMenuRequest?.invoke(position, ContextMenuType.NOTE)
                                }
                        }
                    }
                }
            }
            .pointerInput(state.selectedEntityId, state.selectedRelationshipId) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = false
                        totalDragDistance = 0f
                        draggedAttributeInfo = handleDragStart(state, offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDistance += sqrt(
                            dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y
                        )
                        if (totalDragDistance > 5f) {
                            isDragging = true
                        }
                        if (isDragging) {
                            handleDrag(state, dragAmount, draggedAttributeInfo.first, draggedAttributeInfo.second, draggedAttributeInfo.third)
                        }
                    },
                    onDragEnd = {
                        if (!isDragging) {
                            totalDragDistance = 0f
                        }
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
            drawGrid(size, gridColor, state.zoom, state.panOffset)
            state.diagram.relationships.forEach { relationship ->
                drawConnectionsForRelationship(
                    relationship = relationship,
                    entities = state.diagram.entities,
                    color = connectionColor,
                    textMeasurer = textMeasurer,
                    cardinalityColor = cardinalityColor,
                    textBackgroundColor = textBackgroundColor
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
                    textMeasurer = textMeasurer,
                    attributeBackgroundColor = attributeBackgroundColor,
                    normalAttributeColor = normalAttributeColor,
                    primaryKeyColor = primaryKeyColor,
                    compositeColor = compositeColor,
                    componentColor = componentColor,
                    textBackgroundColor = textBackgroundColor
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
                    textMeasurer = textMeasurer,
                    attributeBackgroundColor = attributeBackgroundColor,
                    normalAttributeColor = normalAttributeColor,
                    primaryKeyColor = primaryKeyColor,
                    compositeColor = compositeColor,
                    componentColor = componentColor,
                    textBackgroundColor = textBackgroundColor
                )
            }
            state.diagram.notes.forEach { note ->
                val isSelected = state.selectedNoteId == note.id
                drawNote(
                    note = note,
                    isSelected = isSelected,
                    textMeasurer = textMeasurer,
                    selectedColor = selectedColor
                )
            }
        }
    }
}
private fun DrawScope.drawGrid(canvasSize: Size, color: Color, zoom: Float, panOffset: Offset, spacing: Float = 20f) {
    val viewportWidth = canvasSize.width / zoom
    val viewportHeight = canvasSize.height / zoom
    val startX = -panOffset.x / zoom
    val startY = -panOffset.y / zoom
    val endX = startX + viewportWidth
    val endY = startY + viewportHeight
    val firstVerticalLine = (startX / spacing).toInt() * spacing
    val firstHorizontalLine = (startY / spacing).toInt() * spacing
    val numVerticalLines = ((endX - firstVerticalLine) / spacing).toInt() + 2
    val numHorizontalLines = ((endY - firstHorizontalLine) / spacing).toInt() + 2
    for (i in 0..numVerticalLines) {
        val x = firstVerticalLine + i * spacing
        drawLine(
            color = color,
            start = Offset(x, startY),
            end = Offset(x, endY),
            strokeWidth = 1f
        )
    }
    for (i in 0..numHorizontalLines) {
        val y = firstHorizontalLine + i * spacing
        drawLine(
            color = color,
            start = Offset(startX, y),
            end = Offset(endX, y),
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
    textMeasurer: TextMeasurer,
    attributeBackgroundColor: Color,
    normalAttributeColor: Color,
    primaryKeyColor: Color,
    compositeColor: Color,
    componentColor: Color,
    textBackgroundColor: Color
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
            fontWeight = if (isSelected || isHovered) FontWeight.SemiBold else FontWeight.Normal
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
                isRectangle = true,
                attributeBackgroundColor = attributeBackgroundColor,
                normalAttributeColor = normalAttributeColor,
                primaryKeyColor = primaryKeyColor,
                compositeColor = compositeColor,
                componentColor = componentColor,
                textBackgroundColor = textBackgroundColor
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
    textMeasurer: TextMeasurer,
    attributeBackgroundColor: Color,
    normalAttributeColor: Color,
    primaryKeyColor: Color,
    compositeColor: Color,
    componentColor: Color,
    textBackgroundColor: Color
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
            fontWeight = if (isSelected || isHovered) FontWeight.SemiBold else FontWeight.Normal
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
                isRectangle = false,
                attributeBackgroundColor = attributeBackgroundColor,
                normalAttributeColor = normalAttributeColor,
                primaryKeyColor = primaryKeyColor,
                compositeColor = compositeColor,
                componentColor = componentColor,
                textBackgroundColor = textBackgroundColor
            )
        }
    }
}
private fun DrawScope.drawNote(
    note: Note,
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
    selectedColor: Color
) {
    val noteColor = Color(0xFFFFEB3B)
    val strokeColor = if (isSelected) selectedColor else Color(0xFFFBC02D)
    val strokeWidth = if (isSelected) 3f else 1.5f
    drawRect(
        color = noteColor,
        topLeft = Offset(note.x, note.y),
        size = Size(note.width, note.height),
        style = Fill
    )
    drawRect(
        color = strokeColor,
        topLeft = Offset(note.x, note.y),
        size = Size(note.width, note.height),
        style = Stroke(width = strokeWidth)
    )
    val foldSize = 15f
    val foldPath = Path().apply {
        moveTo(note.x + note.width - foldSize, note.y)
        lineTo(note.x + note.width, note.y + foldSize)
        lineTo(note.x + note.width - foldSize, note.y + foldSize)
        close()
    }
    drawPath(
        path = foldPath,
        color = Color(0xFFF9A825),
        style = Fill
    )
    drawPath(
        path = foldPath,
        color = strokeColor,
        style = Stroke(width = 1f)
    )
    val padding = 10f
    val availableWidth = note.width - (padding * 2)
    val availableHeight = note.height - (padding * 2)
    val textLayoutResult = textMeasurer.measure(
        text = note.text,
        style = TextStyle(
            color = Color.Black,
            fontSize = 12.sp
        ),
        constraints = androidx.compose.ui.unit.Constraints(
            maxWidth = availableWidth.toInt(),
            maxHeight = availableHeight.toInt()
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            note.x + padding,
            note.y + padding
        )
    )
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
    isRectangle: Boolean,
    attributeBackgroundColor: Color,
    normalAttributeColor: Color,
    primaryKeyColor: Color,
    compositeColor: Color,
    componentColor: Color,
    textBackgroundColor: Color
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
    val distance = sqrt(dx * dx + dy * dy)
    val dirX = if (distance > 0) dx / distance else 1f
    val dirY = if (distance > 0) dy / distance else 0f
    val arrowStartX = attrX - dirX * arrowLength
    val arrowStartY = attrY - dirY * arrowLength
    val connectionPoint = if (isRectangle) {
        getClosestPointOnRectangle(entityX, entityY, entityWidth, entityHeight, arrowStartX, arrowStartY)
    } else {
        getClosestPointOnDiamond(centerX, centerY, entityWidth, entityHeight, arrowStartX, arrowStartY)
    }
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
                        textMeasurer = textMeasurer,
                        attributeBackgroundColor = attributeBackgroundColor,
                        compositeColor = compositeColor,
                        componentColor = componentColor,
                        textBackgroundColor = textBackgroundColor
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
                FontWeight.Bold
            else
                FontWeight.SemiBold
        )
    )
    val isComposite = attribute.type == AttributeType.COMPOSITE && attribute.components.isNotEmpty()
    val textX = if (isComposite) {
        attrX - radius - textLayoutResult.size.width.toFloat() - 10
    } else {
        attrX + radius + 10
    }
    val textY = attrY - textLayoutResult.size.height / 2
    drawRoundRect(
        color = textBackgroundColor,
        topLeft = Offset(textX - 5, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 10,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = attributeTextColor.copy(alpha = 0.5f),
        topLeft = Offset(textX - 5, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 10,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = CornerRadius(4f, 4f),
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
                fontWeight = FontWeight.Normal
            )
        )
        val multY = textY + textLayoutResult.size.height + 4
        drawRoundRect(
            color = textBackgroundColor,
            topLeft = Offset(textX - 3, multY - 1),
            size = Size(
                multiplicityText.size.width.toFloat() + 6,
                multiplicityText.size.height.toFloat() + 2
            ),
            cornerRadius = CornerRadius(3f, 3f)
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
    textMeasurer: TextMeasurer,
    attributeBackgroundColor: Color,
    compositeColor: Color,
    componentColor: Color,
    textBackgroundColor: Color
) {
    val radius = 12f
    val horizontalSpacing = 60f
    val verticalSpacing = 40f
    val startY = parentY - ((total - 1) * verticalSpacing / 2f)
    val compX = parentX + horizontalSpacing
    val compY = startY + (index * verticalSpacing)
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
            fontWeight = FontWeight.Normal
        )
    )
    val textX = compX + radius + 8
    val textY = compY - textLayoutResult.size.height / 2
    drawRoundRect(
        color = textBackgroundColor,
        topLeft = Offset(textX - 4, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 8,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = componentColor.copy(alpha = 0.4f),
        topLeft = Offset(textX - 4, textY - 2),
        size = Size(
            textLayoutResult.size.width.toFloat() + 8,
            textLayoutResult.size.height.toFloat() + 4
        ),
        cornerRadius = CornerRadius(3f, 3f),
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
    textMeasurer: TextMeasurer,
    cardinalityColor: Color,
    textBackgroundColor: Color
) {
    val centerX = relationship.x + relationship.width / 2
    val centerY = relationship.y + relationship.height / 2
    relationship.connections.forEach { connection ->
        val entity = entities.find { it.id == connection.entityId }
        entity?.let {
            val entityCenterX = it.x + it.width / 2
            val entityCenterY = it.y + it.height / 2
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
                    fontWeight = FontWeight.Bold
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
                color = textBackgroundColor,
                topLeft = Offset(labelX - 6, labelY - 3),
                size = Size(
                    textWidth + 12,
                    textHeight + 6
                ),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = cardinalityColor.copy(alpha = 0.6f),
                topLeft = Offset(labelX - 6, labelY - 3),
                size = Size(
                    textWidth + 12,
                    textHeight + 6
                ),
                cornerRadius = CornerRadius(4f, 4f),
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
            clickedRelationship?.let {
                state.selectRelationship(it.id)
                return
            }
            val clickedEntity = state.diagram.entities.firstOrNull { entity ->
                offset.x >= entity.x && offset.x <= entity.x + entity.width &&
                offset.y >= entity.y && offset.y <= entity.y + entity.height
            }
            clickedEntity?.let {
                state.selectEntity(it.id)
                return
            }
            val clickedNote = state.diagram.notes.firstOrNull { note ->
                offset.x >= note.x && offset.x <= note.x + note.width &&
                offset.y >= note.y && offset.y <= note.y + note.height
            }
            clickedNote?.let {
                state.selectNote(it.id)
            } ?: run {
                state.clearSelection()
            }
        }
        ToolMode.ENTITY -> {
            state.addEntity(offset.x - 70f, offset.y - 35f, "Nuova Entità")
            state.toolMode = ToolMode.SELECT
        }
        ToolMode.RELATIONSHIP -> {
            state.addRelationship(offset.x - 60f, offset.y - 60f, "Nuova Relazione")
            state.toolMode = ToolMode.SELECT
        }
        ToolMode.NOTE -> {
            state.addNote(offset.x - 105f, offset.y - 77.5f, "Nuova Nota")
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
            val distance = sqrt(
                (adjustedOffset.x - attrPos.x).let { it * it } +
                (adjustedOffset.y - attrPos.y).let { it * it }
            )
            if (distance <= 30f) {
                state.saveDragStartState()
                return Triple(attribute.id, entity.id, null)
            }
        }
    }
    state.diagram.relationships.forEach { relationship ->
        relationship.attributes.forEachIndexed { index, attribute ->
            val attrPos = calculateAttributePosition(
                relationship.x,
                relationship.y,
                relationship.width,
                relationship.height,
                index,
                relationship.attributes.size,
                attribute
            )
            val distance = sqrt(
                (adjustedOffset.x - attrPos.x).let { it * it } +
                (adjustedOffset.y - attrPos.y).let { it * it }
            )
            if (distance <= 30f) {
                state.saveDragStartState()
                return Triple(attribute.id, null, relationship.id)
            }
        }
    }
    val entity = state.diagram.entities.find { entity ->
        adjustedOffset.x >= entity.x && adjustedOffset.x <= entity.x + entity.width &&
        adjustedOffset.y >= entity.y && adjustedOffset.y <= entity.y + entity.height
    }
    entity?.let {
        state.selectEntity(it.id)
        state.saveDragStartState()
        return Triple(null, null, null)
    }
    val relationship = state.diagram.relationships.find { rel ->
        isPointInDiamond(adjustedOffset, rel)
    }
    relationship?.let {
        state.selectRelationship(it.id)
        state.saveDragStartState()
        return Triple(null, null, null)
    }
    val note = state.diagram.notes.find { note ->
        adjustedOffset.x >= note.x && adjustedOffset.x <= note.x + note.width &&
        adjustedOffset.y >= note.y && adjustedOffset.y <= note.y + note.height
    }
    note?.let {
        state.selectNote(it.id)
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
    draggedAttributeId?.let { attrId ->
        draggedAttributeForEntity?.let {
            state.updateEntityWithoutSave(it) { entity ->
                entity.copy(
                    attributes = entity.attributes.mapIndexed { index, attr ->
                        if (attr.id == attrId) {
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
                        val currentDistanceFromCenter = sqrt(currentDx * currentDx + currentDy * currentDy)
                        val fixedDistance = if (currentDistanceFromCenter > 0) currentDistanceFromCenter else (entity.width / 2 + 60f)
                        val newAttrX = currentAttrX + dragAmount.x / state.zoom
                        val newAttrY = currentAttrY + dragAmount.y / state.zoom
                        val dx = newAttrX - centerX
                        val dy = newAttrY - centerY
                        val currentDistance = sqrt(dx * dx + dy * dy)
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
    }
    draggedAttributeId?.let { attrId ->
        draggedAttributeForRelationship?.let {
            state.updateRelationshipWithoutSave(it) { rel ->
            rel.copy(
                attributes = rel.attributes.mapIndexed { index, attr ->
                    if (attr.id == attrId) {
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
                        val currentDistanceFromCenter = sqrt(currentDx * currentDx + currentDy * currentDy)
                        val fixedDistance = if (currentDistanceFromCenter > 0) currentDistanceFromCenter else (rel.width / 2 + 60f)
                        val newAttrX = currentAttrX + dragAmount.x / state.zoom
                        val newAttrY = currentAttrY + dragAmount.y / state.zoom
                        val dx = newAttrX - centerX
                        val dy = newAttrY - centerY
                        val currentDistance = sqrt(dx * dx + dy * dy)
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
    }
    state.selectedEntityId?.let {
        state.updateEntityWithoutSave(it) { entity ->
            entity.copy(
                x = entity.x + dragAmount.x / state.zoom,
                y = entity.y + dragAmount.y / state.zoom
            )
        }
    }
    state.selectedRelationshipId?.let {
        state.updateRelationshipWithoutSave(it) { rel ->
            rel.copy(
                x = rel.x + dragAmount.x / state.zoom,
                y = rel.y + dragAmount.y / state.zoom
            )
        }
    }
    state.selectedNoteId?.let {
        state.updateNoteWithoutSave(it) { note ->
            note.copy(
                x = note.x + dragAmount.x / state.zoom,
                y = note.y + dragAmount.y / state.zoom
            )
        }
    }
}
private fun isPointInDiamond(point: Offset, relationship: Relationship): Boolean {
    val centerX = relationship.x + relationship.width / 2
    val centerY = relationship.y + relationship.height / 2
    val dx = abs(point.x - centerX) / (relationship.width / 2)
    val dy = abs(point.y - centerY) / (relationship.height / 2)
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
    if (attribute.x != 0f || attribute.y != 0f) return Offset(centerX + attribute.x, centerY + attribute.y)
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
    val scaleX = if (dx != 0f) (rectWidth / 2) / abs(dx) else Float.MAX_VALUE
    val scaleY = if (dy != 0f) (rectHeight / 2) / abs(dy) else Float.MAX_VALUE
    val scale = minOf(scaleX, scaleY)
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
    val totalScale = 1f / (abs(dx) / halfWidth + abs(dy) / halfHeight)
    return Offset(
        centerX + dx * totalScale,
        centerY + dy * totalScale
    )
}
