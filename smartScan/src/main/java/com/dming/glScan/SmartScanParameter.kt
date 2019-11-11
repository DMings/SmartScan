package com.dming.glScan

import android.graphics.drawable.Drawable

/**
 * 定义GLScanView传入参数类
 */
class SmartScanParameter {
    // 扫描框宽度，单位像素
    var scanWidth: Float = 0f
    // 扫描框高度，单位像素
    var scanHeight: Float = 0f
    // 扫描框宽度百分比 0 - 1.0f
    var scanPercentWidth: Float = 0f
    // 扫描框高度百分比 0 - 1.0f
    var scanPercentHeight: Float = 0f
    // 扫描框距离头部偏移，单位像素
    var scanTopOffset: Float = 0f
    // 扫描框距离头部偏移，相对高度百分比 0 - 1.0f
    var scanPercentTopOffset: Float = 0f
    // 约束扫描框为正方形，将使用最短的边为边长，可用在扫描二维码,默认开启
    var scanMustSquare: Boolean? = null
    // 扫描线drawable，当存在时，优先使用
    var scanLine: Drawable? = null
    // 扫描框drawable，当存在时，优先使用
    var scanCorner: Drawable? = null
    // 扫描背景颜色，一般黑色透明
    var scanBackgroundColor: Int? = null
    // 是否加入一维码解码，默认为QR二维码解码，按需加入
    var addOneDCode: Boolean = false
    // 仅使用一维码解码
    var onlyOneDCode: Boolean = false
    // 扫描角颜色
    var scanCornerColor: Int? = null
    // 自定义扫描角长度
    var scanCornerSize: Float = 0f
    // 自定义扫描角线宽度
    var scanCornerThick: Float = 0f
    // 扫描线颜色
    var scanLineColor: Int? = null
    // 扫描线宽度大小
    var scanLineWidth: Float = 0f
    // 扫描框的细线宽度，一般1px
    var scanFrameLineWidth: Float? = null
    // 扫描框的细线颜色
    var scanFrameLineColor: Int? = null
    // 禁止窗口双指放大，默认开启
    var disableScale: Boolean = false
    // 使能滴一声的扫描成功声音
    var enableBeep: Boolean = false
    // 使能震动扫描成功
    var enableVibrate: Boolean = false
    // 使能闪光灯按钮，由于按钮位置图像可变性太强，默认是不开启的，由用户定制
    var enableFlashlightBtn: Boolean = false
}