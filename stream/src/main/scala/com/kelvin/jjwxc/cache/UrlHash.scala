package com.kelvin.jjwxc.cache

import java.math.BigInteger
import java.security.MessageDigest

object UrlHash {
  def hash(url: String): String = {
    val hashed = new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8")))

    String.format("%032x", hashed)
  }
}
