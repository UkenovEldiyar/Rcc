package com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults

import java.io.File
import java.util.jar.JarFile

class ClasspathClassFinder(private val classpathRoots: List<File>) {
    private val jarCache = mutableMapOf<File, JarFile?>()

    fun findClassBytes(internalName: String): ByteArray? {
        val relativePath = "$internalName.class"
        for (root in classpathRoots) {
            if (root.isDirectory) {
                val file = File(root, relativePath)
                if (file.isFile) return file.readBytes()
            } else if (root.isFile) {
                val jar = jarCache.getOrPut(root) {
                    runCatching { JarFile(root) }.getOrNull()
                } ?: continue
                val entry = jar.getJarEntry(relativePath) ?: continue
                jar.getInputStream(entry).use { return it.readBytes() }
            }
        }
        return null
    }
}