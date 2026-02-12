package com.aisummarypodcast.tts

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class AudioConcatenator {

    private val log = LoggerFactory.getLogger(javaClass)

    fun concatenate(audioChunks: List<ByteArray>, outputPath: Path): Path {
        Files.createDirectories(outputPath.parent)

        if (audioChunks.size == 1) {
            Files.write(outputPath, audioChunks.first())
            log.info("Single chunk written directly to {}", outputPath)
            return outputPath
        }

        // Write chunks to temp files
        val tempDir = Files.createTempDirectory("tts-chunks")
        val chunkFiles = audioChunks.mapIndexed { index, bytes ->
            val chunkFile = tempDir.resolve("chunk_$index.mp3")
            Files.write(chunkFile, bytes)
            chunkFile
        }

        // Create FFmpeg concat list
        val concatList = tempDir.resolve("concat.txt")
        val listContent = chunkFiles.joinToString("\n") { "file '${it.toAbsolutePath()}'" }
        Files.writeString(concatList, listContent)

        // Run FFmpeg
        val process = ProcessBuilder(
            "ffmpeg", "-y", "-f", "concat", "-safe", "0",
            "-i", concatList.toAbsolutePath().toString(),
            "-c", "copy",
            outputPath.toAbsolutePath().toString()
        )
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val output = process.inputStream.bufferedReader().readText()
            throw RuntimeException("FFmpeg failed with exit code $exitCode: $output")
        }

        // Clean up temp files
        chunkFiles.forEach { Files.deleteIfExists(it) }
        Files.deleteIfExists(concatList)
        Files.deleteIfExists(tempDir)

        log.info("Concatenated {} chunks into {}", audioChunks.size, outputPath)
        return outputPath
    }
}
