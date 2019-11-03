package com.dming.smallScan

class CameraSize : Comparable<CameraSize> {

    val width: Int
    val height: Int
    lateinit var srcSize: CameraSize

    constructor(size1: Int, size2: Int) {
        this.width = size1
        this.height = size2
    }


    constructor(size1: Int, size2: Int, srcSize: CameraSize) {
        this.width = size1
        this.height = size2
        this.srcSize = srcSize
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other is CameraSize) {
            val size = other as CameraSize?
            return width == size!!.width && height == size.height
        }
        return false
    }

    override fun toString(): String {
        return width.toString() + "x" + height
    }

    override fun hashCode(): Int {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return height xor (width shl Integer.SIZE / 2 or width.ushr(Integer.SIZE / 2))
    }

    override fun compareTo(other: CameraSize): Int {
        return width * height - other.width * other.height
    }

}