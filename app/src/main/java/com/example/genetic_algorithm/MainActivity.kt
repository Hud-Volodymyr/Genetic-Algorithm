package com.example.genetic_algorithm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var a: Int = 0
    private var b: Int = 0
    private var c: Int = 0
    private var d: Int = 0
    private var y: Int = 0
    private var generations = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val aView = findViewById<EditText>(R.id.a)
        val bView = findViewById<EditText>(R.id.b)
        val cView = findViewById<EditText>(R.id.c)
        val dVew  = findViewById<EditText>(R.id.d)
        val yView  = findViewById<EditText>(R.id.y)
        val solutionView = findViewById<TextView>(R.id.solution)
        findViewById<Button>(R.id.solve).setOnClickListener {
            try {
                validateInput(aView.text.toString(), bView.text.toString(), cView.text.toString(), dVew.text.toString(), yView.text.toString())

                val solutionStr = calculateDiophantine()

                solutionView.text = solutionStr
            } catch (e: IllegalStateException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(aStr: String, bStr: String, cStr: String, dStr: String, yStr: String) {
        if (aStr.isBlank() || bStr.isBlank() || cStr.isBlank() || dStr.isBlank() || yStr.isBlank()) {
            error("Please enter all values")
        }

        a = aStr.toInt()
        b = bStr.toInt()
        c = cStr.toInt()
        d = dStr.toInt()
        y = yStr.toInt()

        if (a <= 0 || b <= 0 || c <= 0 || d <= 0 || y <= 0) {
            error("Values must be grater than 0")
        }
    }

    private fun calculateDiophantine(): String {
        var population = generateSemiRandomPopulation()
        var fitness = calculateFitness(population)

        repeat(1000) {
            val index = fitness.indexOfFirst {it == 0}

            if (index != -1) {
                val solution = population[index]
                val deviation = (y - solution[0] * a - solution[1] * b - solution[2] * c - solution[3] * d)
                return "x1 = ${solution[0]}\nx2 = ${solution[1]}\nx3 = ${solution[2]}\nx4 = ${solution[3]}\nDeviation = $deviation"
            }

            population = cross(population, fitness)
            mutate(population)
            fitness = calculateFitness(population)
        }

        val bestLoser = findBestFitness(fitness)
        val closestChromosome = population[bestLoser]
        val deviation = (y - closestChromosome[0] * a - closestChromosome[1] * b - closestChromosome[2] * c - closestChromosome[3] * d)

        return "x1 = ${closestChromosome[0]}\nx2 = ${closestChromosome[1]}\nx3 = ${closestChromosome[2]}\nx4 = ${closestChromosome[3]}\nDeviation = $deviation\n"
    }

    private fun generateSemiRandomPopulation(): Array<IntArray> {
        val maxGene = y / 2
        val pool = 1..maxGene
        val population = Array(generations) { IntArray(4) }
        for (chromosome in 0 until generations) {
            population[chromosome] = intArrayOf(pool.random(), pool.random(), pool.random(), pool.random())

        }
        return population
    }

    private fun calculateFitness(population: Array<IntArray>) : IntArray {
        val res = IntArray(population.size)
        for (i in population.indices) {
            res[i] = fitness(population[i])
        }
        return res
    }

    private fun fitness(solution: IntArray) : Int {
        var res = y
        val coefficient = intArrayOf(a, b ,c ,d)
        for (i in coefficient.indices) {
            res -= coefficient[i] * solution[i]
        }
        return abs(res)
    }

    private fun cross(population: Array<IntArray>, fitness: IntArray) : Array<IntArray>{
        val probabilities = calculateProbabilities(fitness)
        val parentsPool = decideParents(population, probabilities)
        val couples = createCouples(parentsPool)
        return nextGen(couples)
    }

    private fun calculateProbabilities(fitness: IntArray): FloatArray {
        var sum = 0f
        val probabilities = FloatArray(fitness.size)
        for (i in fitness.indices) {
            val probability = 1f / fitness[i]
            sum += probability
            probabilities[i] = probability
        }
        val res = FloatArray(fitness.size)

        for (i in fitness.indices){
            res[i] = probabilities[i] / sum
        }
        return res
    }

    private fun createCouples(parentsPool: Array<IntArray>): Array<Pair<IntArray, IntArray>> {
        val res = Array(parentsPool.size / 2) { Pair(IntArray(4), IntArray(4)) }
        for (i in res.indices) {
            val random1 = (parentsPool.indices).random()
            val random2 = (parentsPool.indices).random()
            val couple = Pair(parentsPool[random1], parentsPool[random2])
            res[i] = couple
        }
        return res
    }

    private fun decideParents(population: Array<IntArray>, probabilties: FloatArray) : Array<IntArray> {
        val parents = Array(population.size) { IntArray(4) }
        val values = calculateValues(probabilties)
        for (i in population.indices) {
            val factor = Math.random()
            val index = calculateIndex(values, factor)
            val parent = population[index]
            parents[i] = parent
        }
        return parents
    }

    private fun calculateValues(probabilties: FloatArray) : FloatArray {
        var sum = 0f
        val res = FloatArray(probabilties.size)
        for (i in probabilties.indices) {
            res[i] = sum
            sum += probabilties[i]
        }

        return res
    }

    private fun calculateIndex(values : FloatArray, factor : Double) : Int {
        var index = values.size / 2

        var valuesStart = 0
        var valuesEnd = values.size - 1
        var start = values[index]
        var end = values[index + 1]
        while(true) {
            if (factor > end) {
               valuesStart = index + 1
            } else if (factor < start) {
                valuesEnd = index - 1
            } else {
                break
            }
            index = (valuesStart + (valuesEnd - valuesStart) / 2)
            start = values[index]
            end = if (index != values.lastIndex) {
                values[index + 1]
            } else {
                1f
            }
        }
        return index
    }

    private fun nextGen(couples: Array<Pair<IntArray, IntArray>>) : Array<IntArray>{
        val nextGen = Array(couples.size * 2) { IntArray(4) }

        for (i in couples.indices) {
            val couple = couples[i]
            val indices = (0..3).toList().toIntArray()
            val randomChange = (1..3).random()
            val randomizedGens = IntArray(randomChange)
            repeat(randomChange) {
                val index = (0 until (indices.size - it)).random()
                randomizedGens[it] = indices[index]
                indices[index] = indices[indices.lastIndex - it]
            }
            val first = couple.first.clone()
            val second = couple.second.clone()
            for (g in randomizedGens) {
                first[g] = couple.second[g]
                second[g] = couple.first[g]
            }

            nextGen[i * 2] = first
            nextGen[i * 2 + 1] = second
        }
        return nextGen
    }
    private fun mutate(population: Array<IntArray>) {
        for (i in population.indices) {
            if(Math.random() < 0.01) {
                val chromosome = population[i]
                chromosome[chromosome.indices.random()] += intArrayOf(-2, -1, 1, 2).random()
            }
        }
    }
    private fun findBestFitness(populationFitness: IntArray): Int {
        var index = 0
        var value = populationFitness[0]
        for (i in 1 until populationFitness.size) {
            if (populationFitness[i] < value) {
                index = i
                value = populationFitness[i]
            }
        }
        return index
    }
}