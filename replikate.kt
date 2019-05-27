#!/usr/bin/env kscript
//DEPS org.yaml:snakeyaml:1.24
//DEPS org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1

import ExecutionState.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.net.URLEncoder

val parser = Yaml(Constructor(ExperimentationConfig::class.java))

class ExperimentationConfig(
    var src: String = "",
    var build: String = "",
    var versioning: Versioning? = null,
    var output: String = "",
    var compile: String = "",
    var execute: String = "",
    var experiments: Array<Experiment> = emptyArray(),
    var measures: Array<String> = emptyArray(),
    var iterations: Int = 0,
    var timeout: TimeLimit? = null,
    var comments: String? = null
) {

    companion object {
        private const val SOURCE = "$" + "SOURCE"
        private const val BUILD = "$" + "BUILD"
        private const val OUTPUT = "$" + "OUTPUT"
    }

    private fun replaceKeyWords(str: String): String {
        var copy = str
        var old: String
        do {
        		old = copy
        		copy = copy.replace(SOURCE, src)
            .replace(BUILD, build)
            .replace(OUTPUT, output)
        } while (old != copy)
        return copy  
    }

    fun build() = replaceKeyWords(build)
    fun output() = replaceKeyWords(output)
    fun compile() = replaceKeyWords(compile)
    fun exec() = replaceKeyWords(execute)

}

data class Versioning(
    var repository: Repository = Repository(),
    var version: String = ""
)

data class Repository(
    var url: String = "",
    var authentication: Boolean = false
)

data class Experiment(
    var name: String = "",
    var parameters: Array<String> = emptyArray(),
    var disable: Boolean = false,
    var timeout: TimeLimit? = null,
    var difficulty: Int? = null
)

class TimeLimit(
    var duration: Long = 0,
    var unit: String = "SECONDS"
) {
    fun unit() = TimeUnit.valueOf(unit)
}

fun createFolderIfNotExists(path: String): File {
    val folder = File(path)
    if (!folder.exists()) folder.mkdirs()
    return folder
}

fun execute(
    cmd: String,
    args: Array<String> = emptyArray(),
    redirect: File? = null,
    verbose: Boolean = false,
    timeout: TimeLimit? = null
): ExecutionState {

    var fullCmd = cmd
    if (args.isNotEmpty()) fullCmd += " " + args.joinToString(" ")

    if (verbose) println("Run cmd ${fullCmd}")

    val fileWriter = redirect?.bufferedWriter()
    var hasTimeout = false

    return try {
        val process = Runtime.getRuntime().exec(fullCmd)

        val stdInput = process.inputStream.bufferedReader()
        val stdError = process.errorStream.bufferedReader()

        if (timeout != null) {
            if (!process.waitFor(timeout.duration, timeout.unit())) {
                hasTimeout = true
                process.destroy()
                process.waitFor()
            }
        }

        var line: String? = stdInput.readLine()
        while (line != null) {
            if (verbose) {
                println(line)
            }
            fileWriter?.append("$line\n")
            line = stdInput.readLine()
        }

        line = stdError.readLine()
        while (line != null) {
            if (verbose) {
                System.err.println("\u001B[31m$line\u001B[0m")
            }
            fileWriter?.append("[ERR] $line\n")
            line = stdError.readLine()
        }

        process.waitFor()
        if (process.exitValue() != 0) {
            Error
        } else {
            Done
        }

    } catch (e: Exception) {
        if (!hasTimeout) {
            e.printStackTrace()
            Error
        } else {
            println("TimeLimit")
            Timeout
        }
    } finally {
        fileWriter?.close()
    }
}

fun main(args: Array<String>) {
    val configurationFile = File(args[0])
    val configuration: ExperimentationConfig = parser.load(configurationFile.bufferedReader()) as ExperimentationConfig

    val outputFolder = createFolderIfNotExists(configuration.output())
    val result = File(configuration.output(), "results.csv")

    val computationOf = mapOf<String, (LongArray) -> Any>(
        "min" to ::_min,
        "max" to ::_max,
        "mean" to ::_mean,
        "std" to ::_std
    )

    val init = "--init" in args
    val build = "--compile" in args
    val clean = "--clean" in args
    val verbose = "--verbose" in args
    val runOpt = "--run" in args

    if(init) {
        if (configuration.versioning == null) {
            println("No versioning data where found")
            return
        }

        val cancelInit = if (File(configuration.src).exists()) {
            var line: String
            do {
                print("Le dossier existe déjà voulez-vous l'écraser (o\\N)? ")
                line = readLine()!!
            } while (line !in listOf("", "y", "Y", "o", "O", "n", "N"))

            when (line) {
                "o", "O", "y", "Y" -> {
                    File(configuration.src).deleteRecursively()
                    false
                }
                else -> {
                    println("Cancel init")
                    true
                }
            }
        } else false

        if (!cancelInit) {
            println("Init src")
            val (url, authentication) = configuration.versioning!!.repository
            val version = configuration.versioning!!.version
            val gitClone = if (authentication) {
                println("Git credentials - ")
                print("username: ")
                val userName = URLEncoder.encode(readLine()!!, "UTF-8")
                print("password: ")
                val password = URLEncoder.encode(readLine()!!, "UTF-8")
                // git clone https://username:password@github.com/username/repository.git
                val (protocol, url) = url.split("://")
                execute("git clone $protocol://$userName:$password@$url ${configuration.src}", verbose = verbose)
            } else {
                execute("git clone $url ${configuration.src}")
            }

            if (gitClone == Done) {
                println("Clonage dans ${configuration.src}")
                val checkout = execute("git --git-dir ${configuration.src}/.git --work-tree ${configuration.src} checkout $version", verbose = verbose)
                if (checkout != Done) {
                    println("Error during git checkout")
                } else {
                    println("Checkout done")
                }
            } else {
                println("Error during git clone")
            }
        }
    }

    if (build) {
        createFolderIfNotExists(configuration.build())
        print("Build... ")
        if (execute(configuration.compile(), verbose = verbose) == Done)
            println("Done")
        else
            println("Error")
    }

    if (clean) {
        println("Cleaning experiments...")
        outputFolder.listFiles()
            .asSequence()
            .filter { it.name.startsWith(".lock_") }
            .forEach { it.delete() }

        outputFolder.listFiles()
            .asSequence()
            .filter { it.isDirectory }
            .forEach { it.deleteRecursively() }

        if (result.exists()) result.delete()
    }

    if (runOpt) {
        println("Run...")
        if (!result.exists()) result.createNewFile()
        result.writer().apply {
            append("\"name\"")
            append(",")
            append(configuration.measures.joinToString(","))
            if (configuration.measures.isNotEmpty()) {
                append(",")
            }
            appendln((0 until configuration.iterations).joinToString(",") { "\"iteration $it\"" })
        }.close()

        for (experiment in configuration.experiments.filter { !it.disable }.sortedBy { it.difficulty ?: 0 }) {
            val lockFile = File(configuration.output(), ".lock_" + experiment.name)
            if (!lockFile.exists()) {
                lockFile.createNewFile()
                val now = LocalDateTime.now()
                println("Running experiment ${experiment.name} @ ${now % "HH:mm"} the ${now % "EE dd, YYYY"}")
                val results = LongArray(configuration.iterations)
                createFolderIfNotExists(configuration.output() + "/" + experiment.name)

                for (iteration in 0 until configuration.iterations) {
                	  val iterationState = runIteration(iteration, configuration, experiment, results, verbose)
                	  
                	  if (iterationState == Error) {
                	  	 lockFile.delete()
                	  }
                	  if (iterationState != Done) {
                	  	 break
                	  }
                }

                writeMeasures(
                    experiment.name,
                    configuration.measures,
                    results,
                    result
                )
            }
        }
    }
}

fun _min(time: LongArray): Long {
    var min = time[0]
    for (i in 0 until time.size) {
        if (time[i] < min) min = time[i]
    }
    return min
}

fun _max(time: LongArray): Long {
    var max = time[0]
    for (i in 0 until time.size) {
        if (time[i] > max) max = time[i]
    }
    return max
}

fun _mean(time: LongArray): Double {
    var sum = 0L
    for (value in time) sum += value
    return sum.toDouble() / time.size.toDouble()
}

fun _std(time: LongArray): Double {
    val mean = _mean(time)
    var sumError = 0.0
    for (value in time) {
        sumError += (value - mean) * (value - mean)
    }
    return Math.sqrt(sumError / time.size.toDouble())
}

fun writeMeasures(
    name: String,
    measures: Array<String>,
    results: LongArray,
    output: File
) {

    val computationOf = mapOf(
        "min" to ::_min,
        "max" to ::_max,
        "mean" to ::_mean,
        "std" to ::_std
    )

    val writer = FileWriter(output, true)
    writer.append("\"$name\"")
    writer.append(",")
    writer.append(
        measures.joinToString(",") { measure ->
            val fn = computationOf[measure]
            if (fn != null) {
                "${fn(results)}"
            } else {
                "\"Invalid\""
            }
        }
    )
    if(measures.isNotEmpty()) {
        writer.append(",")
    }
    writer.appendln(results.joinToString(",") { "$it" })
    writer.close()

}

fun runIteration(
    n: Int,
    configuration: ExperimentationConfig,
    experiment: Experiment,
    results: LongArray,
    verbose: Boolean
): ExecutionState {

    val outputFile = File(configuration.output() + "/" + experiment.name, "$n.txt")
    if (!outputFile.exists()) outputFile.createNewFile()

    val start = System.currentTimeMillis()
    val validExecution = execute(
        configuration.exec(),
        experiment.parameters,
        outputFile,
        verbose,
        experiment.timeout ?: configuration.timeout
    )

    when (validExecution) {
        Done -> {
            val end = System.currentTimeMillis()
            results[n] = (end - start) / 1000
        }
        Timeout -> {
            results[n] = -1
        }
        Error -> {
            results[n] = -2
            outputFile.delete()
        }
    }

    return validExecution

}

operator fun LocalDateTime.rem(format: String): String {
    return format(DateTimeFormatter.ofPattern(format, Locale.ENGLISH))
}

enum class ExecutionState {
    Timeout,
    Done,
    Error
}
