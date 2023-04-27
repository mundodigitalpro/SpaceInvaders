package com.josejordan.spaceinvaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import java.util.Random
import kotlinx.coroutines.*


class GameView(context: Context) : View(context) {
    private val paint = Paint()


    private var player: RectF
    private var enemies = mutableListOf<RectF>()
    private var misteryEnemy: RectF? = null

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
    private var score = 0
    private var highScore = 0
    private val sharedPreferences =
        context.getSharedPreferences("space_invaders_high_score", Context.MODE_PRIVATE)


    private enum class GameState {
        START, PLAYING, GAME_OVER, PLAY_AGAIN
    }

    private var gameState = GameState.START

    private var enemyShootJob: Job? = null

    private var misteryEnemyDirection = 1f


    //** Inicio de la implementación de obstáculos
    data class Obstacle(val rect: RectF, var health: Int)

    private val obstacles = mutableListOf<Obstacle>()
    private fun createObstacles() {
        obstacles.clear()
        val obstacleWidth = 60f
        val obstacleHeight = 40f
        val horizontalSpacing = 100f
        val verticalSpacing = 50f
        val numberOfRows = 2
        val numberOfColumns = 4
        val initialHealth = 3

        val totalWidth = numberOfColumns * (obstacleWidth + horizontalSpacing) - horizontalSpacing
        val startX = (width - totalWidth) / 2
        val startY = 1800f // Posición vertical de inicio de los obstáculos

        for (i in 0 until numberOfRows) {
            for (j in 0 until numberOfColumns) {
                val left = startX + j * (obstacleWidth + horizontalSpacing)
                val top = startY + i * (obstacleHeight + verticalSpacing)
                val right = left + obstacleWidth
                val bottom = top + obstacleHeight

                obstacles.add(Obstacle(RectF(left, top, right, bottom), initialHealth))
            }
        }
    }


    private fun drawObstacles(canvas: Canvas) {
        paint.color = Color.GREEN
        for (obstacle in obstacles) {
            if (obstacle.health > 0) {
                canvas.drawRect(obstacle.rect, paint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createObstacles()
    }


    private fun checkBulletObstacleCollisions() {
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bulletPair = bulletIterator.next()
            val bullet = bulletPair.first
            val direction = bulletPair.second

            // Solo considerar balas de los enemigos (dirección hacia abajo)
            if (direction > 0) {
                val obstacleHitIndex = obstacles.indexOfFirst { obstacle ->
                    obstacle.health > 0 && RectF.intersects(obstacle.rect, bullet)
                }

                if (obstacleHitIndex != -1) {
                    val obstacle = obstacles[obstacleHitIndex]
                    obstacle.health -= 1
                    bulletIterator.remove()
                }
            }
        }
    }

    //** Fin de la implementación de obstáculos

    private suspend fun enemyShoot() {

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
        }
        delay((1000..3000).random().toLong())

    }


    init {
        // Cargar el high score guardado de SharedPreferences
        highScore = sharedPreferences.getInt("high_score", 0)

        paint.color = Color.GREEN
        player = RectF(400f, 2000f, 500f, 2020f)

        for (i in 0..4) {
            for (j in 0..9) {
                enemies.add(RectF(80f + j * 60, 100f + i * 60, 130f + j * 60, 150f + i * 60))
            }
        }

        //** Crear obstáculos
        createObstacles()

        enemyShootJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                enemyShoot()
            }
        }
    }

    // Método para guardar el high score en SharedPreferences
    private fun saveHighScore() {
        val editor = sharedPreferences.edit()
        editor.putInt("high_score", highScore)
        editor.apply()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.WHITE
        canvas.drawColor(Color.BLACK)
        canvas.drawRect(player, paint)
        enemies.forEach { enemy -> canvas.drawRect(enemy, paint) }
        bullets.forEach { bullet -> canvas.drawRect(bullet.first, paint) }

        //** Dibujar obstáculos
        drawObstacles(canvas)

        // Dibujar vidas del jugador
        paint.textSize = 60f
        canvas.drawText("Lives: $playerLives", 50f, height - 50f, paint)

        // Dibujar el nivel actual
        paint.textSize = 60f
        canvas.drawText("Level: $currentLevel", width - 300f, height - 50f, paint)

        val textWidthScore = paint.measureText("SCORE: $score")

        // Dibujar el SCORE
        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("SCORE: $score", 100f, 100f, paint)

        // Dibujar el HI-SCORE
        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("HI-SCORE: $highScore", 200f + textWidthScore + 100f, 100f, paint)

        // Dibujar el Mistery Enemy
        // Dibujar el rectángulo MisteryEnemy
        if (misteryEnemy != null) {
            paint.color = Color.RED
            canvas.drawRect(misteryEnemy!!, paint)
        }

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
        spawnMisteryEnemy()
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
                continue
            }
            // Verificar colisiones entre balas del jugador y enemigos
            if (direction < 0) {
                val enemyIterator = enemies.iterator()
                while (enemyIterator.hasNext()) {
                    val enemy = enemyIterator.next()

                    if (RectF.intersects(enemy, bullet)) {
                        // Eliminar enemigo y bala que lo golpeó
                        enemyIterator.remove()
                        bulletIterator.remove()
                        score += 10
                        break
                    }
                }
            }
            // Verificar colisiones entre balas y obstáculos
            else {
                /*                val hitObstacleIndex =
                                    obstacles.indexOfFirst { obstacle -> RectF.intersects(obstacle.rect, bullet) }
                                if (hitObstacleIndex != -1) {
                                    // Eliminar la bala que golpeó el obstáculo
                                    bulletIterator.remove()
                                }*/
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
                    enemies.add(
                        RectF(
                            80f + j * 60,
                            400f + i * 60,
                            130f + j * 60,
                            450f + i * 60
                        )
                    )
                }
            }
        }
        //Actualizar el highscore si es necesario
        if (score > highScore) {
            highScore = score
            saveHighScore()
        }

        // Verificar colisiones entre balas y obstáculos
        checkBulletObstacleCollisions()

        // Verificar si el jugador se ha quedado sin vidas
        if (playerLives <= 0) {
            // El jugador se ha quedado sin vidas, cambiar el estado a "Game Over"
            gameState = GameState.GAME_OVER
        }
        // Actualizar la posición del enemigo Mistery
        //misteryEnemy?.offset(if (misteryEnemy!!.left < 0) 3f else -3f, 0f)

        misteryEnemy?.offset(misteryEnemyDirection * 3f, 0f)


        // Eliminar el enemigo Mistery si está fuera de la pantalla
        /*
                if (misteryEnemy != null && (misteryEnemy!!.right <= 0 || misteryEnemy!!.left >= width)) {
                    misteryEnemy = null
                }
        */

        if (misteryEnemy != null) {
            if (misteryEnemy!!.left <= 0) {
                misteryEnemyDirection = 1f
            } else if (misteryEnemy!!.right >= width) {
                misteryEnemyDirection = -1f
                misteryEnemy = null
            }
        }


    }

    private fun restartGame() {
        score = 0
        currentLevel = 1
        enemySpeed = baseEnemySpeed * (1 + (currentLevel - 1) * enemySpeedMultiplier)

        playerLives = 3
        gameState = GameState.PLAYING

        // Mover enemigos a su posición inicial
        enemies.clear()
        for (i in 0..4) {
            for (j in 0..9) {
                // Cambiar 400f a 100f para mover los enemigos más arriba
                enemies.add(RectF(80f + j * 60, 100f + i * 60, 130f + j * 60, 150f + i * 60))
            }
        }
        // Reiniciar los obstáculos
        obstacles.clear()
        createObstacles()
    }

    private fun nextLevel() {
        if (currentLevel < 3) { // Limita el juego a 3 niveles
            currentLevel++
            enemySpeed = baseEnemySpeed * (1 + (currentLevel - 1) * enemySpeedMultiplier)
            // Vuelve a generar enemigos
            enemies.clear()
            for (i in 0..4) {
                for (j in 0..9) {
                    enemies.add(
                        RectF(
                            80f + j * 60,
                            400f + i * 60,
                            130f + j * 60,
                            450f + i * 60
                        )
                    )
                }
            }

            // Reiniciar los obstáculos
            obstacles.clear()
            createObstacles()

        } else {
            gameState = GameState.GAME_OVER
        }
    }

    private fun spawnMisteryEnemy() {
        if (misteryEnemy == null && random.nextFloat() < 0.01) {
            val y = 30f
            val left = if (random.nextBoolean()) -150f else width.toFloat()
            val right = left + 70f
            misteryEnemy = RectF(left, y, right, y + 40f)
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
