package crypto

import java.security.MessageDigest

object SHA1 {
  def checksum(text: String) = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(text.getBytes("UTF-8"))
    md.digest().toString
  }
}
