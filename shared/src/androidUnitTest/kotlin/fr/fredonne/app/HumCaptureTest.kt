package fr.fredonne.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class HumCaptureTest {

    @Test
    fun `les blocs deposes depuis plusieurs fils sont tous conserves`() = runBlocking {
        val capture = HumCapture(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        (1..4).map {
            async(Dispatchers.Default) { repeat(250) { capture.offer(FloatArray(100) { 1f }) } }
        }.awaitAll()
        assertEquals(4 * 250 * 100, capture.finish().size)
    }

    @Test
    fun `le bloc est copie a la frontiere`() = runBlocking {
        val capture = HumCapture(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val chunk = floatArrayOf(1f, 2f, 3f)
        capture.offer(chunk)
        chunk.fill(0f)
        assertEquals(listOf(1f, 2f, 3f), capture.finish().toList())
    }

    @Test
    fun `la capture est plafonnee`() = runBlocking {
        val capture = HumCapture(CoroutineScope(SupervisorJob() + Dispatchers.Default), maxSamples = 150)
        repeat(3) { capture.offer(FloatArray(100)) }
        assertEquals(150, capture.finish().size)
    }
}
