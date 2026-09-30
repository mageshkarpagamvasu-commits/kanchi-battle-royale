package com.example.kanchibattleroyale

import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val roster = listOf(
    CharacterSpec("Vimal Don", 120f, 205f, 0.29f, Color(0xFFFBBF24)),
    CharacterSpec("Bottle Mani", 100f, 210f, 0.19f, Color(0xFF38BDF8)),
    CharacterSpec("Aunty Madhan", 150f, 185f, 0.30f, Color(0xFFFB923C)),
    CharacterSpec("Vidhakaran Vishnu", 100f, 250f, 0.28f, Color(0xFFA78BFA)),
    CharacterSpec("Chellam Keerthi", 110f, 215f, 0.27f, Color(0xFFF472B6))
)

private data class CharacterSpec(
    val name: String,
    val hp: Float,
    val speed: Float,
    val rate: Float,
    val color: Color
)

private data class Actor(
    val name: String,
    val color: Color,
    val speed: Float,
    val rate: Float,
    val maxHp: Float,
    var x: Float,
    var y: Float,
    var hp: Float = maxHp,
    var alive: Boolean = true,
    var player: Boolean = false,
    var ammo: Int = 110,
    var kills: Int = 0,
    var cooldown: Float = 0f,
    var angle: Float = 0f,
    var wander: Float = 0f,
    var kind: Int = 0,
)

private data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var owner: Actor,
    var life: Float = 0.9f
)

private data class Loot(
    var x: Float,
    var y: Float,
    val type: String
)

private data class Building(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: Color
)

class GameState {
    val world = 2200f
    val zoneOrigin = Offset(1100f, 1100f)
    var player: Actor? = null
    val actors = mutableStateListOf<Actor>()
    val bullets = mutableStateListOf<Bullet>()
    val loot = mutableStateListOf<Loot>()
    val buildings = mutableStateListOf<Building>()
    var running = false
    var elapsed = 0f
    var zone = 1450f
    var firePressed = false
    var moveX = 0f
    var moveY = 0f
    var moveStickX = 0f
    var moveStickY = 0f
    var dragStartX = 0f
    var dragStartY = 0f
    var dragActive = false

    fun start(characterIndex: Int) {
        generateBuildings()
        val spec = roster[characterIndex]
        player = Actor(
            name = spec.name,
            color = spec.color,
            speed = spec.speed,
            rate = spec.rate,
            maxHp = spec.hp,
            x = 0f,
            y = 0f,
            hp = spec.hp,
            ammo = 110,
            player = true,
            kind = characterIndex
        )
        player!!.x = 1100f
        player!!.y = 1100f
        actors.clear()
        actors.add(player!!)

        repeat(17) { index ->
            val specEnemy = roster[index % roster.size]
            var pos = spawnPoint()
            var attempt = 0
            while (attempt < 200 && hypot(pos.x - player!!.x, pos.y - player!!.y) < 350f) {
                pos = spawnPoint()
                attempt++
            }
            actors.add(
                Actor(
                    name = specEnemy.name,
                    color = specEnemy.color,
                    speed = specEnemy.speed,
                    rate = specEnemy.rate,
                    maxHp = specEnemy.hp,
                    x = pos.x,
                    y = pos.y,
                    hp = specEnemy.hp,
                    ammo = 100,
                    alive = true,
                    player = false,
                    kind = index
                )
            )
        }

        loot.clear()
        repeat(55) { index ->
            loot.add(Loot(x = spawnPoint().x, y = spawnPoint().y, type = if (index % 3 == 0) "health" else "ammo"))
        }

        bullets.clear()
        elapsed = 0f
        zone = 1450f
        firePressed = false
        moveX = 0f
        moveY = 0f
        running = true
    }

    fun update(dt: Float) {
        if (!running || player == null) return

        elapsed += dt
        zone = max(85f, 1450f - elapsed * 6f)

        val playerActor = player!!
        val length = hypot(moveX.toDouble(), moveY.toDouble()).toFloat()
        var dirX = moveX
        var dirY = moveY
        if (length > 1f) {
            dirX /= length
            dirY /= length
        }

        moveActor(playerActor, dirX * playerActor.speed * dt, dirY * playerActor.speed * dt)

        for (actor in actors) {
            if (!actor.alive) continue
            actor.cooldown = (actor.cooldown - dt).coerceAtLeast(0f)

            if (hypot(actor.x - 1100f, actor.y - 1100f) > zone) {
                actor.hp -= 12f * dt
                if (actor.hp <= 0f) {
                    actor.alive = false
                    continue
                }
            } else if (actor.kind % roster.size == 4) {
                actor.hp = min(actor.maxHp, actor.hp + 1.5f * dt)
            }

            if (actor.player) {
                if (firePressed) {
                    val target = nearestEnemy(actor)
                    if (target != null && hypot(target.x - actor.x, target.y - actor.y) < 570f) {
                        shoot(actor, target)
                    }
                }
                continue
            }

            val target = nearestEnemy(actor)
            if (target != null) {
                val dist = hypot(target.x - actor.x, target.y - actor.y)
                val angleToTarget = kotlin.math.atan2(target.y - actor.y, target.x - actor.x)
                if (dist < 500f) {
                    val advance = if (dist > 230f) 1f else if (dist < 140f) -1f else 0f
                    val strafe = 0.4f
                    val moveXStep = cos(angleToTarget) * advance + cos(angleToTarget + Math.PI / 2.0) * strafe
                    val moveYStep = sin(angleToTarget) * advance + sin(angleToTarget + Math.PI / 2.0) * strafe
                    moveActor(actor, moveXStep.toFloat() * actor.speed * 0.65f * dt, moveYStep.toFloat() * actor.speed * 0.65f * dt)
                    if (dist < 460f) shoot(actor, target)
                } else {
                    actor.wander -= dt
                    if (actor.wander <= 0f) {
                        actor.angle = (0f..(Math.PI * 2.0f)).random().toFloat()
                        actor.wander = (1f..3f).random()
                    }
                    val aiX = cos(actor.angle)
                    val aiY = sin(actor.angle)
                    moveActor(actor, aiX * actor.speed * 0.65f * dt, aiY * actor.speed * 0.65f * dt)
                }
            }
        }

        for (bullet in bullets.toList()) {
            var nextX = bullet.x + bullet.vx * dt
            var nextY = bullet.y + bullet.vy * dt
            bullet.life -= dt
            var hit = false
            for (actor in actors) {
                if (!actor.alive || actor === bullet.owner) continue
                if (hypot(actor.x - nextX, actor.y - nextY) <= 17f) {
                    actor.hp -= if (bullet.owner.player) 14f else 23f
                    if (actor.hp <= 0f) {
                        actor.alive = false
                        if (bullet.owner.player) {
                            bullet.owner.kills += 1
                        }
                    }
                    hit = true
                    break
                }
            }
            bullet.x = nextX
            bullet.y = nextY
            if (hit || bullet.life <= 0f || blocked(nextX, nextY, 2f)) {
                bullets.remove(bullet)
            }
        }

        val incomingLoot = mutableListOf<Loot>()
        for (item in loot) {
            if (playerActor != null && hypot(playerActor.x - item.x, playerActor.y - item.y) < 30f) {
                if (item.type == "health") {
                    playerActor.hp = min(playerActor.maxHp, playerActor.hp + 40f)
                } else {
                    playerActor.ammo += 35
                }
                incomingLoot.add(item)
            }
        }
        loot.removeAll(incomingLoot)

        val aliveCount = actors.count { it.alive }
        if (!playerActor.alive || aliveCount <= 1) {
            running = false
        }
    }

    fun setMoveInput(x: Float, y: Float) {
        moveX = x
        moveY = y
    }

    fun onDragStart(x: Float, y: Float) {
        dragActive = true
        dragStartX = x
        dragStartY = y
    }

    fun onDragMove(x: Float, y: Float, width: Float) {
        if (!dragActive || x > width * 0.6f) return
        val dx = x - dragStartX
        val dy = y - dragStartY
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val maxDistance = 55f
        val scale = if (mag > maxDistance) maxDistance / mag else 1f
        moveX = dx * scale
        moveY = dy * scale
    }

    fun onDragEnd() {
        dragActive = false
        moveX = 0f
        moveY = 0f
    }

    private fun nearestEnemy(actor: Actor): Actor? {
        var nearest: Actor? = null
        var minDistance = Float.POSITIVE_INFINITY
        for (candidate in actors) {
            if (candidate !== actor && candidate.alive && candidate.player != actor.player) {
                val d = hypot(candidate.x - actor.x, candidate.y - actor.y)
                if (d < minDistance) {
                    minDistance = d
                    nearest = candidate
                }
            }
        }
        return nearest
    }

    private fun shoot(owner: Actor, target: Actor) {
        if (owner.cooldown > 0f || (owner.player && owner.ammo <= 0)) return
        val angle = kotlin.math.atan2(target.y - owner.y, target.x - owner.x)
        bullets.add(
            Bullet(
                x = owner.x,
                y = owner.y,
                vx = cos(angle) * 660f,
                vy = sin(angle) * 660f,
                owner = owner,
                life = 0.9f
            )
        )
        owner.cooldown = if (owner.player) owner.rate else 0.7f + Math.random().toFloat() * 0.5f
        if (owner.player) owner.ammo -= 1
    }

    private fun moveActor(actor: Actor, dx: Float, dy: Float) {
        if (!blocked(actor.x + dx, actor.y, 15f)) actor.x += dx
        if (!blocked(actor.x, actor.y + dy, 15f)) actor.y += dy
    }

    private fun blocked(x: Float, y: Float, radius: Float): Boolean {
        if (x < radius || y < radius || x > world - radius || y > world - radius) return true
        for (building in buildings) {
            if (x + radius > building.x && x - radius < building.x + building.w &&
                y + radius > building.y && y - radius < building.y + building.h
            ) {
                return true
            }
        }
        return false
    }

    private fun spawnPoint(): Offset {
        for (i in 0 until 5000) {
            val p = Offset((70f..(world - 70f)).random(), (70f..(world - 70f)).random())
            if (!blocked(p.x, p.y, 28f)) return p
        }
        return Offset(1100f, 1100f)
    }

    private fun generateBuildings() {
        buildings.clear()
        for (row in 0..5) {
            for (col in 0..5) {
                if ((row == 2 || row == 3) && (col == 2 || col == 3)) continue
                val colors = listOf(Color(0xFFB77952), Color(0xFFA89069), Color(0xFFB66D65), Color(0xFF82967A))
                buildings.add(
                    Building(
                        x = 100f + col * 350f,
                        y = 110f + row * 350f,
                        w = (100f..175f).random(),
                        h = (95f..160f).random(),
                        color = colors[(row + col) % 4]
                    )
                )
            }
        }
    }

    fun draw(scope: DrawScope) {
        val playerActor = player ?: return
        val cameraX = playerActor.x - scope.size.width / 2f
        val cameraY = playerActor.y - scope.size.height / 2f

        with(scope) {
            drawRect(Color(0xFF20382F), Offset.Zero, size)
            translate(-cameraX, -cameraY) {
                drawRect(Color(0xFF668652), Offset.Zero, Size(world, world))

                drawLine(Color(0xFFA49B80), start = Offset(45f, 0f), end = Offset(45f, world), strokeWidth = 65f)
                for (i in 1..6) {
                    val x = 45f + i * 350f
                    drawLine(Color(0xFFA49B80), start = Offset(x, 0f), end = Offset(x, world), strokeWidth = 65f)
                }
                for (i in 1..6) {
                    val y = 55f + i * 350f
                    drawLine(Color(0xFFA49B80), start = Offset(0f, y), end = Offset(world, y), strokeWidth = 65f)
                }

                for (building in buildings) {
                    drawRect(building.color, Offset(building.x, building.y), Size(building.w, building.h))
                    drawRect(Color(0x33000000), Offset(building.x + 7f, building.y + 8f), Size(building.w, building.h))
                }

                for (item in loot) {
                    drawCircle(
                        color = if (item.type == "health") Color(0xFF22C55E) else Color(0xFFFACC15),
                        radius = 11f,
                        center = Offset(item.x, item.y)
                    )
                }

                for (actor in actors) {
                    if (!actor.alive) continue
                    drawCircle(Color(0x33000000), radius = 17f, center = Offset(actor.x + 3f, actor.y + 5f))
                    drawCircle(actor.color, radius = 16f, center = Offset(actor.x, actor.y))
                    drawCircle(Color(0xFFF5C7A4), radius = 7f, center = Offset(actor.x, actor.y - 3f))
                    drawRect(Color(0xFF17202A), Offset(actor.x - 22f, actor.y - 32f), Size(44f, 5f))
                    drawRect(Color(0xFF4ADE80), Offset(actor.x - 22f, actor.y - 32f), Size(44f * (actor.hp / actor.maxHp).coerceIn(0f, 1f), 5f))
                }

                for (bullet in bullets) {
                    drawCircle(Color(0xFFFFF9B0), radius = 3f, center = Offset(bullet.x, bullet.y))
                }

                drawArc(
                    color = Color(0xFF6D28D955),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    size = Size(zone * 2f, zone * 2f),
                    topLeft = Offset(1100f - zone, 1100f - zone),
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}

@Composable
fun GameCanvas(gameState: GameState) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameState.running) {
        if (!gameState.running) return@LaunchedEffect
        while (gameState.running) {
            delay(16)
            gameState.update(0.016f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1B2D))
            .pointerInput(gameState) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < size.width * 0.6f) {
                            gameState.onDragStart(offset.x, offset.y)
                        }
                    },
                    onDrag = { change, _ ->
                        if (change.position.x < size.width * 0.6f) {
                            gameState.onDragMove(change.position.x, change.position.y, size.width.toFloat())
                        }
                    },
                    onDragEnd = {
                        gameState.onDragEnd()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            gameState.draw(this)
        }
    }
}

fun Double.toFloat(): Float = this.toFloat()
