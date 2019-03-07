package asura.common.util

import java.security._
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util

import javax.crypto.Cipher

object RSAUtils {

  private val KEY_ALGORITHM = "RSA"
  private val KEY_SIZE = 1024

  private val PUBLIC_KEY = "RSAPublicKey"
  private val PRIVATE_KEY = "RSAPrivateKey"

  @throws[Exception]
  def initKey(): util.Map[String, AnyRef] = {
    val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
    keyPairGenerator.initialize(KEY_SIZE)
    val keyPair = keyPairGenerator.generateKeyPair
    val publicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
    val privateKey = keyPair.getPrivate.asInstanceOf[RSAPrivateKey]
    val keyMap = new util.HashMap[String, AnyRef]
    keyMap.put(PUBLIC_KEY, publicKey)
    keyMap.put(PRIVATE_KEY, privateKey)
    keyMap
  }


  @throws[Exception]
  def encryptByPrivateKey(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val pkcs8KeySpec = new PKCS8EncodedKeySpec(key)
    val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
    val privateKey = keyFactory.generatePrivate(pkcs8KeySpec)
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, privateKey)
    cipher.doFinal(data)
  }

  @throws[Exception]
  def encryptByPublicKey(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
    val x509KeySpec = new X509EncodedKeySpec(key)
    val pubKey = keyFactory.generatePublic(x509KeySpec)
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, pubKey)
    cipher.doFinal(data)
  }

  @throws[Exception]
  def decryptByPrivateKey(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val pkcs8KeySpec = new PKCS8EncodedKeySpec(key)
    val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
    val privateKey = keyFactory.generatePrivate(pkcs8KeySpec)
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, privateKey)
    cipher.doFinal(data)
  }

  @throws[Exception]
  def decryptByPublicKey(data: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
    val x509KeySpec = new X509EncodedKeySpec(key)
    val pubKey = keyFactory.generatePublic(x509KeySpec)
    val cipher = Cipher.getInstance(keyFactory.getAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, pubKey)
    cipher.doFinal(data)
  }

  def getPrivateKey(keyMap: util.Map[String, AnyRef]): Array[Byte] = {
    val key = keyMap.get(PRIVATE_KEY).asInstanceOf[Key]
    key.getEncoded
  }

  @throws[Exception]
  def getPublicKey(keyMap: util.Map[String, AnyRef]): Array[Byte] = {
    val key = keyMap.get(PUBLIC_KEY).asInstanceOf[Key]
    key.getEncoded
  }
}
