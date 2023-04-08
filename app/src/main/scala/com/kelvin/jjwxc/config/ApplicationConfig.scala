package com.kelvin.jjwxc.config

final case class ApplicationConfig(jjwxcConfig: JjwxcConfig)

final case class JjwxcConfig(
    hostUrl: String,
    urlEncoding: String,
    loginUrl: String
)
