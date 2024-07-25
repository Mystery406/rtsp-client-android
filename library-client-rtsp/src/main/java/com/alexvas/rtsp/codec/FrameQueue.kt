package com.alexvas.rtsp.codec

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

enum class VideoCodecType {
    H264, H265, UNKNOWN
}

enum class AudioCodecType {
    AAC_LC, UNKNOWN
}

class VideoFrameQueue(frameQueueCapacity: Int): FrameQueue<FrameQueue.VideoFrame>(frameQueueCapacity)
class AudioFrameQueue(frameQueueCapacity: Int): FrameQueue<FrameQueue.AudioFrame>(frameQueueCapacity)

/**
 * Queue for concurrent adding/removing audio/video frames.
 */
open class FrameQueue<T>(private val frameQueueCapacity: Int) {

    interface Frame {
        val data: ByteArray
        val offset: Int
        val length: Int
        val timestampMs: Long  // presentation time in msec
    }

    data class VideoFrame(
        /** Only H264 codec supported */
        val codecType: VideoCodecType,
        /** Indicates whether it is a keyframe or not */
        val isKeyframe: Boolean,
        override val data: ByteArray,
        override val offset: Int,
        override val length: Int,
        /** Video frame timestamp (msec) generated by camera */
        override val timestampMs: Long,
        /** Captured (received) video frame timestamp (msec). If -1, not supported. */
        val capturedTimestampMs: Long = -1
    ) : Frame

    data class AudioFrame(
        val codecType: AudioCodecType,
//      val sampleRate: Int,
        override val data: ByteArray,
        override val offset: Int,
        override val length: Int,
        override val timestampMs: Long,
    ) : Frame

    private val queue = ArrayBlockingQueue<T>(frameQueueCapacity)

    val size: Int
        get() = queue.size

    val capacity: Int
        get() = frameQueueCapacity

    @Throws(InterruptedException::class)
    fun push(frame: T): Boolean {
        if (queue.offer(frame, 5, TimeUnit.MILLISECONDS)) {
            return true
        }
//        Log.w(TAG, "Cannot add frame, queue is full")
        return false
    }

    @Throws(InterruptedException::class)
    open fun pop(timeout: Long = 1000): T? {
        try {
            val frame: T? = queue.poll(timeout, TimeUnit.MILLISECONDS)
//            if (frame == null) {
//                Log.w(TAG, "Cannot get frame within 1 sec, queue is empty")
//            }
            return frame
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return null
    }

    fun clear() {
        queue.clear()
    }

    fun copyInto(dstFrameQueue: FrameQueue<T>) {
        dstFrameQueue.queue.addAll(queue)
    }

    companion object {
        private val TAG: String = FrameQueue::class.java.simpleName
    }

}
