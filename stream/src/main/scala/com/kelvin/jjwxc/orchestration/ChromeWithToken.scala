package com.kelvin.jjwxc.orchestration
import com.kelvin.jjwxc.auth.JjwxcToken
import org.openqa.selenium.chrome.ChromeDriver

case class ChromeWithToken (driver: ChromeDriver, token: JjwxcToken)
