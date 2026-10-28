package com.watchcat.app.ssh

import com.jcraft.jsch.JSch

/**
 * Android 运行时通常不提供 JDK 15+ 的 EdDSA JCE 实现，
 * 因此统一使用 JSch 自带的 BouncyCastle 后端处理 Ed25519 / Ed448。
 */
object JschSupport {
    fun configure() {
        JSch.setConfig("keypairgen.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        JSch.setConfig("keypairgen_fromprivate.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        JSch.setConfig("ssh-ed25519", "com.jcraft.jsch.bc.SignatureEd25519")
        JSch.setConfig("ssh-ed448", "com.jcraft.jsch.bc.SignatureEd448")
    }
}
