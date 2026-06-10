package com.example.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

data class CharacterState(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 3f, // Standard size in normalized units (3% of board width)
    var isDead: Boolean = false,
    var isVictorious: Boolean = false,
    var activeGravityX: Float = 0f,
    var activeGravityY: Float = 16f
)

object PhysicsEngine {

    /**
     * Updates the physics step for a single frame.
     * @param state The Mutable Character State.
     * @param level The Active Level configurations.
     * @param drawnLines Player drawn lines (represented in normalized 0..100 coords).
     * @param dt Elapsed delta time in seconds.
     */
    fun update(
        state: CharacterState,
        level: Level,
        drawnLines: List<List<Offset>>,
        dt: Float
    ) {
        if (state.isDead || state.isVictorious) return

        // 1. Apply Gravity
        state.vx += state.activeGravityX * dt
        state.vy += state.activeGravityY * dt

        // Speed limiting to prevent tunneling
        val maxSpeed = 75f
        val speed = sqrt(state.vx * state.vx + state.vy * state.vy)
        if (speed > maxSpeed) {
            state.vx = (state.vx / speed) * maxSpeed
            state.vy = (state.vy / speed) * maxSpeed
        }

        // 2. Apply Velocity
        state.x += state.vx * dt
        state.y += state.vy * dt

        // 3. Keep within notebook page limits
        val leftWall = state.radius
        val rightWall = 100f - state.radius
        val topWall = state.radius
        val bottomWall = 100f - state.radius

        if (state.x < leftWall) {
            state.x = leftWall
            state.vx = -state.vx * 0.3f // Slight bounce
        } else if (state.x > rightWall) {
            state.x = rightWall
            state.vx = -state.vx * 0.3f
        }

        if (state.y < topWall) {
            state.y = topWall
            state.vy = -state.vy * 0.3f
        } else if (state.y > bottomWall) {
            state.y = bottomWall
            state.vy = -state.vy * 0.3f
            // If normal gravity pulls down and we sit on the spikes-less floor, don't die but stop falling
            // Wait, we can let them roll on bottom floor.
        }

        // 4. Resolve static object collisions
        for (obj in level.objects) {
            when (obj.type) {
                ObjectType.BOX_PLATFORM -> {
                    resolveBoxCollision(state, obj)
                }
                ObjectType.BOUNCER -> {
                    resolveBouncerCollision(state, obj)
                }
                ObjectType.SPIKE_HAZARD -> {
                    if (checkSpikeCollision(state, obj)) {
                        state.isDead = true
                    }
                }
                ObjectType.SPINNER_HAZARD -> {
                    if (checkSpinnerCollision(state, obj)) {
                        state.isDead = true
                    }
                }
                ObjectType.GRAVITY_UP -> {
                    if (checkTriggerCollision(state, obj)) {
                        state.activeGravityX = 0f
                        state.activeGravityY = -15f
                    }
                }
                ObjectType.GRAVITY_DOWN -> {
                    if (checkTriggerCollision(state, obj)) {
                        state.activeGravityX = 0f
                        state.activeGravityY = 15f
                    }
                }
                ObjectType.GRAVITY_LEFT -> {
                    if (checkTriggerCollision(state, obj)) {
                        state.activeGravityX = -15f
                        state.activeGravityY = 0f
                    }
                }
                ObjectType.GRAVITY_RIGHT -> {
                    if (checkTriggerCollision(state, obj)) {
                        state.activeGravityX = 15f
                        state.activeGravityY = 0f
                    }
                }
            }
        }

        // 5. Resolve player-drawn line collisions (sliding ramps)
        for (line in drawnLines) {
            for (i in 0 until line.size - 1) {
                val p1 = line[i]
                val p2 = line[i + 1]
                resolveLineSegmentCollision(state, p1, p2)
            }
        }

        // 6. Check if character reached the portal door
        val dx = state.x - level.doorX
        val dy = state.y - level.doorY
        val distanceToDoor = sqrt(dx * dx + dy * dy)
        if (distanceToDoor < (state.radius + 4.5f)) { // Portal radius is approx 4.5f
            state.isVictorious = true
        }
    }

    private fun resolveBoxCollision(state: CharacterState, box: GameObject) {
        // Find closest point on the AABB to the circle center
        val closestX = state.x.coerceIn(box.x, box.x + box.width)
        val closestY = state.y.coerceIn(box.y, box.y + box.height)

        val dx = state.x - closestX
        val dy = state.y - closestY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < state.radius && dist > 0.0001f) {
            // Overlap detected: push character out
            val overlap = state.radius - dist
            val nx = dx / dist
            val ny = dy / dist

            state.x += nx * overlap
            state.y += ny * overlap

            // Bounce/Deflect velocity
            val dot = state.vx * nx + state.vy * ny
            if (dot < 0) {
                // Moving into the box, reflect with restitution
                val restitution = 0.25f
                state.vx = state.vx - (1 + restitution) * dot * nx
                state.vy = state.vy - (1 + restitution) * dot * ny
            }
        } else if (dist == 0f) {
            // Directly inside the box, push out towards nearest side
            val leftDist = state.x - box.x
            val rightDist = (box.x + box.width) - state.x
            val topDist = state.y - box.y
            val bottomDist = (box.y + box.height) - state.y

            val minDist = minOf(leftDist, rightDist, topDist, bottomDist)
            when (minDist) {
                leftDist -> {
                    state.x = box.x - state.radius
                    state.vx = -state.vx * 0.25f
                }
                rightDist -> {
                    state.x = box.x + box.width + state.radius
                    state.vx = -state.vx * 0.25f
                }
                topDist -> {
                    state.y = box.y - state.radius
                    state.vy = -state.vy * 0.25f
                }
                bottomDist -> {
                    state.y = box.y + box.height + state.radius
                    state.vy = -state.vy * 0.25f
                }
            }
        }
    }

    private fun resolveBouncerCollision(state: CharacterState, bouncer: GameObject) {
        val closestX = state.x.coerceIn(bouncer.x, bouncer.x + bouncer.width)
        val closestY = state.y.coerceIn(bouncer.y, bouncer.y + bouncer.height)

        val dx = state.x - closestX
        val dy = state.y - closestY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < state.radius) {
            // Elastic upward launch trigger!
            val pushAngle = -1.5708f // Straight upwards (-Y)
            val launchSpd = 34f      // Big upwards launch!

            state.vy = -launchSpd
            if (state.vx in -3f..3f) {
                state.vx = if (state.x < 50f) 8f else -8f // Bounce outwards gently
            }

            // Move out of penetrations
            state.y = bouncer.y - state.radius - 1f
        }
    }

    private fun checkSpikeCollision(state: CharacterState, spike: GameObject): Boolean {
        // Spikes are represented by box AABB
        val closestX = state.x.coerceIn(spike.x, spike.x + spike.width)
        val closestY = state.y.coerceIn(spike.y, spike.y + spike.height)

        val dx = state.x - closestX
        val dy = state.y - closestY
        val dist = sqrt(dx * dx + dy * dy)

        return dist < (state.radius * 0.85f) // Tightened bounding shape for fair gameplay
    }

    private fun checkSpinnerCollision(state: CharacterState, spinner: GameObject): Boolean {
        // Spinner is circular with custom radius
        val radius = spinner.width / 2f
        val spinnerCenterX = spinner.x + radius
        val spinnerCenterY = spinner.y + radius

        val dx = state.x - spinnerCenterX
        val dy = state.y - spinnerCenterY
        val dist = sqrt(dx * dx + dy * dy)

        return dist < (state.radius + radius * 0.82f)
    }

    private fun checkTriggerCollision(state: CharacterState, trigger: GameObject): Boolean {
        // Centralized trigger coin detection
        val radius = trigger.width / 2f
        val tcX = trigger.x + radius
        val tcY = trigger.y + radius

        val dx = state.x - tcX
        val dy = state.y - tcY
        val dist = sqrt(dx * dx + dy * dy)

        return dist < (state.radius + radius)
    }

    private fun resolveLineSegmentCollision(state: CharacterState, p1: Offset, p2: Offset) {
        val segmentVectorX = p2.x - p1.x
        val segmentVectorY = p2.y - p1.y

        val segmentLengthSq = segmentVectorX * segmentVectorX + segmentVectorY * segmentVectorY
        if (segmentLengthSq < 0.0001f) return

        // Vector from p1 to ball
        val ballVectorX = state.x - p1.x
        val ballVectorY = state.y - p1.y

        // Projection normalized factor t
        var t = (ballVectorX * segmentVectorX + ballVectorY * segmentVectorY) / segmentLengthSq
        t = t.coerceIn(0f, 1f)

        // Closest point on client line
        val cx = p1.x + t * segmentVectorX
        val cy = p1.y + t * segmentVectorY

        val dx = state.x - cx
        val dy = state.y - cy
        val dist = sqrt(dx * dx + dy * dy)

        // Inside ball radius?
        if (dist < state.radius && dist > 0.0001f) {
            val normalX = dx / dist
            val normalY = dy / dist

            // Resolve overlap (push character out of line)
            val overlap = state.radius - dist
            state.x += normalX * overlap
            state.y += normalY * overlap

            // Velocity relative to normal
            val relativeVelNormal = state.vx * normalX + state.vy * normalY

            if (relativeVelNormal < 0f) {
                // Moving into the line, absorb kinetic energy and slide
                val restitution = 0.15f // Low bounce for natural sliding paper aesthetic
                state.vx = state.vx - (1 + restitution) * relativeVelNormal * normalX
                state.vy = state.vy - (1 + restitution) * relativeVelNormal * normalY

                // Apply slide friction to slow down slightly
                val friction = 0.985f
                state.vx *= friction
                state.vy *= friction
            }
        }
    }
}
