package com.josejordan.spaceinvaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.View
import java.util.Random


class GameView(context: Context) : View(context) {
    private val paint = Paint()

    private var player: RectF
    private var enemies = mutableListOf<RectF>()
    private var bullets = mutableListOf<Pair<RectF, Float>>() // Cambiar a una lista de Pares
    private val playerSpeed = 10f
    private var playerDirection = 0

    private var enemyDirection = 1
    private val bulletSpeed = 20f
    private val random = Random()
    private var playerLives = 3

    private var currentLevel = 1
    private val baseEnemySpeed = 2f
    private val enemySpeedMultiplier = 1.5f // Aumenta la velocidad en un 50% por cada nivel
    private var enemySpeed = baseEnemySpeed * (1 + (currentLevel - 1) * enemySpeedMultiplier)

    private enum class GameState {
        START, PLAYING, GAME_OVER, PLAY_AGAIN
    }

    private var gameState = GameState.START

    private val enemyShootHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (enemies.isNotEmpty()) {
                val shooter = enemies[random.nextInt(enemies.size)]
                bullets.add(
                    Pair(
                        RectF(
                            shooter.centerX() - 5,
                            shooter.bottom,
                            shooter.centerX() + 5,
                            shooter.bottom + 20
                        ), bulletSpeed
                    )
                ) // Agregar la dirección de la bala
                sendEmptyMessageDelayed(0, (1000..3000).random().toLong())
            }
        }
    }


    init {
        paint.color = Color.WHITE
        player = RectF(400f, 2000f, 500f, 2020f) // Cambia 1000f a 1100f y 1020f a 1120f

        for (i in 0..4) {
            for (j in 0..9) {
                enemies.add(RectF(80f + j * 60, 400f + i * 60, 130f + j * 60, 450f + i * 60))
            }
        }
        enemyShootHandler.sendEmptyMessage(0)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(player, paint)
        enemies.forEach { enemy -> canvas.drawRect(enemy, paint) }
        bullets.forEach { bullet -> canvas.drawRect(bullet.first, paint) }

        // Dibujar vidas del jugador
        paint.textSize = 60f
        canvas.drawText("Lives: $playerLives", 50f, height - 50f, paint)

        // Dibujar el nivel actual
        paint.textSize = 60f
        canvas.drawText("Level: $currentLevel", width - 300f, height - 50f, paint)

        when (gameState) {
            GameState.START -> {
                paint.textSize = 100f
                val titleText = "Space Invaders"
                val titleTextWidth = paint.measureText(titleText)
                val titleTextX = (width - titleTextWidth) / 2f
                val titleTextY = height / 2f - 100f
                canvas.drawText(titleText, titleTextX, titleTextY, paint)

                val startText = "Touch to start"
                val startTextWidth = paint.measureText(startText)
                val startTextX = (width - startTextWidth) / 2f
                val startTextY = height / 2f + 200f
                canvas.drawText(startText, startTextX, startTextY, paint)
            }

            GameState.GAME_OVER -> {
                paint.textSize = 100f
                canvas.drawText("Game Over", width / 2f - 250f, height / 2f, paint)
            }

            GameState.PLAY_AGAIN -> {
                paint.textSize = 80f
                canvas.drawText("Play Again", width / 2f - 220f, height / 2f + 100f, paint)
            }

            else -> {}
        }

        update()
        invalidate()
    }

    private fun update() {

        if (gameState != GameState.PLAYING) {
            return

        }
        // Verificar si todos los enemigos han sido destruidos
        if (gameState == GameState.PLAYING && enemies.isEmpty()) {
            nextLevel()
        }


        // Actualizar posición del jugador
        player.offset(playerDirection * playerSpeed, 0f)

        if (player.left < 0) {
            player.offsetTo(0f, player.top)
        }
        if (player.right > width) {
            player.offsetTo(width - player.width(), player.top)
        }

        // Mover enemigos
        for (enemy in enemies) {
            enemy.offset(enemyDirection * enemySpeed, 0f)
        }

        // Verificar si algún enemigo alcanza el borde izquierdo o derecho de la pantalla
        var changeDirection = false
        for (enemy in enemies) {
            if (enemy.left < 0 || enemy.right > width) {
                changeDirection = true
                break
            }
        }

        // Si algún enemigo alcanzó el borde, cambiar la dirección de todos y moverlos hacia abajo
        if (changeDirection) {
            enemyDirection *= -1
            for (enemy in enemies) {
                enemy.offset(0f, 40f) // Cambiar el signo a positivo para mover hacia abajo
            }
        }

        // Mover balas
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bulletPair = bulletIterator.next()
            val bullet = bulletPair.first
            val direction = bulletPair.second

            // Mover bala en función de su dirección
            bullet.offset(0f, direction)

            // Eliminar balas fuera de la pantalla
            if (bullet.top < 0 || bullet.bottom > height) {
                bulletIterator.remove()
            }
        }

        // Verificar colisiones entre balas del jugador y enemigos
        val enemyIterator = enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val hitBulletIndex = bullets.indexOfFirst { bulletPair ->
                val bullet = bulletPair.first
                val direction = bulletPair.second

                // Solo considerar balas del jugador (dirección hacia arriba)
                direction < 0 && RectF.intersects(enemy, bullet)
            }

            if (hitBulletIndex != -1) {
                // Eliminar enemigo y bala que lo golpeó
                enemyIterator.remove()
                bullets.removeAt(hitBulletIndex)
            }
        }

        // Verificar colisiones entre balas de los enemigos y el jugador
        val hitEnemyBulletIndex = bullets.indexOfFirst { bulletPair ->
            val bullet = bulletPair.first
            val direction = bulletPair.second

            // Solo considerar balas de los enemigos (dirección hacia abajo) y que colisionen con el jugador
            direction > 0 && RectF.intersects(player, bullet)
        }

        if (hitEnemyBulletIndex != -1) {
            // Eliminar la bala que golpeó al jugador y descontar una vida
            bullets.removeAt(hitEnemyBulletIndex)
            playerLives--
        }

        // Verificar si algún enemigo colisiona con el jugador
        val enemyCollision = enemies.any { enemy -> RectF.intersects(player, enemy) }

        if (enemyCollision) {
            // Reducir una vida del jugador
            playerLives--

            // Mover enemigos a su posición inicial
            enemies.clear()
            for (i in 0..4) {
                for (j in 0..9) {
                    enemies.add(RectF(80f + j * 60, 400f + i * 60, 130f + j * 60, 450f + i * 60))
                }
            }
        }

        // Verificar si el jugador se ha quedado sin vidas
        if (playerLives <= 0) {
            // El jugador se ha quedado sin vidas, cambiar el estado a "Game Over"
            gameState = GameState.GAME_OVER
            enemyShootHandler.removeMessages(0) // Detener disparos de enemigos
        }
    }

    private fun restartGame() {

        currentLevel = 1
        enemySpeed = baseEnemySpeed * (1 + (currentLevel - 1) * enemySpeedMultiplier)

        playerLives = 3
        gameState = GameState.PLAYING

        // Mover enemigos a su posición inicial
        enemies.clear()
        for (i in 0..4) {
            for (j in 0..9) {
                enemies.add(RectF(80f + j * 60, 400f + i * 60, 130f + j * 60, 450f + i * 60))
            }
        }
        enemyShootHandler.sendEmptyMessage(0) // Reiniciar el enemyShootHandler
    }

    private fun nextLevel() {
        if (currentLevel < 3) { // Limita el juego a 3 niveles
            currentLevel++
            enemySpeed = baseEnemySpeed * (1 + (currentLevel - 1) * enemySpeedMultiplier)
            // Vuelve a generar enemigos
            enemies.clear()
            for (i in 0..4) {
                for (j in 0..9) {
                    enemies.add(RectF(80f + j * 60, 400f + i * 60, 130f + j * 60, 450f + i * 60))
                }
            }
        } else {
            gameState = GameState.GAME_OVER
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (gameState == GameState.PLAYING) {
                    playerDirection = if (event.x > width / 2) {
                        1
                    } else {
                        -1
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                when (gameState) {
                    GameState.START -> {
                        gameState = GameState.PLAYING
                    }

                    GameState.PLAYING -> {
                        playerDirection = 0
                        // Disparar una bala desde el jugador
                        bullets.add(
                            Pair(
                                RectF(
                                    player.centerX() - 5,
                                    player.top - 20,
                                    player.centerX() + 5,
                                    player.top
                                ), -bulletSpeed
                            )
                        )
                    }

                    GameState.GAME_OVER -> {
                        gameState = GameState.PLAY_AGAIN
                    }

                    GameState.PLAY_AGAIN -> {
                        restartGame()
                    }
                }
            }
        }
        return true
    }


}
