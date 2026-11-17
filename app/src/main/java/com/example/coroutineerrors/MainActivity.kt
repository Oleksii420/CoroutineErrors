package com.example.coroutineerrors

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coroutineerrors.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "Coroutines"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var calculationJob: Job? = null

    private val ceh = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Глобальне перехоплення крашу: ${throwable.message}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener { startCalculation() }

        binding.btnCancel.setOnClickListener {
            Log.d(TAG, "Натиснуто Cancel")
            calculationJob?.cancel()
        }

        binding.btnTestCrash.setOnClickListener { testCrash() }

        binding.btnGlobalCrash.setOnClickListener {
            val safeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + ceh)

            safeScope.launch {
                Log.d(TAG, "safeScope: починаю роботу...")
                delay(500)
                throw IllegalArgumentException("Ой")
            }
        }
    }

    private fun startCalculation() {
        calculationJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                for (i in 1..100) {
                    Thread.sleep(50)
                    ensureActive()
                    Log.d(TAG, "Обробка: $i%")
                }
                Log.d(TAG, "Роботу успішно завершено!")
            } catch (e: CancellationException) {
                Log.d(TAG, "Обробку скасовано користувачем!")
                throw e
            }
        }

    }

    private fun testCrash() {
        lifecycleScope.launch {
            Log.d(TAG, "Батьківська корутина: старт")

            supervisorScope {
                launch {
                    try {
                        Log.d(TAG, "Банер: завантаження...")
                        delay(1000)
                        throw RuntimeException("Банер не завантажився!")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Банер не вдалося завантажити: ${e.message}")
                    }
                }

                launch {
                    try {
                        Log.d(TAG, "Новини: завантаження...")
                        delay(2000)
                        Log.d(TAG, "Стрічка новин завантажена")
                    } catch (e: CancellationException) {
                        Log.d(TAG, "Новини: мене скасували")
                        throw e
                    }
                }
            }

            Log.d(TAG, "Обидві задачі завершились")
        }
    }
}