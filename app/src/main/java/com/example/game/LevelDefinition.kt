package com.example.game

enum class ObjectType {
    BOX_PLATFORM,   // Solid obstacle that the ball collides with
    BOUNCER,        // Trampoline/bouncer that spring-launches the ball
    SPIKE_HAZARD,   // Sharp red spikes (kills character on touch)
    SPINNER_HAZARD, // Spinning circular hazard (kills character on touch)
    GRAVITY_UP,     // Changes gravity vector upwards (0, -1) on touch
    GRAVITY_DOWN,   // Changes gravity vector downwards (0, 1) on touch
    GRAVITY_LEFT,   // Changes gravity vector left (-1, 0) on touch
    GRAVITY_RIGHT   // Changes gravity vector right (1, 0) on touch
}

data class GameObject(
    val type: ObjectType,
    val x: Float,          // Normalized coordinates (0 to 100)
    val y: Float,          // Normalized coordinates (0 to 100)
    val width: Float = 0f,  // In normalized units
    val height: Float = 0f, // In normalized units
    val label: String = ""  // Helpful decorative label
)

data class Level(
    val id: Int,
    val name: String,
    val startX: Float,
    val startY: Float,
    val doorX: Float,
    val doorY: Float,
    val inkLimit: Float,     // Total drawing ink (units)
    val baseGravityX: Float, // Initial gravity vector X
    val baseGravityY: Float, // Initial gravity vector Y
    val hint: String,
    val objects: List<GameObject>
)

object LevelManager {
    val levels: List<Level> = (1..50).map { id ->
        generateLevel(id)
    }

    private fun generateLevel(id: Int): Level {
        var name = ""
        var startX = 0f
        var startY = 0f
        var doorX = 0f
        var doorY = 0f
        var inkLimit = 0f
        var gravityX = 0f
        var gravityY = 16f // Downward gravity standard
        var hint = ""
        val objects = mutableListOf<GameObject>()

        // Progressive logic to generate exactly 50 distinct levels
        when {
            // ==========================================
            // PHASE 1: Levels 1-10 (Básicos y Rampas)
            // ==========================================
            id == 1 -> {
                name = "Puente en el Cuaderno"
                startX = 15f
                startY = 20f
                doorX = 85f
                doorY = 55f
                inkLimit = 350f
                hint = "Dibuja una rampa continua que conecte el inicio y la puerta."
                // Safety floor at start
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 25f, 20f, 6f, "Inicio"))
                // Safety floor near door
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 65f, 20f, 6f, "Meta"))
            }
            id == 2 -> {
                name = "Esquiva la Escuadra"
                startX = 12f
                startY = 15f
                doorX = 88f
                doorY = 75f
                inkLimit = 400f
                hint = "Dibuja un tobogán que pase por debajo de la gran barra negra."
                // Safe platforms
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 22f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 85f, 16f, 5f))
                // Central divider blocking straight lines
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 0f, 10f, 50f, "Obstáculo"))
            }
            id == 3 -> {
                name = "El Salto del Compás"
                startX = 15f
                startY = 20f
                doorX = 80f
                doorY = 20f
                inkLimit = 500f
                hint = "Utiliza el resorte verde para impulsarte hasta la meta."
                // Start platform
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 28f, 20f, 5f))
                // Door platform
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 28f, 20f, 5f))
                // Bouncer in the bottom pit
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 80f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 47f, 75f, 6f, 5f))
            }
            id == 4 -> {
                name = "Tobogán Curvo"
                startX = 20f
                startY = 15f
                doorX = 80f
                doorY = 85f
                inkLimit = 300f
                hint = "Dibuja un arco circular suave para que ruede rápido."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 25f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 70f, 92f, 25f, 5f))
                // Solid obstacle right below the start
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 35f, 40f, 30f, 10f))
            }
            id == 5 -> {
                name = "Laberinto Simple"
                startX = 15f
                startY = 15f
                doorX = 15f
                doorY = 80f
                inkLimit = 450f
                hint = "Guía al personaje en forma de zigzag por las estanterías."
                // Zigzag platforms
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 25f, 30f, 4f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 60f, 45f, 35f, 4f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 65f, 40f, 4f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 90f, 25f, 4f))
            }
            id <= 10 -> {
                val step = id - 5
                name = "Desafío Escrito #$step"
                startX = 10f + step * 2f
                startY = 20f
                doorX = 90f - step * 2f
                doorY = 80f
                inkLimit = 400f - (step * 15f)
                hint = "Controla tu trazo: la tinta es limitada en este nivel."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, startX - 5f, startY + 8f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, doorX - 5f, doorY + 12f, 15f, 5f))

                when (id) {
                    6 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 60f, 20f, 6f, "Estante"))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 95f, 25f, 5f))
                    }
                    7 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 30f, 45f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 60f, 65f, 15f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 95f, 20f, 5f))
                    }
                    8 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 30f, 10f, 35f, "Columna"))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 20f, 95f, 20f, 5f))
                    }
                    9 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 50f, 70f, 25f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 60f, 65f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 15f, 95f, 30f, 5f))
                    }
                    10 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 55f, 5f, 25f, "Pared"))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 25f, 95f, 40f, 5f))
                    }
                }
            }

            // ==========================================
            // PHASE 2: Levels 11-20 (Primeros Peligros / Pinchos)
            // ==========================================
            id == 11 -> {
                name = "¡Cuidado con los Pinchos!"
                startX = 15f
                startY = 15f
                doorX = 85f
                doorY = 80f
                inkLimit = 420f
                hint = "Dibuja un escudo de tinta sobre los pinchos inferiores."
                // Safe platforms
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 25f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 90f, 20f, 5f))
                // Dangerous red spikes at bottom
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 95f, 40f, 5f))
            }
            id == 12 -> {
                name = "El Barranco Sangriento"
                startX = 15f
                startY = 20f
                doorX = 85f
                doorY = 20f
                inkLimit = 550f
                hint = "El abismo está lleno de pinchos. ¡Dibuja un puente impecable!"
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 30f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 30f, 15f, 5f))
                // Giant bed of spikes
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 20f, 90f, 60f, 10f))
            }
            id == 13 -> {
                name = "La Lluvia de Grafitos"
                startX = 50f
                startY = 10f
                doorX = 85f
                doorY = 80f
                inkLimit = 450f
                hint = "Protege el personaje contra los pinchos superiores dibujando un tejado."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 20f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 90f, 20f, 5f))
                // Spike barriers on top-right blocking straight falling
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 65f, 35f, 30f, 6f))
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 10f, 65f, 40f, 6f))
            }
            id == 14 -> {
                name = "El Pozo de Tinta"
                startX = 12f
                startY = 12f
                doorX = 85f
                doorY = 85f
                inkLimit = 350f
                hint = "La puerta está tras un muro bajo; dibuja una rampa para saltarla."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 20f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 95f, 18f, 5f))
                // Wall right next to the door
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 65f, 60f, 6f, 40f))
                // Spikes at the bottom-left of the pit
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 25f, 95f, 35f, 5f))
            }
            id == 15 -> {
                name = "La Rueda Dentada"
                startX = 20f
                startY = 20f
                doorX = 80f
                doorY = 20f
                inkLimit = 500f
                hint = "Rodea el peligroso engranaje rojo en el centro por el lado superior."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 30f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 70f, 30f, 20f, 5f))
                // Large circular spinner in middle
                objects.add(GameObject(ObjectType.SPINNER_HAZARD, 50f, 50f, 15f, 15f, "Cuchilla"))
            }
            id <= 20 -> {
                val step = id - 15
                name = "Bosque Extremo #$step"
                startX = 15f
                startY = 15f
                doorX = 85f
                doorY = 85f
                inkLimit = 450f
                hint = "Esquiva los pinchos colocados estratégicamente."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 22f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 92f, 20f, 5f))

                when (id) {
                    16 -> {
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 25f, 45f, 15f, 6f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 65f, 75f, 15f, 6f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 50f, 25f, 8f, 8f))
                    }
                    17 -> {
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 35f, 80f, 15f, 6f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 50f, 30f, 5f, 40f))
                    }
                    18 -> {
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 35f, 40f, 10f, 10f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 65f, 60f, 10f, 10f))
                    }
                    19 -> {
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 20f, 40f, 6f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 95f, 45f, 5f))
                    }
                    20 -> {
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 35f, 45f, 30f, 8f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 50f, 25f, 10f, 10f))
                    }
                }
            }

            // ==========================================
            // PHASE 3: Levels 21-30 (Placas Elásticas / Rebotes)
            // ==========================================
            id == 21 -> {
                name = "Fábrica de Rebotes"
                startX = 15f
                startY = 20f
                doorX = 85f
                doorY = 20f
                inkLimit = 380f
                hint = "La amortiguación del resorte te ayudará a cruzar el abismo."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 30f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 30f, 20f, 5f))
                // Central trampoline platform
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 75f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 47f, 70f, 6f, 5f))
                // Red hazard underneath
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 0f, 95f, 100f, 5f))
            }
            id == 22 -> {
                name = "Pase de Tres Bandas"
                startX = 15f
                startY = 10f
                doorX = 85f
                doorY = 85f
                inkLimit = 420f
                hint = "Rebota en el lado izquierdo y deslízate en el lado derecho."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 18f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 92f, 20f, 5f))
                // Spring on left-wall
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 50f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 10f, 45f, 6f, 5f))
                // High central spike
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 40f, 15f, 40f))
            }
            id == 23 -> {
                name = "Trampolín de Papel"
                startX = 80f
                startY = 15f
                doorX = 20f
                doorY = 80f
                inkLimit = 500f
                hint = "Dibuja un embudo que canalice la caída hacia el resorte."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 70f, 22f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 88f, 20f, 5f))
                // Spring in the corner
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 75f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 80f, 70f, 6f, 5f))
                // Spikes on floor
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 95f, 40f, 5f))
            }
            id <= 30 -> {
                val step = id - 23
                name = "Cámara Elástica #$step"
                inkLimit = 400f
                hint = "Apóyate en el resorte para ascender."

                when (id) {
                    24 -> {
                        startX = 15f
                        startY = 30f
                        doorX = 85f
                        doorY = 85f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 38f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 92f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 55f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 48f, 50f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 40f, 95f, 20f, 5f))
                    }
                    25 -> {
                        startX = 85f
                        startY = 30f
                        doorX = 15f
                        doorY = 85f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 38f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 92f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 50f, 65f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 53f, 60f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 15f, 95f, 25f, 5f))
                    }
                    26 -> {
                        startX = 15f
                        startY = 20f
                        doorX = 85f
                        doorY = 20f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 28f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 28f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 25f, 80f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 28f, 75f, 6f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 65f, 50f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 68f, 45f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 40f, 95f, 20f, 5f))
                    }
                    27 -> {
                        startX = 5f
                        startY = 20f
                        doorX = 90f
                        doorY = 20f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 0f, 28f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 85f, 28f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 85f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 48f, 80f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 20f, 95f, 25f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 58f, 95f, 25f, 5f))
                    }
                    28 -> {
                        startX = 12f
                        startY = 20f
                        doorX = 85f
                        doorY = 25f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 28f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 33f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 30f, 8f, 50f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 20f, 75f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 23f, 70f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 95f, 40f, 5f))
                    }
                    29 -> {
                        startX = 15f
                        startY = 80f
                        doorX = 85f
                        doorY = 25f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 88f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 33f, 20f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 85f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 45f, 80f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 65f, 95f, 30f, 5f))
                    }
                    30 -> {
                        startX = 10f
                        startY = 15f
                        doorX = 90f
                        doorY = 15f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 23f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 23f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 20f, 80f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 23f, 75f, 6f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 68f, 80f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 71f, 75f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 38f, 95f, 24f, 5f))
                    }
                }
            }

            // ==========================================
            // PHASE 4: Levels 31-40 (Gravedad Alucinante, Inversa, Lateral)
            // ==========================================
            id == 31 -> {
                name = "Gravedad Flotante"
                startX = 20f
                startY = 85f // START AT BOTTOM
                doorX = 80f
                doorY = 15f // DOOR AT TOP
                inkLimit = 400f
                gravityY = -14f // INVERTED BASE GRAVITY!
                hint = "¡La gravedad tira de ti hacia el cielo! Dibuja techos inclinados."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 92f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 70f, 22f, 20f, 5f))
                // Spike on the original ceiling
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 5f, 40f, 6f))
            }
            id == 32 -> {
                name = "Viento de Tinta"
                startX = 15f
                startY = 30f
                doorX = 85f
                doorY = 30f
                inkLimit = 450f
                gravityX = 14f // SIDE GRAVITY (pulls to the right)
                gravityY = 0f
                hint = "La fuerza te arrastra hacia la derecha. Dibuja paredes verticales."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 40f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 40f, 15f, 5f))
                // Red Spikes on right wall
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 95f, 15f, 5f, 70f))
            }
            id == 33 -> {
                name = "Orbe Multidireccional"
                startX = 10f
                startY = 20f
                doorX = 85f
                doorY = 80f
                inkLimit = 500f
                hint = "Toca los engranajes azules de gravedad para cambiar de dirección en el aire."
                // Platforms
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 28f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 90f, 20f, 5f))
                // Gravity modifiers in empty space
                objects.add(GameObject(ObjectType.GRAVITY_UP, 30f, 40f, 8f, 8f))     // Switch gravity Up
                objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 60f, 20f, 8f, 8f))  // Switch gravity Right
                objects.add(GameObject(ObjectType.GRAVITY_DOWN, 70f, 50f, 8f, 8f))   // Switch gravity Down
                // Danger at normal floor
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 20f, 95f, 50f, 5f))
            }
            id == 34 -> {
                name = "El Salto Invertido"
                startX = 80f
                startY = 80f
                doorX = 20f
                doorY = 20f
                inkLimit = 400f
                hint = "Activa la gravedad inversa y déjate guiar al portal."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 70f, 90f, 20f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 28f, 20f, 5f))
                // Switch
                objects.add(GameObject(ObjectType.GRAVITY_UP, 45f, 80f, 8f, 8f))
                // Spikes blocking standard straight path
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 50f, 40f, 6f))
            }
            id == 35 -> {
                name = "Ciclón en la Libreta"
                startX = 50f
                startY = 50f
                doorX = 90f
                doorY = 10f
                inkLimit = 450f
                hint = "Esquiva las sierras utilizando los botones de gravedad."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 60f, 12f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 18f, 15f, 5f))

                objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 25f, 40f, 6f, 6f))
                objects.add(GameObject(ObjectType.GRAVITY_UP, 70f, 75f, 6f, 6f))

                objects.add(GameObject(ObjectType.SPINNER_HAZARD, 20f, 20f, 10f, 10f))
                objects.add(GameObject(ObjectType.SPINNER_HAZARD, 50f, 80f, 10f, 10f))
            }
            id <= 40 -> {
                val step = id - 35
                name = "Fisuras Gravitacionales #$step"
                startX = 10f
                startY = 30f
                doorX = 90f
                doorY = 70f
                inkLimit = 420f
                hint = "Prepárate para cambios dinámicos de caída."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 38f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 78f, 15f, 5f))

                when (id) {
                    36 -> {
                        objects.add(GameObject(ObjectType.GRAVITY_UP, 35f, 60f, 7f, 7f))
                        objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 65f, 20f, 7f, 7f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 5f, 30f, 6f))
                    }
                    37 -> {
                        objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 25f, 40f, 7f, 7f))
                        objects.add(GameObject(ObjectType.GRAVITY_LEFT, 75f, 45f, 7f, 7f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 85f, 15f, 6f))
                    }
                    38 -> {
                        objects.add(GameObject(ObjectType.GRAVITY_DOWN, 30f, 25f, 7f, 7f))
                        objects.add(GameObject(ObjectType.GRAVITY_UP, 50f, 75f, 7f, 7f))
                        objects.add(GameObject(ObjectType.GRAVITY_LEFT, 70f, 40f, 7f, 7f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 95f, 30f, 5f, 40f))
                    }
                    39 -> {
                        gravityY = -14f
                        objects.add(GameObject(ObjectType.GRAVITY_DOWN, 50f, 35f, 7f, 7f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 40f, 5f, 30f, 6f))
                    }
                    40 -> {
                        objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 30f, 45f, 7f, 7f))
                        objects.add(GameObject(ObjectType.GRAVITY_LEFT, 60f, 45f, 7f, 7f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 45f, 15f, 10f, 10f))
                    }
                }
            }

            // ==========================================
            // PHASE 5: Levels 41-50 (Laberintos Avanzados De Tinta)
            // ==========================================
            id == 41 -> {
                name = "La Jaula del Compás"
                startX = 15f
                startY = 15f
                doorX = 85f
                doorY = 85f
                inkLimit = 320f // Low ink!!
                hint = "Paso angosto con un laberinto de cuchillas rojas."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 22f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 92f, 15f, 5f))

                // Blades blocking path
                objects.add(GameObject(ObjectType.SPINNER_HAZARD, 35f, 30f, 8f, 8f))
                objects.add(GameObject(ObjectType.SPINNER_HAZARD, 65f, 60f, 8f, 8f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 45f, 10f, 10f))
            }
            id == 42 -> {
                name = "Ascenso Mecánico"
                startX = 15f
                startY = 80f
                doorX = 85f
                doorY = 15f
                inkLimit = 500f
                hint = "La puerta está arriba. Utiliza la amortiguación múltiple."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 90f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 25f, 15f, 5f))

                // Two springers
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 70f, 10f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 47f, 65f, 6f, 5f))

                objects.add(GameObject(ObjectType.BOX_PLATFORM, 15f, 40f, 10f, 5f))
                objects.add(GameObject(ObjectType.BOUNCER, 17f, 35f, 6f, 5f))

                // Dead-end drops have spikes
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 60f, 95f, 40f, 5f))
            }
            id == 43 -> {
                name = "Laberinto del Estudiante"
                startX = 50f
                startY = 10f
                doorX = 50f
                doorY = 90f
                inkLimit = 420f
                hint = "Dibuja tubos que lleven el personaje a través del laberinto espinado."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 42f, 18f, 16f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 42f, 95f, 16f, 5f))

                // Maze horizontal slabs
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 35f, 35f, 4f))
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 35f, 45f, 5f))

                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 10f, 60f, 45f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 55f, 60f, 35f, 4f))
            }
            id == 44 -> {
                name = "Boceto Infinito"
                startX = 10f
                startY = 10f
                doorX = 90f
                doorY = 90f
                inkLimit = 300f // Hard constraint!
                hint = "La gravedad es muy baja, lánzate describiendo una sola parábola."
                gravityY = 6f // Low gravity downward
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 20f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 95f, 15f, 5f))

                // Floaters
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 40f, 45f, 20f, 5f))
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 45f, 40f, 10f, 5f))
            }
            id == 45 -> {
                name = "Vuelo Cruzado"
                startX = 15f
                startY = 50f
                doorX = 85f
                doorY = 50f
                inkLimit = 450f
                gravityY = 0f
                gravityX = 0f // ZERO GRAVITY BY DEFAULT! Floating game.
                hint = "Gravedad Cero. Dibuja curvas para golpear la pelota y darle inercia."
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 60f, 15f, 5f))
                objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 60f, 15f, 5f))

                // Moving/floating spikes
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 30f, 15f, 8f, 30f))
                objects.add(GameObject(ObjectType.SPIKE_HAZARD, 60f, 55f, 8f, 30f))
                // Middle gravity kick
                objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 45f, 45f, 10f, 10f))
            }
            else -> {
                val step = id - 45
                name = "Prueba de Graduación #$step"
                inkLimit = 280f + (step * 10f)
                hint = "La prueba definitiva: ¡tinta escasa, gravedad mixta, trampas móviles!"

                when (id) {
                    46 -> {
                        startX = 15f
                        startY = 15f
                        doorX = 85f
                        doorY = 85f
                        gravityY = 10f // Low gravity downward
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 22f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 92f, 20f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 35f, 45f, 15f, 6f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 65f, 65f, 10f, 10f))
                        objects.add(GameObject(ObjectType.GRAVITY_LEFT, 50f, 30f, 6f, 6f))
                    }
                    47 -> {
                        startX = 50f
                        startY = 15f
                        doorX = 50f
                        doorY = 85f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 42f, 22f, 16f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 42f, 92f, 16f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 25f, 40f, 25f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 50f, 65f, 25f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 10f, 55f, 10f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 12f, 50f, 6f, 5f))
                    }
                    48 -> {
                        startX = 10f
                        startY = 50f
                        doorX = 90f
                        doorY = 50f
                        gravityX = 0f
                        gravityY = 0f // Zero gravity
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 58f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 58f, 20f, 5f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 30f, 35f, 12f, 12f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 60f, 65f, 12f, 12f))
                        objects.add(GameObject(ObjectType.GRAVITY_DOWN, 45f, 45f, 7f, 7f))
                    }
                    49 -> {
                        startX = 15f
                        startY = 15f
                        doorX = 85f
                        doorY = 15f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 22f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 75f, 22f, 20f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 25f, 90f, 50f, 6f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 80f, 12f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 48f, 75f, 6f, 5f))
                        objects.add(GameObject(ObjectType.SPINNER_HAZARD, 50f, 50f, 10f, 10f))
                    }
                    50 -> {
                        startX = 10f
                        startY = 15f
                        doorX = 90f
                        doorY = 85f
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 5f, 22f, 15f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 80f, 92f, 18f, 5f))
                        objects.add(GameObject(ObjectType.GRAVITY_UP, 30f, 70f, 8f, 8f))
                        objects.add(GameObject(ObjectType.GRAVITY_RIGHT, 60f, 30f, 8f, 8f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 20f, 95f, 30f, 5f))
                        objects.add(GameObject(ObjectType.SPIKE_HAZARD, 40f, 5f, 35f, 5f))
                        objects.add(GameObject(ObjectType.BOX_PLATFORM, 45f, 50f, 10f, 5f))
                        objects.add(GameObject(ObjectType.BOUNCER, 47f, 45f, 6f, 5f))
                    }
                }
            }
        }

        return Level(
            id = id,
            name = name,
            startX = startX,
            startY = startY,
            doorX = doorX,
            doorY = doorY,
            inkLimit = inkLimit,
            baseGravityX = gravityX,
            baseGravityY = gravityY,
            hint = hint,
            objects = objects
        )
    }
}

object LevelTranslator {
    fun getTranslatedName(id: Int, defaultName: String, lang: String): String {
        return when (lang) {
            "en" -> when {
                id == 1 -> "Notebook Bridge"
                id == 2 -> "Dodge the Square"
                id == 3 -> "The Compass Leap"
                id == 4 -> "Curved Slide"
                id == 5 -> "Simple Maze"
                id <= 10 -> "Written Challenge #${id - 5}"
                id == 11 -> "Watch Out for Spikes!"
                id == 12 -> "The Bloody Ravine"
                id == 13 -> "The Graphite Rain"
                id == 14 -> "The Ink Pit"
                id == 15 -> "The Cogwheel"
                id <= 20 -> "Extreme Forest #${id - 15}"
                id == 21 -> "Bounce Factory"
                id == 22 -> "Three-Band Pass"
                id == 23 -> "Paper Trampoline"
                id <= 25 -> "Elastic Chamber #${id - 23}"
                id == 26 -> "Floating Gravity"
                id == 27 -> "Ink Wind"
                id == 28 -> "Multidirectional Orb"
                id == 29 -> "The Inverted Leap"
                id == 30 -> "Notebook Cyclone"
                id <= 35 -> "Gravitational Fissures #${id - 30}"
                id == 36 -> "The Compass Cage"
                id == 37 -> "Mechanical Ascent"
                id <= 40 -> "Student\'s Maze"
                id == 41 -> "Infinite Sketch"
                id == 42 -> "Crossed Flight"
                id <= 50 -> "Graduation Exam #${id - 42}"
                else -> defaultName
            }
            "pt" -> when {
                id == 1 -> "Ponte no Caderno"
                id == 2 -> "Desvie do Esquadro"
                id == 3 -> "O Salto do Compasso"
                id == 4 -> "Escorregador Curvo"
                id == 5 -> "Labirinto Simples"
                id <= 10 -> "Desafio Escrito #${id - 5}"
                id == 11 -> "Cuidado com os Espinhos!"
                id == 12 -> "A Ravina Sangrenta"
                id == 13 -> "A Chuva de Grafite"
                id == 14 -> "O Poço de Tinta"
                id == 15 -> "A Roda Dentada"
                id <= 20 -> "Floresta Extrema #${id - 15}"
                id == 21 -> "Fábrica de Rebotes"
                id == 22 -> "Passe de Três Tabelas"
                id == 23 -> "Trampolim de Papel"
                id <= 25 -> "Câmara Elástica #${id - 23}"
                id == 26 -> "Gravidade Flutuante"
                id == 27 -> "Vento de Tinta"
                id == 28 -> "Orbe Multidirecional"
                id == 29 -> "O Salto Invertido"
                id == 30 -> "Ciclone no Caderno"
                id <= 35 -> "Fissuras Gravitacionais #${id - 30}"
                id == 36 -> "A Gaiola do Compasso"
                id == 37 -> "Subida Mecânica"
                id <= 40 -> "Labirinto do Estudante"
                id == 41 -> "Esboço Infinito"
                id == 42 -> "Voo Cruzado"
                id <= 50 -> "Prova de Formatura #${id - 42}"
                else -> defaultName
            }
            else -> defaultName
        }
    }

    fun getTranslatedHint(id: Int, defaultHint: String, lang: String): String {
        return when (lang) {
            "en" -> when {
                id == 1 -> "Draw a continuous ramp that connects the start and the door."
                id == 2 -> "Draw a slide that goes below the big black bar."
                id == 3 -> "Use the green spring to launch yourself to the goal."
                id == 4 -> "Draw a smooth circular arc so it rolls fast."
                id == 5 -> "Guide the character in a zigzag pattern through the shelves."
                id <= 10 -> "Control your stroke: ink is limited in this level."
                id == 11 -> "Draw an ink shield over the bottom spikes."
                id == 12 -> "The abyss is full of spikes. Draw a flawless bridge!"
                id == 13 -> "Protect the character from top spikes by drawing a roof."
                id == 14 -> "The door is behind a low wall; draw a ramp to jump over it."
                id == 15 -> "Go up and around the dangerous red gear in the center."
                id <= 20 -> "Dodge the strategically placed spikes."
                id == 21 -> "The spring\'s bounce will help you cross the abyss."
                id == 22 -> "Bounce on the left side and slide on the right side."
                id == 23 -> "Draw a funnel that channels the fall toward the spring."
                id <= 25 -> "Use the spring to ascend."
                id == 26 -> "Gravity pulls you to the sky! Draw sloped ceilings."
                id == 27 -> "The force drags you to the right. Draw vertical walls."
                id == 28 -> "Touch the blue gravity gears to change direction in mid-air."
                id == 29 -> "Activate reverse gravity and let it guide you to the portal."
                id == 30 -> "Dodge the saws using the gravity buttons."
                id <= 35 -> "Prepare for dynamic fall changes."
                id == 36 -> "Narrow path with a maze of red blades."
                id == 37 -> "The door is at the top. Use multiple bounces."
                id <= 40 -> "Draw tubes that carry the character through the spiked maze."
                id == 41 -> "Gravity is very low, launch yourself by describing a single parabola."
                id == 42 -> "Zero Gravity. Draw curves to strike the ball and give it momentum."
                id <= 50 -> "The ultimate trial: scarce ink, mixed gravity, moving hazards!"
                else -> defaultHint
            }
            "pt" -> when {
                id == 1 -> "Desenhe uma rampa contínua que ligue o início e a porta."
                id == 2 -> "Desenhe um escorregador que passe embaixo da grande barra preta."
                id == 3 -> "Use a mola verde para se impulsionar até a meta."
                id == 4 -> "Desenhe um arco circular suave para rolar rápido."
                id == 5 -> "Guie o personagem em forma de zigue-zague pelas prateleiras."
                id <= 10 -> "Controle seu traço: a tinta é limitada neste nível."
                id == 11 -> "Desenhe um escudo de tinta sobre os espinhos inferiores."
                id == 12 -> "O abismo está cheio de espinhos. Desenhe uma ponte impecável!"
                id == 13 -> "Proteja o personagem dos espinhos superiores desenhando um telhado."
                id == 14 -> "A porta está atrás de um muro baixo; desenhe uma rampa para saltá-lo."
                id == 15 -> "Contorne a perigosa engrenagem vermelha no centro pelo lado superior."
                id <= 20 -> "Desvie dos espinhos colocados estrategicamente."
                id == 21 -> "O amortecimento da mola ajudará você a cruzar o abismo."
                id == 22 -> "Rebote no lado esquerdo e deslize no lado direito."
                id == 23 -> "Desenhe um funil que canalize a queda em direção à mola."
                id <= 25 -> "Apoie-se na mola para subir."
                id == 26 -> "A gravidade te puxa para o céu! Desenhe tetos inclinados."
                id == 27 -> "A força te arrasta para a direita. Desenhe paredes verticais."
                id == 28 -> "Toque nas engrenagens azuis de gravidade para mudar de direção no ar."
                id == 29 -> "Ative a gravidade inversa e deixe-se guiar até o portal."
                id == 30 -> "Desvie das serras usando os botões de gravidade."
                id <= 35 -> "Prepare-se para mudanças dinâmicas de queda."
                id == 36 -> "Caminho estreito com um labirinto de lâminas vermelhas."
                id == 37 -> "A porta está em cima. Use o amortecimento múltiplo."
                id <= 40 -> "Desenhe tubos que levem o personagem através do labirinto espinhoso."
                id == 41 -> "A gravidade é muito baixa, lance-se descrevendo uma única parábola."
                id == 42 -> "Gravidade Zero. Desenhe curvas para bater na bola e dar-lhe inércia."
                id <= 50 -> "O teste definitivo: tinta escassa, gravidade mista, perigos móveis!"
                else -> defaultHint
            }
            else -> defaultHint
        }
    }
}

fun Level.localizedName(): String {
    val lang = java.util.Locale.getDefault().language
    return LevelTranslator.getTranslatedName(id, name, lang)
}

fun Level.localizedHint(): String {
    val lang = java.util.Locale.getDefault().language
    return LevelTranslator.getTranslatedHint(id, hint, lang)
}
