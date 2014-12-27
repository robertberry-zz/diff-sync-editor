package crypto

import org.apache.commons.codec.digest.DigestUtils

object SHA1 {
  def checksum(text: String) = DigestUtils.sha1Hex(text)
}
