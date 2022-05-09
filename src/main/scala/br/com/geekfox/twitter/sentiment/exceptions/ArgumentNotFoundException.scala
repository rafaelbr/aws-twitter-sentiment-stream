package br.com.geekfox.twitter.sentiment.exceptions

case class ArgumentNotFoundException(
                                      private val message: String,
                                      private val cause: Throwable = None.orNull
                                    ) extends Exception(message, cause)
