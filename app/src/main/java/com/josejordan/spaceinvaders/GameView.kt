package com.josejordan.spaceinvaders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class GameView(context: Context) : View(context) {
    private val paint = Paint()

    private var player: RectF
    private var enemies = mutableListOf<RectF>()
    private var bullets = mutableListOf<RectF>()

    private val playerSpeed = 10f
    private var playerDirection = 0

    private val enemySpeed = 2f
    private var enemyDirection = 1

    init {
        paint.color = Color.WHITE
        player = RectF(400f, 1000f, 500f, 1020f)

        for (i in 0..4) {
            for (j in 0..9) {
                enemies.add(RectF(80f + j * 60, 400f + i * 60, 130f + j * 60, 450f + i * 60))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        canvas.drawRect(player, paint)
        enemies.forEach { enemy -> canvas.drawRect(enemy, paint) }
        bullets.forEach { bullet -> canvas.drawRect(bullet, paint) }

        update()
        invalidate()
    }

    private fun update() {
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
                enemy.offset(0f, 40f)
            }
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
            }
        }
        return true
    }
}
