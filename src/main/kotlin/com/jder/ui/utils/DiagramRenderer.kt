package com.jder.ui.utils
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jder.domain.model.AttributeType
import com.jder.domain.model.ERDiagram
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.sqrt
fun renderDiagramToBitmap(diagram: ERDiagram): ImageBitmap {
    val padding = 200f
    val entities = diagram.entities
    val relationships = diagram.relationships
    val allXCoords = mutableListOf<Float>()
    val allYCoords = mutableListOf<Float>()
    entities.forEach { entity ->
        allXCoords.add(entity.x)
        allXCoords.add(entity.x + entity.width)
        allYCoords.add(entity.y)
        allYCoords.add(entity.y + entity.height)
        entity.attributes.forEachIndexed { index, attr ->
            val centerX = entity.x + entity.width / 2
            val centerY = entity.y + entity.height / 2
            val arrowLength = 60f
            val verticalSpacing = 60f
            val startY = centerY - ((entity.attributes.size - 1) * verticalSpacing / 2f)
            val defaultAttrX = entity.x + entity.width + arrowLength
            val defaultAttrY = startY + (index * verticalSpacing)
            val attrX = if (attr.x != 0f) centerX + attr.x else defaultAttrX
            val attrY = if (attr.y != 0f) centerY + attr.y else defaultAttrY
            allXCoords.add(attrX - 20)
            allXCoords.add(attrX + 150)
            allYCoords.add(attrY - 20)
            allYCoords.add(attrY + 20)
            if (attr.type == AttributeType.COMPOSITE && attr.components.isNotEmpty()) {
                attr.components.forEachIndexed { compIndex, _ ->
                    val horizontalSpacing = 60f
                    val compVerticalSpacing = 40f
                    val compStartY = attrY - ((attr.components.size - 1) * compVerticalSpacing / 2f)
                    val compX = attrX + 20 + horizontalSpacing
                    val compY = compStartY + (compIndex * compVerticalSpacing)
                    allXCoords.add(compX - 12)
                    allXCoords.add(compX + 100)
                    allYCoords.add(compY - 12)
                    allYCoords.add(compY + 12)
                }
            }
        }
    }
    relationships.forEach { rel ->
        allXCoords.add(rel.x)
        allXCoords.add(rel.x + rel.width)
        allYCoords.add(rel.y)
        allYCoords.add(rel.y + rel.height)
        rel.attributes.forEachIndexed { index, attr ->
            val centerX = rel.x + rel.width / 2
            val centerY = rel.y + rel.height / 2
            val arrowLength = 60f
            val verticalSpacing = 60f
            val startY = centerY - ((rel.attributes.size - 1) * verticalSpacing / 2f)
            val defaultAttrX = rel.x + rel.width + arrowLength
            val defaultAttrY = startY + (index * verticalSpacing)
            val attrX = if (attr.x != 0f) centerX + attr.x else defaultAttrX
            val attrY = if (attr.y != 0f) centerY + attr.y else defaultAttrY
            allXCoords.add(attrX - 20)
            allXCoords.add(attrX + 150)
            allYCoords.add(attrY - 20)
            allYCoords.add(attrY + 20)
            if (attr.type == AttributeType.COMPOSITE && attr.components.isNotEmpty()) {
                attr.components.forEachIndexed { compIndex, _ ->
                    val horizontalSpacing = 60f
                    val compVerticalSpacing = 40f
                    val compStartY = attrY - ((attr.components.size - 1) * compVerticalSpacing / 2f)
                    val compX = attrX + 20 + horizontalSpacing
                    val compY = compStartY + (compIndex * compVerticalSpacing)
                    allXCoords.add(compX - 12)
                    allXCoords.add(compX + 100)
                    allYCoords.add(compY - 12)
                    allYCoords.add(compY + 12)
                }
            }
        }
    }
    val minX = (allXCoords.minOrNull() ?: 0f) - padding
    val minY = (allYCoords.minOrNull() ?: 0f) - padding
    val maxX = (allXCoords.maxOrNull() ?: 1000f) + padding
    val maxY = (allYCoords.maxOrNull() ?: 1000f) + padding
    val width = (maxX - minX).toInt().coerceAtLeast(800)
    val height = (maxY - minY).toInt().coerceAtLeast(600)
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g2d = bufferedImage.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2d.color = Color.WHITE
    g2d.fillRect(0, 0, width, height)
    val offsetX = -minX
    val offsetY = -minY
    g2d.color = Color(0xBDBDBD)
    g2d.stroke = BasicStroke(2f)
    relationships.forEach { relationship ->
        val centerX = (relationship.x + relationship.width / 2 + offsetX).toInt()
        val centerY = (relationship.y + relationship.height / 2 + offsetY).toInt()
        relationship.connections.forEach { conn ->
            val entity = entities.find { it.id == conn.entityId }
            entity?.let {
                val entityCenterX = (it.x + it.width / 2 + offsetX).toInt()
                val entityCenterY = (it.y + it.height / 2 + offsetY).toInt()
                g2d.drawLine(centerX, centerY, entityCenterX, entityCenterY)
                val labelX = (centerX + entityCenterX) / 2
                val labelY = (centerY + entityCenterY) / 2
                g2d.font = Font("Arial", Font.BOLD, 14)
                g2d.color = Color(0x222222)
                g2d.drawString(conn.cardinality.display, labelX - 10, labelY)
                g2d.color = Color(0xBDBDBD)
            }
        }
    }
    g2d.stroke = BasicStroke(2.5f)
    entities.forEach { entity ->
        val x = (entity.x + offsetX).toInt()
        val y = (entity.y + offsetY).toInt()
        val w = entity.width.toInt()
        val h = entity.height.toInt()
        g2d.color = Color.WHITE
        g2d.fillRect(x, y, w, h)
        g2d.color = Color(0x64B5F6)
        g2d.drawRect(x, y, w, h)
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 14)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(entity.name)
        g2d.drawString(entity.name, x + (w - textWidth) / 2, y + h / 2 + fm.ascent / 2)
        entity.attributes.forEachIndexed { index, attribute ->
            val arrowLength = 60
            val verticalSpacing = 45
            val centerX = entity.x + entity.width / 2
            val centerY = entity.y + entity.height / 2
            val startY = centerY - ((entity.attributes.size - 1) * verticalSpacing / 2f)
            val defaultAttrX = entity.x + entity.width + arrowLength
            val defaultAttrY = startY + (index * verticalSpacing)
            val attrX = if (attribute.x != 0f) (centerX + attribute.x + offsetX).toInt() else (defaultAttrX + offsetX).toInt()
            val attrY = if (attribute.y != 0f) (centerY + attribute.y + offsetY).toInt() else (defaultAttrY + offsetY).toInt()
            val radius = 20
            val entityCenterX = (centerX + offsetX).toInt()
            val entityCenterY = (centerY + offsetY).toInt()
            val dx = attrX - entityCenterX
            val dy = attrY - entityCenterY
            val distance = sqrt((dx * dx + dy * dy).toDouble())
            val dirX = if (distance > 0) dx / distance else 1.0
            val dirY = if (distance > 0) dy / distance else 0.0
            val arrowStartX = (attrX - dirX * arrowLength).toInt()
            val arrowStartY = (attrY - dirY * arrowLength).toInt()
            val halfWidth = entity.width / 2
            val halfHeight = entity.height / 2
            val dxToStart = arrowStartX - entityCenterX
            val dyToStart = arrowStartY - entityCenterY
            val scaleX = if (dxToStart != 0) halfWidth / abs(dxToStart) else Float.MAX_VALUE
            val scaleY = if (dyToStart != 0) halfHeight / abs(dyToStart) else Float.MAX_VALUE
            val scale = minOf(scaleX, scaleY)
            val connectionX = (entityCenterX + dxToStart * scale).toInt()
            val connectionY = (entityCenterY + dyToStart * scale).toInt()
            g2d.color = Color(0xBDBDBD)
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine(connectionX, connectionY, attrX, attrY)
            g2d.color = Color.WHITE
            g2d.fillOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
            val attrColor = when {
                attribute.isPrimaryKey -> Color(0xFFEB3B)
                attribute.type == AttributeType.COMPOSITE -> Color(0xFFA726)
                else -> Color(0x90CAF9)
            }
            g2d.color = attrColor
            when (attribute.type) {
                AttributeType.COMPOSITE -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                    g2d.drawOval(attrX - radius + 5, attrY - radius + 5, (radius - 5) * 2, (radius - 5) * 2)
                }
                AttributeType.MULTIVALUED -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                    g2d.drawOval(attrX - radius + 5, attrY - radius + 5, (radius - 5) * 2, (radius - 5) * 2)
                }
                else -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                }
            }
            g2d.font = Font("Arial", Font.BOLD, 12)
            g2d.color = Color.BLACK
            val isComposite = attribute.type == AttributeType.COMPOSITE && attribute.components.isNotEmpty()
            val textX = if (isComposite) {
                val attrFm = g2d.fontMetrics
                val attrTextWidth = attrFm.stringWidth(attribute.name)
                attrX - radius - attrTextWidth - 10
            } else {
                attrX + radius + 10
            }
            g2d.drawString(attribute.name, textX, attrY + 5)
            if (attribute.type == AttributeType.COMPOSITE && attribute.components.isNotEmpty()) {
                val compRadius = 12
                val horizontalSpacing = 60
                val compVerticalSpacing = 40
                val compStartY = attrY - ((attribute.components.size - 1) * compVerticalSpacing / 2)
                attribute.components.forEachIndexed { compIndex, component ->
                    val compX = attrX + radius + horizontalSpacing
                    val compY = compStartY + (compIndex * compVerticalSpacing)
                    g2d.color = Color(0xBDBDBD)
                    g2d.stroke = BasicStroke(1.5f)
                    g2d.drawLine(attrX + radius, attrY, compX, compY)
                    g2d.color = Color.WHITE
                    g2d.fillOval(compX - compRadius, compY - compRadius, compRadius * 2, compRadius * 2)
                    g2d.color = Color(0xFFA726)
                    g2d.stroke = BasicStroke(2f)
                    g2d.drawOval(compX - compRadius, compY - compRadius, compRadius * 2, compRadius * 2)
                    g2d.font = Font("Arial", Font.PLAIN, 11)
                    g2d.color = Color.BLACK
                    g2d.drawString(component.name, compX + compRadius + 8, compY + 4)
                }
            }
        }
    }
    relationships.forEach { relationship ->
        val centerX = (relationship.x + relationship.width / 2 + offsetX).toInt()
        val centerY = (relationship.y + relationship.height / 2 + offsetY).toInt()
        val halfWidth = (relationship.width / 2).toInt()
        val halfHeight = (relationship.height / 2).toInt()
        val xPoints = intArrayOf(
            centerX,
            centerX + halfWidth,
            centerX,
            centerX - halfWidth
        )
        val yPoints = intArrayOf(
            centerY - halfHeight,
            centerY,
            centerY + halfHeight,
            centerY
        )
        g2d.color = Color.WHITE
        g2d.fillPolygon(xPoints, yPoints, 4)
        g2d.color = Color(0xE57373)
        g2d.drawPolygon(xPoints, yPoints, 4)
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 14)
        val fm = g2d.fontMetrics
        val textWidth = fm.stringWidth(relationship.name)
        g2d.drawString(relationship.name, centerX - textWidth / 2, centerY + fm.ascent / 2)
        relationship.attributes.forEachIndexed { index, attribute ->
            val arrowLength = 60
            val verticalSpacing = 45
            val relCenterX = relationship.x + relationship.width / 2
            val relCenterY = relationship.y + relationship.height / 2
            val startY = relCenterY - ((relationship.attributes.size - 1) * verticalSpacing / 2f)
            val defaultAttrX = relationship.x + relationship.width + arrowLength
            val defaultAttrY = startY + (index * verticalSpacing)
            val attrX = if (attribute.x != 0f) (relCenterX + attribute.x + offsetX).toInt() else (defaultAttrX + offsetX).toInt()
            val attrY = if (attribute.y != 0f) (relCenterY + attribute.y + offsetY).toInt() else (defaultAttrY + offsetY).toInt()
            val radius = 20
            val dx = attrX - centerX
            val dy = attrY - centerY
            val distance = sqrt((dx * dx + dy * dy).toDouble())
            val dirX = if (distance > 0) dx / distance else 1.0
            val dirY = if (distance > 0) dy / distance else 0.0
            val arrowStartX = (attrX - dirX * arrowLength).toInt()
            val arrowStartY = (attrY - dirY * arrowLength).toInt()
            val dxToStart = arrowStartX - centerX
            val dyToStart = arrowStartY - centerY
            val halfWidth2 = relationship.width / 2f
            val halfHeight2 = relationship.height / 2f
            val totalScale = 1f / (abs(dxToStart.toFloat()) / halfWidth2 + abs(dyToStart.toFloat()) / halfHeight2)
            val connectionX = (centerX + dxToStart * totalScale).toInt()
            val connectionY = (centerY + dyToStart * totalScale).toInt()
            g2d.color = Color(0xBDBDBD)
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine(connectionX, connectionY, attrX, attrY)
            g2d.color = Color.WHITE
            g2d.fillOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
            val attrColor = when {
                attribute.isPrimaryKey -> Color(0xFFEB3B)
                attribute.type == AttributeType.COMPOSITE -> Color(0xFFA726)
                else -> Color(0x90CAF9)
            }
            g2d.color = attrColor
            when (attribute.type) {
                AttributeType.COMPOSITE -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                    g2d.drawOval(attrX - radius + 5, attrY - radius + 5, (radius - 5) * 2, (radius - 5) * 2)
                }
                AttributeType.MULTIVALUED -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                    g2d.drawOval(attrX - radius + 5, attrY - radius + 5, (radius - 5) * 2, (radius - 5) * 2)
                }
                else -> {
                    g2d.drawOval(attrX - radius, attrY - radius, radius * 2, radius * 2)
                }
            }
            g2d.font = Font("Arial", Font.BOLD, 12)
            g2d.color = Color.BLACK
            val isComposite = attribute.type == AttributeType.COMPOSITE && attribute.components.isNotEmpty()
            val textX = if (isComposite) {
                val attrFm = g2d.fontMetrics
                val attrTextWidth = attrFm.stringWidth(attribute.name)
                attrX - radius - attrTextWidth - 10
            } else {
                attrX + radius + 10
            }
            g2d.drawString(attribute.name, textX, attrY + 5)
            if (attribute.type == AttributeType.COMPOSITE && attribute.components.isNotEmpty()) {
                val compRadius = 12
                val horizontalSpacing = 60
                val compVerticalSpacing = 40
                val compStartY = attrY - ((attribute.components.size - 1) * compVerticalSpacing / 2)
                attribute.components.forEachIndexed { compIndex, component ->
                    val compX = attrX + radius + horizontalSpacing
                    val compY = compStartY + (compIndex * compVerticalSpacing)
                    g2d.color = Color(0xBDBDBD)
                    g2d.stroke = BasicStroke(1.5f)
                    g2d.drawLine(attrX + radius, attrY, compX, compY)
                    g2d.color = Color.WHITE
                    g2d.fillOval(compX - compRadius, compY - compRadius, compRadius * 2, compRadius * 2)
                    g2d.color = Color(0xFFA726)
                    g2d.stroke = BasicStroke(2f)
                    g2d.drawOval(compX - compRadius, compY - compRadius, compRadius * 2, compRadius * 2)
                    g2d.font = Font("Arial", Font.PLAIN, 11)
                    g2d.color = Color.BLACK
                    g2d.drawString(component.name, compX + compRadius + 8, compY + 4)
                }
            }
        }
    }
    g2d.dispose()
    return bufferedImage.toComposeImageBitmap()
}
