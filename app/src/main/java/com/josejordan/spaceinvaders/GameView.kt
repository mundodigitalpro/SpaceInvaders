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

    private val enemySpeed = 2f
    private var enemyDirection = 1

    private val bulletSpeed = 20f

    private val random = Random()

    private var playerLives = 3

    private var isGameOver = false


    private val enemyShootHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (enemies.isNotEmpty()) {
                val shooter = enemies[random.nextInt(enemies.size)]
                bullets.add(Pair(RectF(shooter.centerX() - 5, shooter.bottom, shooter.centerX() + 5, shooter.bottom + 20), bulletSpeed)) // Agregar la dirección de la bala
                sendEmptyMessageDelayed(0, (1000..3000).random().toLong())
            }
        }
    }


    init {
        paint.color = Color.WHITE
        player = RectF(400f, 1000f, 500f, 1020f)

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

        // Mostrar "Game Over" si el juego ha terminado
        if (isGameOver) {
            paint.textSize = 100f
            canvas.drawText("Game Over", width / 2f, height / 2f, paint)
            return
        }
        update()
        invalidate()
    }

    private fun update() {
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
            // El jugador se ha quedado sin vidas, mostrar "Game Over"
            isGameOver = true
        }



        // Verificar si el jugador se ha quedado sin vidas
        if (playerLives <= 0) {
            // El jugador se ha quedado sin vidas, mostrar "Game Over"
            isGameOver = true
        }



    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                playerDirection = if (event.x > width / 2) {
                    1
                } else {
                    -1
                }
            }
            MotionEvent.ACTION_UP -> {
                playerDirection = 0
                // Disparar una bala desde el jugador
                bullets.add(Pair(RectF(player.centerX() - 5, player.top - 20, player.centerX() + 5, player.top), -bulletSpeed)) // Agregar la dirección de la bala
            }
        }
        return true
    }
}
