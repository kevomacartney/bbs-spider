package com.kelvin.jjwxc.orchestration
import com.kelvin.jjwxc.auth.JjwxcToken
import org.openqa.selenium.remote.RemoteWebDriver

case class DriverWithToken(driver: RemoteWebDriver, token: JjwxcToken)
