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

        // Write chunks to temp files (always needed now for silence prepend)
        val tempDir = Files.createTempDirectory("tts-chunks")

        // Generate 500ms silence as the first chunk
        val silenceFile = tempDir.resolve("silence.mp3")
        val silenceProcess = ProcessBuilder(
            "ffmpeg", "-y", "-f", "lavfi", "-i", "anullsrc=r=48000:cl=mono",
            "-t", "0.5", "-codec:a", "libmp3lame", "-b:a", "128k",
            silenceFile.toAbsolutePath().toString()
        )
            .redirectErrorStream(true)
            .start()
        if (silenceProcess.waitFor() != 0) {
            log.warn("Failed to generate silence, proceeding without it")
        }

        val chunkFiles = mutableListOf<Path>()
        if (Files.exists(silenceFile)) {
            chunkFiles.add(silenceFile)
        }
        audioChunks.forEachIndexed { index, bytes ->
            val chunkFile = tempDir.resolve("chunk_$index.mp3")
            Files.write(chunkFile, bytes)
            chunkFiles.add(chunkFile)
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
